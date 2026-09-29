package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.ai.*;
import com.easyprufung.backend.exam.domain.*;
import com.easyprufung.backend.exam.dto.*;
import com.easyprufung.backend.exam.exception.ExamConfigurationException;
import com.easyprufung.backend.exam.exception.AiIntegrationException;
import com.easyprufung.backend.exam.mapper.ExamViewMapper;
import com.easyprufung.backend.exam.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ExamGenerationService {
    private static final String AI_SCHEMA_VERSION = "1.0";
    private static final Logger log = LoggerFactory.getLogger(ExamGenerationService.class);
    private final ExamDefinitionService definitionService;
    private final ExerciseTemplateRepository templateRepository;
    private final ExamSessionRepository sessionRepository;
    private final ExamAiClient aiClient;
    private final JsonSupport json;
    private final ExamViewMapper mapper;
    private final Executor generationExecutor;

    public ExamGenerationService(ExamDefinitionService definitionService,
                                 ExerciseTemplateRepository templateRepository,
                                 ExamSessionRepository sessionRepository,
                                 ExamAiClient aiClient,
                                 JsonSupport json,
                                 ExamViewMapper mapper,
                                 @Qualifier("examGenerationExecutor") Executor generationExecutor) {
        this.definitionService = definitionService;
        this.templateRepository = templateRepository;
        this.sessionRepository = sessionRepository;
        this.aiClient = aiClient;
        this.json = json;
        this.mapper = mapper;
        this.generationExecutor = generationExecutor;
    }

    public ExamSessionView start(StartExamRequest request) {
        Instant requestStartedAt = Instant.now();
        ExamDefinition definition = definitionService.resolve(request);
        validateDefinition(definition);

        List<PartDefinition> parts = orderedParts(definition);
        Map<UUID, CompletableFuture<AiGeneratedExerciseResponse>> aiResponses =
                startAiGeneration(definition, parts);

        ExamSession session = new ExamSession();
        session.setUserId(request.getUserId());
        session.setExamDefinition(definition);
        session.setDefinitionVersion(definition.getDefinitionVersion());
        session.setStatus(SessionStatus.IN_PROGRESS);
        session.setCreatedAt(requestStartedAt);

        Map<String, Integer> timingGroups = new HashMap<>();
        for (SectionDefinition section : definition.getSections()) {
            if (section.getDurationSeconds() != null) {
                String group = section.getTimingGroup() == null ? section.getSectionKey() : section.getTimingGroup();
                timingGroups.merge(group, section.getDurationSeconds(), Math::max);
            }
        }
        int duration = timingGroups.values().stream().mapToInt(Integer::intValue).sum();

        int order = 0;
        for (PartDefinition part : parts) {
            ExerciseInstance exercise;

            switch (part.getContentSource()) {
                case AI_GENERATED:
                    exercise = fromAi(
                            part,
                            awaitAiResponse(part, aiResponses.get(part.getId()))
                    );
                    break;

                case PREDEFINED:
                case AUDIO_PREDEFINED:
                    exercise = fromTemplate(part);
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Unsupported content source: " + part.getContentSource()
                    );
            }

            exercise.setOrderIndex(order++);
            session.addExercise(exercise);
        }

        /*
         * The candidate's exam time starts only after every exercise is ready.
         * AI generation time must never reduce the available exam duration.
         */
        Instant examStartedAt = Instant.now();
        session.setStartedAt(examStartedAt);
        session.setExpiresAt(
                duration > 0 ? examStartedAt.plusSeconds(duration) : null
        );

        ExamSession saved = sessionRepository.save(session);

        log.info(
                "Created exam session {} with {} parts in {} ms",
                saved.getId(),
                saved.getExercises().size(),
                Duration.between(requestStartedAt, Instant.now()).toMillis()
        );

        return mapper.toView(saved);
    }

    private Map<UUID, CompletableFuture<AiGeneratedExerciseResponse>> startAiGeneration(
            ExamDefinition definition,
            List<PartDefinition> parts
    ) {
        Map<UUID, CompletableFuture<AiGeneratedExerciseResponse>> responses =
                new HashMap<>();

        for (PartDefinition part : parts) {
            if (part.getContentSource() != ContentSource.AI_GENERATED) {
                continue;
            }

            AiExerciseGenerationRequest aiRequest =
                    createAiRequest(definition, part);
            String partKey = part.getPartKey();

            responses.put(
                    part.getId(),
                    CompletableFuture.supplyAsync(
                            () -> generatePart(partKey, aiRequest),
                            generationExecutor
                    )
            );
        }

        return responses;
    }

    private AiGeneratedExerciseResponse generatePart(
            String partKey,
            AiExerciseGenerationRequest request
    ) {
        Instant startedAt = Instant.now();

        try {
            return aiClient.generateExercise(request);
        } finally {
            log.info(
                    "OpenAI generation for part {} finished in {} ms",
                    partKey,
                    Duration.between(startedAt, Instant.now()).toMillis()
            );
        }
    }

    private AiGeneratedExerciseResponse awaitAiResponse(
            PartDefinition part,
            CompletableFuture<AiGeneratedExerciseResponse> future
    ) {
        if (future == null) {
            throw new ExamConfigurationException(
                    "Missing AI generation task for part " + part.getPartKey()
            );
        }

        try {
            return future.join();
        } catch (CompletionException exception) {
            Throwable cause = exception.getCause();

            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }

            throw new AiIntegrationException(
                    "AI generation failed for part " + part.getPartKey(),
                    cause
            );
        }
    }

    private AiExerciseGenerationRequest createAiRequest(
            ExamDefinition definition,
            PartDefinition part
    ) {
        return new AiExerciseGenerationRequest(
                AI_SCHEMA_VERSION,
                definition.getCode(),
                definition.getProvider(),
                definition.getLevel(),
                part.getSectionDefinition().getSectionKey(),
                part.getPartKey(),
                part.getQuestionType(),
                part.getFirstQuestionNumber(),
                part.getQuestionCount(),
                part.getPointsPerQuestion(),
                part.getMinimumWords(),
                part.getMaximumWords(),
                part.getGenerationInstructions(),
                "de-DE"
        );
    }

    private ExerciseInstance fromTemplate(PartDefinition part) {
        List<ExerciseTemplate> templates = templateRepository
                .findByPartDefinitionIdAndActiveTrueOrderByTemplateKeyAsc(part.getId());
        if (templates.isEmpty()) {
            throw new ExamConfigurationException("No active predefined template for part " + part.getPartKey());
        }
        ExerciseTemplate source = templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
        if (part.getContentSource() == ContentSource.AUDIO_PREDEFINED
                && (source.getAudioUrl() == null || source.getAudioUrl().isBlank())) {
            throw new ExamConfigurationException("Audio template has no audioUrl: " + source.getTemplateKey());
        }
        ExerciseInstance target = new ExerciseInstance();
        target.setPartDefinition(part);
        target.setInstructions(source.getInstructions());
        target.setContent(source.getContent());
        target.setAudioUrl(source.getAudioUrl());
        target.setAudioContentType(source.getAudioContentType());
        target.setAudioPlayLimit(source.getAudioPlayLimit());
        int order = 0;
        for (QuestionTemplate question : source.getQuestions()) {
            QuestionInstance copy = new QuestionInstance();
            copy.setOrderIndex(order++);
            copy.setExternalNumber(question.getExternalNumber());
            copy.setStimulus(question.getStimulus());
            copy.setPrompt(question.getPrompt());
            copy.setQuestionType(question.getQuestionType());
            copy.setOptionsJson(question.getOptionsJson());
            copy.setCorrectAnswerJson(question.getCorrectAnswerJson());
            copy.setMaximumScore(question.getMaximumScore());
            copy.setExplanation(question.getExplanation());
            target.addQuestion(copy);
        }
        validateExercise(part, target);
        return target;
    }

    private ExerciseInstance fromAi(
            PartDefinition part,
            AiGeneratedExerciseResponse generated
    ) {
        ExerciseInstance exercise = new ExerciseInstance();
        exercise.setPartDefinition(part);
        exercise.setInstructions(generated.getInstructions());
        exercise.setContent(generated.getContent());
        int order = 0;
        for (AiGeneratedExerciseResponse.GeneratedQuestion q : generated.getQuestions()) {
            QuestionInstance question = new QuestionInstance();
            question.setOrderIndex(order++);
            question.setExternalNumber(q.getNumber());
            question.setStimulus(q.getStimulus());
            question.setPrompt(q.getPrompt());
            question.setQuestionType(q.getType());
            question.setOptionsJson(json.write(q.getOptions()));
            question.setCorrectAnswerJson(json.write(q.getCorrectAnswer()));
            question.setMaximumScore(q.getMaximumScore());
            question.setExplanation(q.getExplanation());
            exercise.addQuestion(question);
        }
        validateExercise(part, exercise);
        return exercise;
    }

    private void validateDefinition(ExamDefinition definition) {
        if (definition.getSections().isEmpty()) throw new ExamConfigurationException("Exam has no sections");
        var partKeys = new HashSet<String>();
        for (SectionDefinition section : definition.getSections()) {
            if (section.getParts().isEmpty()) throw new ExamConfigurationException("Section has no parts: " + section.getSectionKey());
            for (PartDefinition part : section.getParts()) {
                if (!partKeys.add(part.getPartKey())) throw new ExamConfigurationException("Duplicate partKey: " + part.getPartKey());
            }
        }
    }

    private void validateExercise(PartDefinition part, ExerciseInstance exercise) {
        if (exercise.getQuestions().size() != part.getQuestionCount()) {
            throw new ExamConfigurationException("Part " + part.getPartKey() + " requires "
                    + part.getQuestionCount() + " questions, got " + exercise.getQuestions().size());
        }
        Set<String> numbers = new HashSet<>();
        for (QuestionInstance q : exercise.getQuestions()) {
            if (!numbers.add(q.getExternalNumber())) {
                throw new ExamConfigurationException("Duplicate question number " + q.getExternalNumber());
            }
            if (q.getQuestionType() != part.getQuestionType()) {
                throw new ExamConfigurationException("Question type does not match part " + part.getPartKey());
            }
            if (q.getMaximumScore().compareTo(part.getPointsPerQuestion()) != 0) {
                throw new ExamConfigurationException("Question " + q.getExternalNumber()
                        + " must have maximumScore " + part.getPointsPerQuestion());
            }
            var options = json.read(q.getOptionsJson(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<OptionDto>>() {});
            var correct = json.read(q.getCorrectAnswerJson(), CorrectAnswerPayload.class);
            if (part.getQuestionType() == QuestionType.FREE_TEXT) {
                if (!options.isEmpty()) throw new ExamConfigurationException("FREE_TEXT questions cannot have options");
            } else {
                if (options.isEmpty()) throw new ExamConfigurationException("Objective question "
                        + q.getExternalNumber() + " has no options");
                if (correct.getAcceptedOptionKeys().isEmpty()) throw new ExamConfigurationException("Objective question "
                        + q.getExternalNumber() + " has no correct option key");
                Set<String> optionKeys = options.stream().map(o -> o.getKey().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
                if (optionKeys.size() != options.size()) throw new ExamConfigurationException("Duplicate option key in question " + q.getExternalNumber());
                if (correct.getAcceptedOptionKeys().stream().map(k -> k.toLowerCase(Locale.ROOT)).anyMatch(k -> !optionKeys.contains(k))) {
                    throw new ExamConfigurationException("Correct answer refers to an unknown option in question " + q.getExternalNumber());
                }
            }
        }
        if (part.getQuestionType() != QuestionType.FREE_TEXT) {
            for (int i = 0; i < part.getQuestionCount(); i++) {
                String expected = Integer.toString(part.getFirstQuestionNumber() + i);
                if (!numbers.contains(expected)) throw new ExamConfigurationException("Part " + part.getPartKey()
                        + " is missing question number " + expected);
            }
        }
    }

    private List<SectionDefinition> sortedSections(ExamDefinition definition) {
        return definition.getSections().stream().sorted(Comparator.comparingInt(SectionDefinition::getOrderIndex)).collect(Collectors.toList());
    }
    private List<PartDefinition> sortedParts(SectionDefinition section) {
        return section.getParts().stream().sorted(Comparator.comparingInt(PartDefinition::getOrderIndex)).collect(Collectors.toList());
    }

    private List<PartDefinition> orderedParts(ExamDefinition definition) {
        return sortedSections(definition).stream()
                .flatMap(section -> sortedParts(section).stream())
                .collect(Collectors.toList());
    }
}

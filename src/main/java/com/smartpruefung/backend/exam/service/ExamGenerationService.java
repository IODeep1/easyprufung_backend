package com.smartpruefung.backend.exam.service;

import com.smartpruefung.backend.exam.ai.*;
import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.dto.*;
import com.smartpruefung.backend.exam.exception.ExamConfigurationException;
import com.smartpruefung.backend.exam.exception.AiIntegrationException;
import com.smartpruefung.backend.exam.mapper.ExamViewMapper;
import com.smartpruefung.backend.exam.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
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
    private final ExamDefinitionRepository definitions;
    private final SharedExerciseRepository pool;
    private final ExerciseInstanceRepository instances;
    private final SharedExerciseStore poolStore;
    private final ExerciseFingerprint fingerprint;

    public ExamGenerationService(ExamDefinitionService definitionService,
                                 ExerciseTemplateRepository templateRepository,
                                 ExamSessionRepository sessionRepository,
                                 ExamAiClient aiClient,
                                 JsonSupport json,
                                 ExamViewMapper mapper,
                                 @Qualifier("examGenerationExecutor") Executor generationExecutor,
                                 ExamDefinitionRepository definitions,
                                 SharedExerciseRepository pool,
                                 ExerciseInstanceRepository instances,
                                 SharedExerciseStore poolStore,
                                 ExerciseFingerprint fingerprint) {
        this.definitionService = definitionService;
        this.templateRepository = templateRepository;
        this.sessionRepository = sessionRepository;
        this.aiClient = aiClient;
        this.json = json;
        this.mapper = mapper;
        this.generationExecutor = generationExecutor;
        this.definitions = definitions;
        this.pool = pool;
        this.instances = instances;
        this.poolStore = poolStore;
        this.fingerprint = fingerprint;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ExamSessionView start(StartExamRequest request) {
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId is required");
        }
        Instant requestStartedAt = Instant.now();
        ExamDefinition definition = definitionService.resolve(request);
        definitions.lockForStart(definition.getId()).orElseThrow(() ->
                new ExamConfigurationException("Exam definition disappeared during start"));
        validateDefinition(definition);

        List<PartDefinition> parts = orderedParts(definition);
        Map<UUID, CompletableFuture<SharedExercise>> aiResponses =
                startAiGeneration(definition, parts, request.getUserId());

        try {
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
                        SharedExercise shared = awaitAiResponse(part, aiResponses.get(part.getId()));
                        exercise = fromAi(part, json.read(shared.getPayload(), AiGeneratedExerciseResponse.class));
                        exercise.setPoolKey(shared.getPoolKey());
                        exercise.setContentKey(shared.getContentKey());
                        break;

                    case PREDEFINED:
                    case AUDIO_PREDEFINED:
                        exercise = selectTemplate(definition, part, request.getUserId());
                        break;

                    default:
                        throw new IllegalArgumentException(
                                "Unsupported content source: " + part.getContentSource()
                        );
                }

                exercise.setOrderIndex(order++);
                session.addExercise(exercise);
            }

            // Generation and pool lookup never reduce the candidate's exam duration.
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
        } finally {
            awaitAll(aiResponses);
        }
    }

    private void awaitAll(Map<UUID, CompletableFuture<SharedExercise>> responses) {
        // Keep the definition lock until workers have published every valid result,
        // including when a different part failed. Never mask the original exception.
        responses.values().forEach(future -> future.handle((value, failure) -> null).join());
    }

    private Map<UUID, CompletableFuture<SharedExercise>> startAiGeneration(
            ExamDefinition definition, List<PartDefinition> parts, String userId) {
        Map<UUID, CompletableFuture<SharedExercise>> responses = new HashMap<>();
        try {
            for (PartDefinition part : parts) {
                if (part.getContentSource() != ContentSource.AI_GENERATED) continue;
                AiExerciseGenerationRequest request = createAiRequest(definition, part);
                String poolKey = fingerprint.pool(request, definition.getDefinitionVersion(), part.getContentSource());

                // Adopt historical AI exercises without making another API call.
                for (ExerciseInstance old : instances.findByPartDefinitionIdAndPoolKeyIsNull(part.getId())) {
                    validateExercise(part, old);
                    SharedExercise shared = poolStore.save(poolKey, fingerprint.snapshot(old));
                    old.setPoolKey(poolKey);
                    old.setContentKey(shared.getContentKey());
                }
                Set<String> used = new HashSet<>();
                for (ExerciseInstance old : instances.history(userId, poolKey, part.getId())) {
                    used.addAll(fingerprint.questions(fingerprint.snapshot(old)));
                }
                List<SharedExercise> available = pool.findByPoolKey(poolKey).stream()
                        .filter(entry -> Collections.disjoint(used, fingerprint.questions(
                                json.read(entry.getPayload(), AiGeneratedExerciseResponse.class))))
                        .collect(Collectors.toList());
                if (!available.isEmpty()) {
                    Collections.shuffle(available);
                    responses.put(part.getId(), CompletableFuture.completedFuture(available.get(0)));
                } else {
                    responses.put(part.getId(), CompletableFuture.supplyAsync(() -> {
                        // A provider can return a duplicate. Never silently deliver a seen question.
                        for (int attempt = 0; attempt < 3; attempt++) {
                            AiGeneratedExerciseResponse generated = generatePart(part.getPartKey(), request);
                            fromAi(part, generated); // validate before publishing to the shared pool
                            SharedExercise shared = poolStore.save(poolKey, generated);
                            if (Collections.disjoint(used, fingerprint.questions(generated))) return shared;
                        }
                        throw new AiIntegrationException("AI repeatedly returned previously used questions for part "
                                + part.getPartKey());
                    }, generationExecutor));
                }
            }
        } catch (RuntimeException failure) {
            awaitAll(responses);
            throw failure;
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

    private SharedExercise awaitAiResponse(
            PartDefinition part,
            CompletableFuture<SharedExercise> future
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

    private ExerciseInstance selectTemplate(ExamDefinition definition, PartDefinition part, String userId) {
        List<ExerciseTemplate> templates = templateRepository
                .findByPartDefinitionIdAndActiveTrueOrderByTemplateKeyAsc(part.getId());
        if (templates.isEmpty()) {
            throw new ExamConfigurationException("No active predefined template for part " + part.getPartKey());
        }
        // Include level, provider, version and requirements in the independent listening scope.
        String poolKey = fingerprint.pool(createAiRequest(definition, part), definition.getDefinitionVersion(), part.getContentSource());
        Map<String, Long> counts = new HashMap<>();
        for (ExerciseInstance old : instances.history(userId, poolKey, part.getId())) {
            String key = old.getContentKey() == null ? fingerprint.listening(old) : old.getContentKey();
            counts.merge(key, 1L, Long::sum);
        }
        // Identical templates are one pool item, even if imported under different template IDs.
        Map<String, ExerciseInstance> candidates = new HashMap<>();
        for (ExerciseTemplate template : templates) {
            ExerciseInstance exercise = fromTemplate(part, template);
            String key = fingerprint.listening(exercise);
            exercise.setPoolKey(poolKey);
            exercise.setContentKey(key);
            candidates.putIfAbsent(key, exercise);
        }
        long minimum = candidates.keySet().stream().mapToLong(key -> counts.getOrDefault(key, 0L)).min().orElse(0L);
        List<ExerciseInstance> available = candidates.entrySet().stream()
                .filter(entry -> counts.getOrDefault(entry.getKey(), 0L) == minimum)
                .map(Map.Entry::getValue).collect(Collectors.toList());
        Collections.shuffle(available);
        return available.get(0);
    }

    private ExerciseInstance fromTemplate(PartDefinition part, ExerciseTemplate source) {
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
        if (fingerprint.questions(generated).size() != generated.getQuestions().size()) {
            throw new ExamConfigurationException("AI exercise contains duplicate questions for part " + part.getPartKey());
        }
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

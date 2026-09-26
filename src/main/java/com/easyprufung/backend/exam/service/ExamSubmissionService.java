package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.domain.*;
import com.easyprufung.backend.exam.dto.*;
import com.easyprufung.backend.exam.exception.*;
import com.easyprufung.backend.exam.mapper.ExamViewMapper;
import com.easyprufung.backend.exam.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.*;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ExamSubmissionService {
    private final ExamSessionRepository sessions;
    private final UserAnswerRepository answers;
    private final ExamResultRepository results;
    private final CompactAnswerParser compactParser;
    private final JsonSupport json;
    private final ExamViewMapper mapper;
    private final Map<EvaluationMode, AnswerEvaluator> evaluators;

    public ExamSubmissionService(ExamSessionRepository sessions, UserAnswerRepository answers,
                                 ExamResultRepository results, CompactAnswerParser compactParser,
                                 JsonSupport json, ExamViewMapper mapper, List<AnswerEvaluator> evaluators) {
        this.sessions = sessions; this.answers = answers; this.results = results;
        this.compactParser = compactParser; this.json = json; this.mapper = mapper;
        this.evaluators = evaluators.stream().collect(Collectors.toUnmodifiableMap(AnswerEvaluator::mode, Function.identity()));
    }

    @Transactional(noRollbackFor = ExpiredSessionException.class)
    public ExamResultView submit(UUID sessionId, SubmitExamRequest request) {
        ExamSession session = sessions.findDetailedById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam session not found: " + sessionId));
        if (session.getStatus() == SessionStatus.EVALUATED) {
            return mapper.toView(results.findByExamSessionId(sessionId)
                    .orElseThrow(() -> new ExamStateException("Evaluated session has no result")));
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw new ExamStateException("Session cannot be submitted in status " + session.getStatus());
        }
        if (session.getExpiresAt() != null && Instant.now().isAfter(session.getExpiresAt())) {
            session.setStatus(SessionStatus.EXPIRED);
            sessions.save(session);
            throw new ExpiredSessionException("Exam session has expired");
        }

        Map<String, AnswerPayload> submitted = mergeAnswers(request);
        Map<String, QuestionLocation> questionByNumber = indexQuestions(session);
        submitted.keySet().forEach(number -> {
            if (!questionByNumber.containsKey(number)) throw new IllegalArgumentException("Unknown question number " + number);
        });

        answers.deleteByExamSessionId(sessionId);
        List<UserAnswer> storedAnswers = new ArrayList<>();
        for (AnswerPayload answer : submitted.values()) {
            validateAnswer(questionByNumber.get(answer.getQuestionNumber()).getQuestion(), answer);
            UserAnswer entity = new UserAnswer();
            entity.setExamSession(session);
            entity.setQuestionInstance(questionByNumber.get(answer.getQuestionNumber()).getQuestion());
            entity.setAnswerJson(json.write(answer));
            entity.setAnsweredAt(Instant.now());
            storedAnswers.add(entity);
        }
        answers.saveAll(storedAnswers);

        List<QuestionResultView> questionResults = new ArrayList<>();
        Map<String, SectionAccumulator> sectionResults = new LinkedHashMap<>();
        for (QuestionLocation location : questionByNumber.values()) {
            PartDefinition part = location.getExercise().getPartDefinition();
            AnswerEvaluator evaluator = Optional.ofNullable(evaluators.get(part.getEvaluationMode()))
                    .orElseThrow(() -> new ExamConfigurationException("No evaluator for " + part.getEvaluationMode()));
            QuestionResultView result = evaluator.evaluate(new EvaluationItem(
                    session, location.getExercise(), location.getQuestion(), submitted.get(location.getQuestion().getExternalNumber())));
            questionResults.add(result);
            SectionDefinition section = part.getSectionDefinition();
            sectionResults.computeIfAbsent(section.getSectionKey(), key -> new SectionAccumulator(section.getTitle()))
                    .add(result.getScore(), result.getMaximumScore());
        }
        questionResults.sort(Comparator.comparing(QuestionResultView::getNumber, ExamSubmissionService::compareNumbers));
        List<SectionResultView> sectionViews = sectionResults.entrySet().stream()
                .map(e -> new SectionResultView(e.getKey(), e.getValue().title, e.getValue().score, e.getValue().maximum))
                .collect(Collectors.toList());
        BigDecimal total = questionResults.stream().map(QuestionResultView::getScore).reduce(BigDecimal.ZERO, BigDecimal::add);
        ExamDefinition definition = session.getExamDefinition();
        BigDecimal percentage = definition.getMaximumScore().signum() == 0 ? BigDecimal.ZERO
                : total.multiply(BigDecimal.valueOf(100)).divide(definition.getMaximumScore(), 2,
                RoundingMode.valueOf(definition.getScoreRounding()));

        ExamResult examResult = new ExamResult();
        examResult.setExamSession(session);
        examResult.setScore(total);
        examResult.setMaximumScore(definition.getMaximumScore());
        examResult.setPercentage(percentage);
        examResult.setPassed(percentage.compareTo(definition.getPassPercentage()) >= 0);
        examResult.setSectionResultsJson(json.write(sectionViews));
        examResult.setQuestionResultsJson(json.write(questionResults));
        examResult.setOverallFeedback(questionResults.stream()
                .filter(q -> q.getExplanation() != null && !q.getExplanation().isBlank())
                .filter(q -> questionByNumber.get(q.getNumber()).getQuestion().getQuestionType() == QuestionType.FREE_TEXT)
                .map(QuestionResultView::getExplanation).collect(Collectors.joining("\n")));
        examResult.setEvaluatedAt(Instant.now());
        session.setSubmittedAt(Instant.now());
        session.setStatus(SessionStatus.EVALUATED);
        sessions.save(session);
        return mapper.toView(results.save(examResult));
    }

    private Map<String, AnswerPayload> mergeAnswers(SubmitExamRequest request) {
        Map<String, AnswerPayload> merged = new LinkedHashMap<>();
        List<AnswerPayload> all = new ArrayList<>(compactParser.parse(request.getCompactAnswers()));
        all.addAll(request.getAnswers());
        for (AnswerPayload answer : all) {
            if (merged.putIfAbsent(answer.getQuestionNumber(), answer) != null) {
                throw new IllegalArgumentException("Duplicate answer for question " + answer.getQuestionNumber());
            }
        }
        return merged;
    }

    private Map<String, QuestionLocation> indexQuestions(ExamSession session) {
        Map<String, QuestionLocation> result = new LinkedHashMap<>();
        for (ExerciseInstance exercise : session.getExercises()) {
            for (QuestionInstance question : exercise.getQuestions()) {
                if (result.put(question.getExternalNumber(), new QuestionLocation(exercise, question)) != null) {
                    throw new ExamConfigurationException("Duplicate session question number " + question.getExternalNumber());
                }
            }
        }
        return result;
    }

    private void validateAnswer(QuestionInstance question, AnswerPayload answer) {
        boolean hasOptions = answer.getSelectedOptionKeys() != null && !answer.getSelectedOptionKeys().isEmpty();
        boolean hasText = answer.getText() != null && !answer.getText().isBlank();
        if (question.getQuestionType() == QuestionType.FREE_TEXT && (!hasText || hasOptions)) {
            throw new IllegalArgumentException("Question " + question.getExternalNumber() + " requires a text answer");
        }
        if (question.getQuestionType() != QuestionType.FREE_TEXT && (!hasOptions || hasText)) {
            throw new IllegalArgumentException("Question " + question.getExternalNumber() + " requires selected option keys");
        }
        PartDefinition part = question.getExerciseInstance().getPartDefinition();
        if (hasOptions && part.getAnswerPattern() != null) {
            Pattern allowed = Pattern.compile(part.getAnswerPattern());
            answer.getSelectedOptionKeys().forEach(key -> {
                if (key == null || !allowed.matcher(key).matches()) {
                    throw new IllegalArgumentException("Invalid answer key for question " + question.getExternalNumber());
                }
            });
        }
        if (hasOptions) {
            var optionKeys = json.read(question.getOptionsJson(), new com.fasterxml.jackson.core.type.TypeReference<List<OptionDto>>() {})
                    .stream().map(o -> o.getKey().toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
            answer.getSelectedOptionKeys().forEach(key -> {
                if (!optionKeys.contains(key.toLowerCase(Locale.ROOT))) {
                    throw new IllegalArgumentException("Unknown option '" + key + "' for question " + question.getExternalNumber());
                }
            });
        }
    }

    private static int compareNumbers(String left, String right) {
        try { return Integer.compare(Integer.parseInt(left), Integer.parseInt(right)); }
        catch (NumberFormatException ignored) { return left.compareTo(right); }
    }

    private static final class QuestionLocation {
        private final ExerciseInstance exercise;
        private final QuestionInstance question;
        private QuestionLocation(ExerciseInstance exercise, QuestionInstance question) {
            this.exercise = exercise;
            this.question = question;
        }
        private ExerciseInstance getExercise() { return exercise; }
        private QuestionInstance getQuestion() { return question; }
    }
    private static final class SectionAccumulator {
        private final String title;
        private BigDecimal score = BigDecimal.ZERO;
        private BigDecimal maximum = BigDecimal.ZERO;
        private SectionAccumulator(String title) { this.title = title; }
        private void add(BigDecimal score, BigDecimal maximum) {
            this.score = this.score.add(score); this.maximum = this.maximum.add(maximum);
        }
    }
}

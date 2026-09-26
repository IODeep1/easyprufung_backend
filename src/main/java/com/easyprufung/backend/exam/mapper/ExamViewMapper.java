package com.easyprufung.backend.exam.mapper;

import com.easyprufung.backend.exam.domain.*;
import com.easyprufung.backend.exam.dto.*;
import com.easyprufung.backend.exam.service.JsonSupport;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class ExamViewMapper {
    private static final TypeReference<java.util.List<OptionDto>> OPTIONS = new TypeReference<>() {};
    private static final TypeReference<java.util.List<SectionResultView>> SECTIONS = new TypeReference<>() {};
    private static final TypeReference<java.util.List<QuestionResultView>> QUESTIONS = new TypeReference<>() {};
    private final JsonSupport json;

    public ExamViewMapper(JsonSupport json) { this.json = json; }

    public ExamSessionView toView(ExamSession session) {
        ExamDefinition definition = session.getExamDefinition();
        var exercises = session.getExercises().stream()
                .sorted(Comparator.comparingInt(ExerciseInstance::getOrderIndex))
                .map(e -> {
                    PartDefinition part = e.getPartDefinition();
                    SectionDefinition section = part.getSectionDefinition();
                    var questions = e.getQuestions().stream()
                            .sorted(Comparator.comparingInt(QuestionInstance::getOrderIndex))
                            .map(q -> new QuestionView(q.getId(), q.getExternalNumber(),q.getStimulus(), q.getPrompt(),
                                    q.getQuestionType(), json.read(q.getOptionsJson(), OPTIONS), q.getMaximumScore()))
                            .collect(Collectors.toList());
                    return new ExerciseView(e.getId(), section.getSectionKey(), section.getTitle(),
                            part.getPartKey(), part.getTitle(), e.getInstructions(), e.getContent(),
                            e.getAudioUrl(), e.getAudioContentType(), e.getAudioPlayLimit(),
                            part.getDurationSeconds(), questions);
                }).collect(Collectors.toList());
        return new ExamSessionView(session.getId(), definition.getCode(), definition.getTitle(),
                definition.getProvider(), definition.getLevel(), session.getDefinitionVersion(), session.getStatus(),
                session.getStartedAt(), session.getExpiresAt(), exercises);
    }

    public ExamResultView toView(ExamResult result) {
        return new ExamResultView(result.getExamSession().getId(), result.getScore(), result.getMaximumScore(),
                result.getPercentage(), result.isPassed(), json.read(result.getSectionResultsJson(), SECTIONS),
                json.read(result.getQuestionResultsJson(), QUESTIONS), result.getOverallFeedback(),
                result.getEvaluatedAt());
    }

    public ExamSessionSummaryView toSummary(ExamSession session) {
        ExamDefinition definition = session.getExamDefinition();

        return new ExamSessionSummaryView(
                session.getId(),
                definition.getCode(),
                definition.getTitle(),
                definition.getProvider(),
                definition.getLevel(),
                session.getDefinitionVersion(),
                session.getStatus(),
                session.getCreatedAt(),
                session.getStartedAt(),
                session.getExpiresAt(),
                session.getSubmittedAt()
        );
    }
}

package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.ai.*;
import com.easyprufung.backend.exam.domain.EvaluationMode;
import com.easyprufung.backend.exam.dto.QuestionResultView;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class WrittenExpressionEvaluator implements AnswerEvaluator {
    private static final String SCHEMA_VERSION = "1.0";
    private final ExamAiClient aiClient;

    public WrittenExpressionEvaluator(ExamAiClient aiClient) { this.aiClient = aiClient; }

    @Override public EvaluationMode mode() { return EvaluationMode.AI_WRITTEN; }

    @Override
    public QuestionResultView evaluate(EvaluationItem item) {
        String text = item.getAnswer() == null ? null : item.getAnswer().getText();
        if (text == null || text.isBlank()) {
            return new QuestionResultView(item.getQuestion().getExternalNumber(), false,
                    java.math.BigDecimal.ZERO, item.getQuestion().getMaximumScore(), List.of(), List.of(),
                    "No written response was submitted.");
        }
        var definition = item.getSession().getExamDefinition();
        var part = item.getExercise().getPartDefinition();
        var response = aiClient.evaluateWriting(new AiWritingEvaluationRequest(
                SCHEMA_VERSION, definition.getCode(), definition.getProvider(), definition.getLevel(),
                item.getExercise().getInstructions(), item.getExercise().getContent(), text,
                item.getQuestion().getMaximumScore(), part.getMinimumWords(), part.getMaximumWords(),
                part.getEvaluationRubricJson(), "de-DE"));
        String criteria = response.getCriteria().stream()
                .map(c -> c.getTitle() + ": " + c.getScore() + "/" + c.getMaximumScore() + " — " + c.getFeedback())
                .collect(Collectors.joining("\n"));
        String feedback = response.getOverallFeedback() + (criteria.isBlank() ? "" : "\n" + criteria);
        return new QuestionResultView(item.getQuestion().getExternalNumber(),
                response.getScore().compareTo(response.getMaximumScore()) == 0,
                response.getScore(), response.getMaximumScore(), List.of(text), List.of(), feedback);
    }
}

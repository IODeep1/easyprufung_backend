package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.domain.EvaluationMode;
import com.easyprufung.backend.exam.dto.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class ObjectiveAnswerEvaluator implements AnswerEvaluator {
    protected final JsonSupport json;

    public ObjectiveAnswerEvaluator(JsonSupport json) { this.json = json; }

    @Override public EvaluationMode mode() { return EvaluationMode.OBJECTIVE; }

    @Override
    public QuestionResultView evaluate(EvaluationItem item) {
        CorrectAnswerPayload correct = json.read(item.getQuestion().getCorrectAnswerJson(), CorrectAnswerPayload.class);
        List<String> submitted = submittedValues(item.getAnswer());
        boolean matchesOptions = !correct.getAcceptedOptionKeys().isEmpty()
                && normalizedSet(submitted).equals(normalizedSet(correct.getAcceptedOptionKeys()));
        boolean matchesText = !correct.getAcceptedTexts().isEmpty() && item.getAnswer() != null
                && correct.getAcceptedTexts().stream().anyMatch(v -> normalize(v).equals(normalize(item.getAnswer().getText())));
        boolean isCorrect = matchesOptions || matchesText;
        String explanation = isCorrect ? null : item.getQuestion().getExplanation();
        return new QuestionResultView(item.getQuestion().getExternalNumber(), isCorrect,
                isCorrect ? item.getQuestion().getMaximumScore() : BigDecimal.ZERO,
                item.getQuestion().getMaximumScore(), submitted, correct.getAcceptedOptionKeys(), explanation);
    }

    protected List<String> submittedValues(AnswerPayload answer) {
        if (answer == null) return List.of();
        if (!answer.getSelectedOptionKeys().isEmpty()) return answer.getSelectedOptionKeys();
        return answer.getText() == null || answer.getText().isBlank() ? List.of() : List.of(answer.getText());
    }

    private Set<String> normalizedSet(Collection<String> values) {
        Set<String> result = new TreeSet<>();
        values.stream().filter(Objects::nonNull).map(this::normalize).forEach(result::add);
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}

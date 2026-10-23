package com.smartpruefung.backend.exam.service;

import com.smartpruefung.backend.exam.domain.EvaluationMode;
import com.smartpruefung.backend.exam.dto.QuestionResultView;
import com.smartpruefung.backend.exam.exception.ExamConfigurationException;
import org.springframework.stereotype.Component;

@Component
public class AudioAnswerEvaluator extends ObjectiveAnswerEvaluator {
    public AudioAnswerEvaluator(JsonSupport json) { super(json); }

    @Override public EvaluationMode mode() { return EvaluationMode.AUDIO_OBJECTIVE; }

    @Override
    public QuestionResultView evaluate(EvaluationItem item) {
        if (item.getExercise().getAudioUrl() == null || item.getExercise().getAudioUrl().isBlank()) {
            throw new ExamConfigurationException("Audio exercise has no audioUrl: " + item.getExercise().getId());
        }
        return super.evaluate(item);
    }
}

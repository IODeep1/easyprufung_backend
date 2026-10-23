package com.smartpruefung.backend.exam.service;

import com.smartpruefung.backend.exam.domain.EvaluationMode;
import com.smartpruefung.backend.exam.dto.QuestionResultView;

public interface AnswerEvaluator {
    EvaluationMode mode();
    QuestionResultView evaluate(EvaluationItem item);
}

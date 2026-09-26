package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.domain.EvaluationMode;
import com.easyprufung.backend.exam.dto.QuestionResultView;

public interface AnswerEvaluator {
    EvaluationMode mode();
    QuestionResultView evaluate(EvaluationItem item);
}

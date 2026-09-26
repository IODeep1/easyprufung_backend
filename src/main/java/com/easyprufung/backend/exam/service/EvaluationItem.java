package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.domain.ExamSession;
import com.easyprufung.backend.exam.domain.ExerciseInstance;
import com.easyprufung.backend.exam.domain.QuestionInstance;
import com.easyprufung.backend.exam.dto.AnswerPayload;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EvaluationItem {
    private final ExamSession session;
    private final ExerciseInstance exercise;
    private final QuestionInstance question;
    private final AnswerPayload answer;
}

package com.smartpruefung.backend.exam.service;

import com.smartpruefung.backend.exam.domain.ExamSession;
import com.smartpruefung.backend.exam.domain.ExerciseInstance;
import com.smartpruefung.backend.exam.domain.QuestionInstance;
import com.smartpruefung.backend.exam.dto.AnswerPayload;
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

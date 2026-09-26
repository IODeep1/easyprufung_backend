package com.easyprufung.backend.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class SubmitExamRequest {
    private String compactAnswers;
    private List<@Valid AnswerPayload> answers = new ArrayList<>();
}

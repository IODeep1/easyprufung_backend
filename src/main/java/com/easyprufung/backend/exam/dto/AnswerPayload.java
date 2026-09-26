package com.easyprufung.backend.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class AnswerPayload {
    @NotBlank private String questionNumber;
    private List<String> selectedOptionKeys = new ArrayList<>();
    private String text;
}

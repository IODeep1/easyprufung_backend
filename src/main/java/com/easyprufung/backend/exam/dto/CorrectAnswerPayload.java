package com.easyprufung.backend.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CorrectAnswerPayload {
    private List<String> acceptedOptionKeys = new ArrayList<>();
    private List<String> acceptedTexts = new ArrayList<>();
}

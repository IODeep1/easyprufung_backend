package com.easyprufung.backend.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class QuestionResultView {
    private String number;
    private boolean correct;
    private BigDecimal score;
    private BigDecimal maximumScore;
    private List<String> submittedAnswers;
    private List<String> correctAnswers;
    private String explanation;
}

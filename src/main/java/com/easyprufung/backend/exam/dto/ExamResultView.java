package com.easyprufung.backend.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class ExamResultView {
    private UUID sessionId;
    private BigDecimal score;
    private BigDecimal maximumScore;
    private BigDecimal percentage;
    private boolean passed;
    private List<SectionResultView> sections;
    private List<QuestionResultView> questions;
    private String overallFeedback;
    private Instant evaluatedAt;
}

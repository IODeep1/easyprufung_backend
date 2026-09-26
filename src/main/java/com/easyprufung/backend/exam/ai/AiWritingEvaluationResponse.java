package com.easyprufung.backend.exam.ai;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class AiWritingEvaluationResponse {
    @NotBlank private String schemaVersion;
    @NotNull @DecimalMin("0.0") private BigDecimal score;
    @NotNull @DecimalMin("0.0") private BigDecimal maximumScore;
    @NotEmpty private List<@Valid CriterionResult> criteria = new ArrayList<>();
    @NotBlank private String overallFeedback;
    @NotNull private List<String> corrections = new ArrayList<>();
    @NotNull private List<String> strengths = new ArrayList<>();

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class CriterionResult {
        @NotBlank private String key;
        @NotBlank private String title;
        @NotNull @DecimalMin("0.0") private BigDecimal score;
        @NotNull @DecimalMin("0.0") private BigDecimal maximumScore;
        @NotBlank private String feedback;
    }
}

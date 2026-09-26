package com.easyprufung.backend.exam.ai;

import com.easyprufung.backend.exam.domain.CefrLevel;
import com.easyprufung.backend.exam.domain.ExamProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class AiWritingEvaluationRequest {
    @NotBlank private String schemaVersion;
    @NotBlank private String examCode;
    @NotNull private ExamProvider provider;
    @NotNull private CefrLevel level;
    @NotBlank private String taskInstructions;
    private String sourceContent;
    @NotBlank private String candidateText;
    @NotNull @DecimalMin("0.0") private BigDecimal maximumScore;
    private Integer minimumWords;
    private Integer maximumWords;
    @NotBlank private String rubricJson;
    @NotBlank private String locale;
}

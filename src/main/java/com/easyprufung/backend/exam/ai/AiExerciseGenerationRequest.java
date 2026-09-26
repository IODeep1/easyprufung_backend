package com.easyprufung.backend.exam.ai;

import com.easyprufung.backend.exam.domain.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class AiExerciseGenerationRequest {
    @NotBlank private String schemaVersion;
    @NotBlank private String examCode;
    @NotNull private ExamProvider provider;
    @NotNull private CefrLevel level;
    @NotBlank private String sectionKey;
    @NotBlank private String partKey;
    @NotNull private QuestionType questionType;
    @Min(1) private int firstQuestionNumber;
    @Min(1) private int questionCount;
    @NotNull @DecimalMin("0.0") private BigDecimal pointsPerQuestion;
    private Integer minimumWords;
    private Integer maximumWords;
    @NotBlank private String generationInstructions;
    @NotBlank private String locale;
}

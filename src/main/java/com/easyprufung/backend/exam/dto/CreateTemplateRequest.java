package com.easyprufung.backend.exam.dto;

import com.easyprufung.backend.exam.domain.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateTemplateRequest {
    @NotBlank private String templateKey;
    @NotBlank private String instructions;
    private String content;
    private String audioUrl;
    private String audioContentType;
    @Positive private Integer audioPlayLimit;
    @NotEmpty private List<@Valid TemplateQuestionRequest> questions = new ArrayList<>();

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class TemplateQuestionRequest {
        @NotBlank private String number;
        private String stimulus;
        @NotBlank private String prompt;
        @NotNull private QuestionType type;
        @NotNull private List<@Valid OptionDto> options = new ArrayList<>();
        @NotNull @Valid private CorrectAnswerPayload correctAnswer;
        @NotNull @DecimalMin("0.0") private BigDecimal maximumScore;
        private String explanation;
    }
}

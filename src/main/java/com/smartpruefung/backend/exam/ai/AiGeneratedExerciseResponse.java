package com.smartpruefung.backend.exam.ai;

import com.smartpruefung.backend.exam.domain.QuestionType;
import com.smartpruefung.backend.exam.dto.CorrectAnswerPayload;
import com.smartpruefung.backend.exam.dto.OptionDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.validation.Valid;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class AiGeneratedExerciseResponse {
    @NotBlank private String schemaVersion;
    @NotBlank private String instructions;
    private String content;
    @NotEmpty private List<@Valid GeneratedQuestion> questions = new ArrayList<>();

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class GeneratedQuestion {
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

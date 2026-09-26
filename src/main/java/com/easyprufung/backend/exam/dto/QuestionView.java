package com.easyprufung.backend.exam.dto;

import com.easyprufung.backend.exam.domain.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data @NoArgsConstructor @AllArgsConstructor
public class QuestionView {
    private UUID id;
    private String number;
    private String prompt;
    private QuestionType type;
    private List<OptionDto> options;
    private BigDecimal maximumScore;
}

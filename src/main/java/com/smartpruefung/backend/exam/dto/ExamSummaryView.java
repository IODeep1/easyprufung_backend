package com.smartpruefung.backend.exam.dto;

import com.smartpruefung.backend.exam.domain.CefrLevel;
import com.smartpruefung.backend.exam.domain.ExamProvider;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class ExamSummaryView {
    private String code;
    private String title;
    private ExamProvider provider;
    private CefrLevel level;
    private int version;
    private BigDecimal maximumScore;
    private BigDecimal passPercentage;
}

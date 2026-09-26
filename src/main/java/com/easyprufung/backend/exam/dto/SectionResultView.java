package com.easyprufung.backend.exam.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class SectionResultView {
    private String sectionKey;
    private String title;
    private BigDecimal score;
    private BigDecimal maximumScore;
}

package com.smartpruefung.backend.exam.dto;

import lombok.Value;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Value
public class PublicExamCertificate {
    String name;
    String level;
    Instant evaluatedAt;
    BigDecimal score;
    BigDecimal maximumScore;
    BigDecimal percentage;
    List<SectionResultView> sections;
}

package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "exam_results", uniqueConstraints =
        @UniqueConstraint(name = "uk_result_session", columnNames = "exam_session_id"))
@Getter @Setter @NoArgsConstructor
public class ExamResult extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_session_id")
    private ExamSession examSession;
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal score;
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal maximumScore;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal percentage;
    @Column(nullable = false)
    private boolean passed;
    @Lob @Column(nullable = false)
    private String sectionResultsJson;
    @Lob @Column(nullable = false)
    private String questionResultsJson;
    @Lob
    private String overallFeedback;
    @Column(nullable = false)
    private Instant evaluatedAt;
}

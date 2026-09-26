package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "part_definitions")
@Getter @Setter @NoArgsConstructor
public class PartDefinition extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private SectionDefinition sectionDefinition;
    @Column(nullable = false, length = 60)
    private String partKey;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false)
    private int orderIndex;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private ContentSource contentSource;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private EvaluationMode evaluationMode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private QuestionType questionType;
    @Column(nullable = false)
    private int firstQuestionNumber;
    @Column(nullable = false)
    private int questionCount;
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal pointsPerQuestion;
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal maximumScore;
    private Integer durationSeconds;
    private Integer minimumWords;
    private Integer maximumWords;
    @Column(length = 500)
    private String answerPattern;
    @Lob
    private String generationInstructions;
    @Lob
    private String evaluationRubricJson;
}

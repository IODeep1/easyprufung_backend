package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "question_instances", uniqueConstraints =
        @UniqueConstraint(name = "uk_exercise_question_number", columnNames = {"exercise_instance_id", "external_number"}))
@Getter @Setter @NoArgsConstructor
public class QuestionInstance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exercise_instance_id")
    private ExerciseInstance exerciseInstance;
    @Column(nullable = false)
    private int orderIndex;
    @Column(name = "external_number", nullable = false, length = 20)
    private String externalNumber;
    @Lob
    private String stimulus;
    @Lob @Column(nullable = false)
    private String prompt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private QuestionType questionType;
    @Lob @Column(nullable = false)
    private String optionsJson = "[]";
    @Lob @Column(nullable = false)
    private String correctAnswerJson;
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal maximumScore;
    @Lob
    private String explanation;
}

package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_definitions", uniqueConstraints =
        @UniqueConstraint(name = "uk_exam_definition", columnNames = {"code", "definition_version"}))
@Getter @Setter @NoArgsConstructor
public class ExamDefinition extends BaseEntity {
    @Column(nullable = false, length = 80)
    private String code;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private ExamProvider provider;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 5)
    private CefrLevel level;
    @Column(nullable = false)
    private String title;
    @Column(name = "definition_version", nullable = false)
    private int definitionVersion;
    @Column(nullable = false)
    private boolean active;
    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal maximumScore;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal passPercentage;
    @Column(nullable = false)
    private String scoreRounding = "HALF_UP";
    @OneToMany(mappedBy = "examDefinition", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<SectionDefinition> sections = new ArrayList<>();

    public void addSection(SectionDefinition section) {
        sections.add(section);
        section.setExamDefinition(this);
    }
}

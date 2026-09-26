package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercise_templates", uniqueConstraints =
        @UniqueConstraint(name = "uk_part_template", columnNames = {"part_definition_id", "template_key"}))
@Getter @Setter @NoArgsConstructor
public class ExerciseTemplate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "part_definition_id")
    private PartDefinition partDefinition;
    @Column(name = "template_key", nullable = false)
    private String templateKey;
    @Lob @Column(nullable = false)
    private String instructions;
    @Lob
    private String content;
    private String audioUrl;
    private String audioContentType;
    private Integer audioPlayLimit;
    @Column(nullable = false)
    private boolean active = true;
    @OneToMany(mappedBy = "exerciseTemplate", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuestionTemplate> questions = new ArrayList<>();

    public void addQuestion(QuestionTemplate question) {
        questions.add(question);
        question.setExerciseTemplate(this);
    }
}

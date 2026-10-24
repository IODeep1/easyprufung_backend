package com.smartpruefung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercise_instances", indexes = {
        @Index(name = "idx_exercise_pool", columnList = "pool_key"),
        @Index(name = "idx_exercise_legacy", columnList = "part_definition_id,pool_key")
})
@Getter @Setter @NoArgsConstructor
public class ExerciseInstance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ExamSession examSession;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PartDefinition partDefinition;
    // Scalar provenance survives independently of mutable templates and pool maintenance.
    @Column(name = "pool_key", length = 64)
    private String poolKey;
    @Column(name = "content_key", length = 64)
    private String contentKey;
    @Column(nullable = false)
    private int orderIndex;
    @Lob @Column(nullable = false)
    private String instructions;
    @Lob
    private String content;
    private String audioUrl;
    private String audioContentType;
    private Integer audioPlayLimit;
    @OneToMany(mappedBy = "exerciseInstance", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<QuestionInstance> questions = new ArrayList<>();

    public void addQuestion(QuestionInstance question) {
        questions.add(question);
        question.setExerciseInstance(this);
    }
}

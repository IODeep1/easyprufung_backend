package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exercise_instances")
@Getter @Setter @NoArgsConstructor
public class ExerciseInstance extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ExamSession examSession;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private PartDefinition partDefinition;
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

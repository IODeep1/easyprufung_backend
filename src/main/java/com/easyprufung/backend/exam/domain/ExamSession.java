package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_sessions", indexes = @Index(name = "idx_session_user", columnList = "user_id"))
@Getter @Setter @NoArgsConstructor
public class ExamSession extends BaseEntity {
    @Column(name = "user_id", nullable = false)
    private String userId;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private ExamDefinition examDefinition;
    @Column(nullable = false)
    private int definitionVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private SessionStatus status;
    @Column(nullable = false)
    private Instant createdAt;
    private Instant startedAt;
    private Instant expiresAt;
    private Instant submittedAt;
    @OneToMany(mappedBy = "examSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("orderIndex ASC")
    private List<ExerciseInstance> exercises = new ArrayList<>();

    public void addExercise(ExerciseInstance exercise) {
        exercises.add(exercise);
        exercise.setExamSession(this);
    }
}

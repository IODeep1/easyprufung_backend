package com.easyprufung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "user_answers", uniqueConstraints =
        @UniqueConstraint(name = "uk_session_question_answer", columnNames = {"exam_session_id", "question_instance_id"}))
@Getter @Setter @NoArgsConstructor
public class UserAnswer extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "exam_session_id")
    private ExamSession examSession;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_instance_id")
    private QuestionInstance questionInstance;
    @Lob @Column(nullable = false)
    private String answerJson;
    @Column(nullable = false)
    private Instant answeredAt;
}

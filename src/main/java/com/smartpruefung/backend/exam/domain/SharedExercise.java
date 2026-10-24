package com.smartpruefung.backend.exam.domain;

import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

/** Shared immutable AI output; session-specific question IDs are created on delivery. */
@Entity
@Table(name = "shared_exercises", uniqueConstraints = @UniqueConstraint(
        name = "uk_pool_content", columnNames = {"pool_key", "content_key"}))
@Getter @Setter
public class SharedExercise extends BaseEntity {
    @Column(name = "pool_key", nullable = false, length = 64)
    private String poolKey;
    @Column(name = "content_key", nullable = false, length = 64)
    private String contentKey;
    @org.hibernate.annotations.Type(type = "text")
    @Column(nullable = false)
    private String payload;
    @Column(nullable = false)
    private Instant createdAt;
}

package com.easyprufung.backend.exam.domain;

import javax.persistence.*;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {
    @Id
    @Type(type = "uuid-char")
    @Column(length = 36, nullable = false, updatable = false)
    private UUID id;

    @Version
    private long entityVersion;

    @PrePersist
    void assignId() {
        if (id == null) id = UUID.randomUUID();
    }
}

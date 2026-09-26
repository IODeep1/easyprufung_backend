package com.easyprufung.backend.exam.repository;

import com.easyprufung.backend.exam.domain.ExamResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExamResultRepository extends JpaRepository<ExamResult, UUID> {
    Optional<ExamResult> findByExamSessionId(UUID sessionId);
}

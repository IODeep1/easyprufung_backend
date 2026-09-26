package com.easyprufung.backend.exam.repository;

import com.easyprufung.backend.exam.domain.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ExamSessionRepository extends JpaRepository<ExamSession, UUID> {
    @Query("select s from ExamSession s where s.id = :id")
    Optional<ExamSession> findDetailedById(@Param("id") UUID id);
}

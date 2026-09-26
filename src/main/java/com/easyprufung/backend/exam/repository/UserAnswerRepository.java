package com.easyprufung.backend.exam.repository;

import com.easyprufung.backend.exam.domain.UserAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UserAnswerRepository extends JpaRepository<UserAnswer, UUID> {
    List<UserAnswer> findByExamSessionId(UUID sessionId);
    void deleteByExamSessionId(UUID sessionId);
}

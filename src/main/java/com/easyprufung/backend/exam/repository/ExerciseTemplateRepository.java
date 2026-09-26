package com.easyprufung.backend.exam.repository;

import com.easyprufung.backend.exam.domain.ExerciseTemplate;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExerciseTemplateRepository extends JpaRepository<ExerciseTemplate, UUID> {
    @EntityGraph(attributePaths = "questions")
    List<ExerciseTemplate> findByPartDefinitionIdAndActiveTrueOrderByTemplateKeyAsc(UUID partId);
}

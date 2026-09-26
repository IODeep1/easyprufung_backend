package com.easyprufung.backend.exam.repository;

import com.easyprufung.backend.exam.domain.PartDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PartDefinitionRepository extends JpaRepository<PartDefinition, UUID> {
    Optional<PartDefinition> findBySectionDefinitionExamDefinitionCodeAndPartKeyAndSectionDefinitionExamDefinitionActiveTrue(
            String examCode, String partKey);
}

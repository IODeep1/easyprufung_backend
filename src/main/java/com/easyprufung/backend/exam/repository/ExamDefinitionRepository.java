package com.easyprufung.backend.exam.repository;

import com.easyprufung.backend.exam.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamDefinitionRepository extends JpaRepository<ExamDefinition, UUID> {
    Optional<ExamDefinition> findFirstByProviderAndLevelAndCodeAndActiveTrueOrderByDefinitionVersionDesc(
            ExamProvider provider, CefrLevel level, String code);

    Optional<ExamDefinition> findFirstByProviderAndLevelAndActiveTrueOrderByDefinitionVersionDesc(
            ExamProvider provider, CefrLevel level);

    List<ExamDefinition> findByActiveTrueOrderByProviderAscLevelAscCodeAsc();
    boolean existsByCodeAndDefinitionVersion(String code, int definitionVersion);
}

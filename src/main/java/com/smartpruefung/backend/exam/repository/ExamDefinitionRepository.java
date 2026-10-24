package com.smartpruefung.backend.exam.repository;

import com.smartpruefung.backend.exam.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamDefinitionRepository extends JpaRepository<ExamDefinition, UUID> {
    // One cross-process lock covers selection, generation and session/history commit.
    @org.springframework.data.jpa.repository.Lock(javax.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from ExamDefinition d where d.id = :id")
    Optional<ExamDefinition> lockForStart(@org.springframework.data.repository.query.Param("id") UUID id);

    Optional<ExamDefinition> findFirstByProviderAndLevelAndCodeAndActiveTrueOrderByDefinitionVersionDesc(
            ExamProvider provider, CefrLevel level, String code);

    Optional<ExamDefinition> findFirstByProviderAndLevelAndActiveTrueOrderByDefinitionVersionDesc(
            ExamProvider provider, CefrLevel level);

    List<ExamDefinition> findByActiveTrueOrderByProviderAscLevelAscCodeAsc();
    boolean existsByCodeAndDefinitionVersion(String code, int definitionVersion);
}

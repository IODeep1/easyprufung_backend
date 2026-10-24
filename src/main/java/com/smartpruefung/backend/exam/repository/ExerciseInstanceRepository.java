package com.smartpruefung.backend.exam.repository;

import com.smartpruefung.backend.exam.domain.ExerciseInstance;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ExerciseInstanceRepository extends JpaRepository<ExerciseInstance, UUID> {
    @EntityGraph(attributePaths = "questions")
    @Query("select e from ExerciseInstance e where e.examSession.userId = :userId "
            + "and (e.poolKey = :poolKey or (e.poolKey is null and e.partDefinition.id = :partId))")
    List<ExerciseInstance> history(@Param("userId") String userId,
            @Param("poolKey") String poolKey, @Param("partId") UUID partId);

    @EntityGraph(attributePaths = "questions")
    List<ExerciseInstance> findByPartDefinitionIdAndPoolKeyIsNull(UUID partId);
}

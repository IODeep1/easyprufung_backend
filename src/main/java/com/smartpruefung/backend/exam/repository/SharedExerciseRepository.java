package com.smartpruefung.backend.exam.repository;

import com.smartpruefung.backend.exam.domain.SharedExercise;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

public interface SharedExerciseRepository extends JpaRepository<SharedExercise, UUID> {
    List<SharedExercise> findByPoolKey(String poolKey);
    Optional<SharedExercise> findByPoolKeyAndContentKey(String poolKey, String contentKey);
}

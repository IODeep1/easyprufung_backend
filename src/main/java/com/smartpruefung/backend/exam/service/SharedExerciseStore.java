package com.smartpruefung.backend.exam.service;

import com.smartpruefung.backend.exam.ai.AiGeneratedExerciseResponse;
import com.smartpruefung.backend.exam.domain.SharedExercise;
import com.smartpruefung.backend.exam.repository.SharedExerciseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;

@Service
public class SharedExerciseStore {
    private final SharedExerciseRepository repository;
    private final ExerciseFingerprint fingerprint;
    private final JsonSupport json;
    public SharedExerciseStore(SharedExerciseRepository repository, ExerciseFingerprint fingerprint, JsonSupport json) {
        this.repository = repository; this.fingerprint = fingerprint; this.json = json;
    }

    // Valid AI output remains reusable even if another part or session creation fails.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public SharedExercise save(String poolKey, AiGeneratedExerciseResponse response) {
        String key = fingerprint.content(response);
        return repository.findByPoolKeyAndContentKey(poolKey, key).orElseGet(() -> {
            SharedExercise entry = new SharedExercise();
            entry.setPoolKey(poolKey);
            entry.setContentKey(key);
            entry.setPayload(json.write(response));
            entry.setCreatedAt(Instant.now());
            return repository.saveAndFlush(entry);
        });
    }
}

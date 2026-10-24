package com.smartpruefung.backend.exam.service;

import com.smartpruefung.backend.exam.ai.*;
import com.smartpruefung.backend.exam.domain.*;
import com.smartpruefung.backend.exam.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import java.util.stream.Collectors;

/** Stable identities exclude question numbering, scoring, instructions and explanations. */
@Component
public class ExerciseFingerprint {
    private final JsonSupport json;
    public ExerciseFingerprint(JsonSupport json) { this.json = json; }

    public String pool(AiExerciseGenerationRequest request, int definitionVersion, ContentSource source) {
        return hash(json.write(Arrays.asList("pool-v1", definitionVersion, source, request)));
    }

    public AiGeneratedExerciseResponse snapshot(ExerciseInstance exercise) {
        AiGeneratedExerciseResponse result = new AiGeneratedExerciseResponse();
        result.setSchemaVersion("1.0");
        result.setInstructions(exercise.getInstructions());
        result.setContent(exercise.getContent());
        exercise.getQuestions().stream().sorted(Comparator.comparingInt(QuestionInstance::getOrderIndex))
                .forEach(q -> result.getQuestions().add(new AiGeneratedExerciseResponse.GeneratedQuestion(
                        q.getExternalNumber(), q.getStimulus(), q.getPrompt(), q.getQuestionType(),
                        json.read(q.getOptionsJson(), new TypeReference<List<OptionDto>>() {}),
                        json.read(q.getCorrectAnswerJson(), CorrectAnswerPayload.class),
                        q.getMaximumScore(), q.getExplanation())));
        return result;
    }

    public Set<String> questions(AiGeneratedExerciseResponse response) {
        return response.getQuestions().stream().map(q -> hash(json.write(Arrays.asList(
                normalize(response.getContent()), normalize(q.getStimulus()), normalize(q.getPrompt()),
                q.getType(), q.getOptions().stream().map(o -> normalize(o.getText()))
                        .sorted().collect(Collectors.toList()))))).collect(Collectors.toSet());
    }

    public String content(AiGeneratedExerciseResponse response) {
        return hash(json.write(questions(response).stream().sorted().collect(Collectors.toList())));
    }

    public String listening(ExerciseInstance exercise) {
        return hash(json.write(Arrays.asList(exercise.getAudioUrl(), content(snapshot(exercise)))));
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes) result.append(String.format("%02x", item & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}

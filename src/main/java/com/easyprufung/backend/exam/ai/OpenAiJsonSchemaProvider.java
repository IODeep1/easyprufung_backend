package com.easyprufung.backend.exam.ai;

import com.easyprufung.backend.exam.exception.AiIntegrationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
public class OpenAiJsonSchemaProvider {
    private final JsonNode generatedExerciseSchema;
    private final JsonNode writingEvaluationSchema;

    public OpenAiJsonSchemaProvider(ObjectMapper objectMapper) {
        this.generatedExerciseSchema = load(objectMapper,
                "schemas/ai-generated-exercise-response.schema.json");
        this.writingEvaluationSchema = load(objectMapper,
                "schemas/ai-writing-evaluation-response.schema.json");
    }

    public JsonNode generatedExerciseSchema() {
        return generatedExerciseSchema;
    }

    public JsonNode writingEvaluationSchema() {
        return writingEvaluationSchema;
    }

    private JsonNode load(ObjectMapper objectMapper, String path) {
        try (InputStream input = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readTree(input);
        } catch (IOException ex) {
            throw new AiIntegrationException("Could not load OpenAI JSON schema: " + path, ex);
        }
    }
}

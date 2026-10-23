package com.smartpruefung.backend.exam.config;

import com.smartpruefung.backend.exam.ai.ExamAiClient;
import com.smartpruefung.backend.exam.ai.OpenAiExamAiClient;
import com.smartpruefung.backend.exam.ai.OpenAiJsonSchemaProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.validation.Validator;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AiClientConfiguration {
    @Bean
    ExamAiClient examAiClient(AiProperties properties,
                              Validator validator,
                              ObjectMapper objectMapper,
                              OpenAiJsonSchemaProvider schemaProvider,
                              RestTemplateBuilder builder) {
        RestTemplate restTemplate = builder
                .rootUri(properties.getBaseUrl())
                .setConnectTimeout(properties.getConnectTimeout())
                .setReadTimeout(properties.getReadTimeout())
                .build();
        return new OpenAiExamAiClient(restTemplate, properties, validator, objectMapper, schemaProvider);
    }
}

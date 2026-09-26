package com.easyprufung.backend.exam.config;

import com.easyprufung.backend.exam.ai.ExamAiClient;
import com.easyprufung.backend.exam.ai.OpenAiExamAiClient;
import com.easyprufung.backend.exam.ai.OpenAiJsonSchemaProvider;
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

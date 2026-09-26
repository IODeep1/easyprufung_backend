package com.easyprufung.backend.exam.service;

import com.easyprufung.backend.exam.exception.ExamConfigurationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class JsonSupport {
    private final ObjectMapper objectMapper;

    public JsonSupport(ObjectMapper objectMapper) { this.objectMapper = objectMapper; }

    public String write(Object value) {
        try { return objectMapper.writeValueAsString(value); }
        catch (Exception ex) { throw new ExamConfigurationException("Could not serialize exam JSON: " + ex.getMessage()); }
    }

    public <T> T read(String json, Class<T> type) {
        try { return objectMapper.readValue(json, type); }
        catch (Exception ex) { throw new ExamConfigurationException("Could not parse exam JSON: " + ex.getMessage()); }
    }

    public <T> T read(String json, TypeReference<T> type) {
        try { return objectMapper.readValue(json, type); }
        catch (Exception ex) { throw new ExamConfigurationException("Could not parse exam JSON: " + ex.getMessage()); }
    }
}

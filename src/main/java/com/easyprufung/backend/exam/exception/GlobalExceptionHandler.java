package com.easyprufung.backend.exam.exception;

import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResourceNotFoundException.class)
    ResponseEntity<ApiError> notFound(ResourceNotFoundException ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), Map.of());
    }

    @ExceptionHandler({ExamStateException.class, ExamConfigurationException.class})
    ResponseEntity<ApiError> conflict(RuntimeException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(AiIntegrationException.class)
    ResponseEntity<ApiError> ai(AiIntegrationException ex) {
        return error(HttpStatus.BAD_GATEWAY, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex) {
        Map<String, String> violations = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(e -> e.getField(), e -> e.getDefaultMessage() == null ? "invalid" : e.getDefaultMessage(),
                        (a, b) -> a));
        return error(HttpStatus.BAD_REQUEST, "Request validation failed", violations);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> badRequest(IllegalArgumentException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage(), Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> malformedJson(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Malformed or structurally invalid JSON request", Map.of());
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String message, Map<String, String> violations) {
        return ResponseEntity.status(status).body(new ApiError(
                Instant.now(), status.value(), status.getReasonPhrase(), message, violations));
    }
}

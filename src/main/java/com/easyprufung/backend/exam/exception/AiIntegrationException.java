package com.easyprufung.backend.exam.exception;

public class AiIntegrationException extends RuntimeException {
    public AiIntegrationException(String message) { super(message); }
    public AiIntegrationException(String message, Throwable cause) { super(message, cause); }
}

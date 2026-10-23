package com.smartpruefung.backend.exam.exception;

public class ExpiredSessionException extends ExamStateException {
    public ExpiredSessionException(String message) { super(message); }
}

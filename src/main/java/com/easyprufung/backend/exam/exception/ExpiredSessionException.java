package com.easyprufung.backend.exam.exception;

public class ExpiredSessionException extends ExamStateException {
    public ExpiredSessionException(String message) { super(message); }
}

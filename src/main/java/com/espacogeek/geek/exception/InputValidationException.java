package com.espacogeek.geek.exception;

public class InputValidationException extends RuntimeException {
    public InputValidationException() {
        super("Input validation failed");
    }

    public InputValidationException(String message) {
        super(message);
    }
}

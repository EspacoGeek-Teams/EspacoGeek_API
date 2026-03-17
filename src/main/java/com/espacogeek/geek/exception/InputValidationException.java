package com.espacogeek.geek.exception;

public class InputValidationException extends RuntimeException {
    public InputValidationException() {
        super("Validação de input falhou");
    }

    public InputValidationException(String message) {
        super(message);
    }
}

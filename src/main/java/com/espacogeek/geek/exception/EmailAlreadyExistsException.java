package com.espacogeek.geek.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("E-mail já cadastrado");
    }

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}

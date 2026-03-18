package com.espacogeek.geek.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("Email already registered");
    }

    public EmailAlreadyExistsException(String message) {
        super(message);
    }
}

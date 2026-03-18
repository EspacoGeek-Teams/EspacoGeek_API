package com.espacogeek.geek.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token expired/invalid");
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}

package com.espacogeek.geek.exception;

public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
        super("Token expirado/inválido");
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}

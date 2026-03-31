package com.espacogeek.geek.exception;

public class MediaAlreadyInLibraryException extends RuntimeException {
    public MediaAlreadyInLibraryException() {
        super("Media already in library");
    }
}

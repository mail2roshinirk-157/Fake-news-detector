package com.example.fakenewsdetector.exception;

public class UrlExtractionException extends RuntimeException {
    public UrlExtractionException(String message) {
        super(message);
    }

    public UrlExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}

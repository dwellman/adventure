package com.demo.adventure.buui;

public class MarkdownValidationException extends IllegalArgumentException {
    public MarkdownValidationException(String message) {
        super(message);
    }

    public MarkdownValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

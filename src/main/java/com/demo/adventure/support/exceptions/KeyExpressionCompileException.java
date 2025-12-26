package com.demo.adventure.support.exceptions;

public class KeyExpressionCompileException extends GameBuilderException {
    private final int currentPos;
    private final String input;

    public KeyExpressionCompileException(String message, int currentPos, String input) {
        super(message + " at position " + currentPos + ": " + input);
        this.currentPos = currentPos;
        this.input = input;
    }

    public int getCurrentPos() {
        return currentPos;
    }

    public String getInput() {
        return input;
    }
}

package com.demo.adventure.engine.command;

public class CompilerException extends RuntimeException {

    public CompilerException(String message, Token token) {
        super(message + " at line " + token.line + ", column " + token.column);
    }
}

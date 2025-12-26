package com.demo.adventure.engine.command;

public class Token {
    public final TokenType type;

    public final String lexeme;
    public final Object literal;
    public final int line;
    public final int column;

    public Token(TokenType tokenType, String lexeme, Object literal, int line, int column) {
        this.type = tokenType;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
        this.column = column;
    }

    public Token(TokenType tokenType, String lexeme, int column) {
        this.type = tokenType;
        this.lexeme = lexeme;
        this.literal = lexeme;
        this.line = 0;
        this.column = column;
    }

    public String value() {
        return this.lexeme;
    }

    @Override
    public String toString() {
        if (literal == null || lexeme == null || lexeme.isBlank()) {
            return "[" + line + ":" + column + "] " + this.type;
        } else {
            return "[" + line + ":" + column + "] " + this.type + " '" + this.literal + "'";
        }
    }
}

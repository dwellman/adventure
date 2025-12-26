package com.demo.adventure.support.exceptions;

public class GdlCompileException extends GameBuilderException {
    private final int line;
    private final int column;
    private final String token;
    private final String source;

    public GdlCompileException(String message, int line, int column, String token, String source) {
        super(format(message, line, column, token));
        this.line = line;
        this.column = column;
        this.token = token;
        this.source = source;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getToken() {
        return token;
    }

    public String getSource() {
        return source;
    }

    private static String format(String message, int line, int column, String token) {
        String location = "line " + line + ", column " + column;
        if (token == null || token.isBlank()) {
            return message + " at " + location;
        }
        return message + " at " + location + " near '" + token + "'";
    }
}

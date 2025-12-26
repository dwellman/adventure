package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class GdlScanner {
    private final String source;
    private final List<GdlToken> tokens = new ArrayList<>();
    private final Map<String, GdlTokenType> keywords = new HashMap<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;
    private int column = 1;
    private int tokenLine = 1;
    private int tokenColumn = 1;

    public GdlScanner(String source) {
        this.source = source == null ? "" : source;
        keywords.put("thing", GdlTokenType.THING);
        keywords.put("actor", GdlTokenType.ACTOR);
        keywords.put("fixture", GdlTokenType.FIXTURE);
        keywords.put("true", GdlTokenType.TRUE);
        keywords.put("false", GdlTokenType.FALSE);
    }

    public List<GdlToken> scanTokens() throws GdlCompileException {
        while (!isAtEnd()) {
            start = current;
            tokenLine = line;
            tokenColumn = column;
            scanToken();
        }
        tokens.add(new GdlToken(GdlTokenType.EOF, "", null, line, column, current, current));
        return tokens;
    }

    private void scanToken() throws GdlCompileException {
        char c = advance();
        switch (c) {
            case ' ':
            case '\r':
            case '\t':
                break;
            case '\n':
                line++;
                column = 1;
                break;
            case '(':
                addToken(GdlTokenType.LEFT_PAREN);
                break;
            case ')':
                addToken(GdlTokenType.RIGHT_PAREN);
                break;
            case '[':
                addToken(GdlTokenType.LEFT_BRACKET);
                break;
            case ']':
                addToken(GdlTokenType.RIGHT_BRACKET);
                break;
            case ',':
                addToken(GdlTokenType.COMMA);
                break;
            case '.':
                addToken(GdlTokenType.DOT);
                break;
            case '=':
                addToken(GdlTokenType.EQUAL);
                break;
            case '"':
                scanString();
                break;
            case '-':
                if (isDigit(peek())) {
                    number(true);
                } else {
                    throw error("Unexpected character '-'", tokenLine, tokenColumn, "-");
                }
                break;
            default:
                if (isDigit(c)) {
                    number(false);
                } else if (isAlpha(c)) {
                    identifier();
                } else {
                    throw error("Unexpected character '" + c + "'", tokenLine, tokenColumn, String.valueOf(c));
                }
        }
    }

    private void identifier() {
        while (!isAtEnd() && isAlphaNumeric(peek())) {
            advance();
        }
        String text = source.substring(start, current);
        String normalized = text.toLowerCase(Locale.ROOT);
        GdlTokenType type = keywords.getOrDefault(normalized, GdlTokenType.IDENTIFIER);
        addToken(type, text);
    }

    private void number(boolean hasSign) throws GdlCompileException {
        while (!isAtEnd() && isDigit(peek())) {
            advance();
        }
        boolean hasDecimal = false;
        if (!isAtEnd() && peek() == '.' && isDigit(peekNext())) {
            hasDecimal = true;
            advance();
            while (!isAtEnd() && isDigit(peek())) {
                advance();
            }
        }
        String text = source.substring(start, current);
        Number literal;
        try {
            literal = hasDecimal ? Double.parseDouble(text) : Long.parseLong(text);
        } catch (NumberFormatException ex) {
            String message = hasSign ? "Invalid signed number" : "Invalid number";
            throw error(message, tokenLine, tokenColumn, text);
        }
        addToken(GdlTokenType.NUMBER, literal);
    }

    private void scanString() throws GdlCompileException {
        StringBuilder value = new StringBuilder();
        while (!isAtEnd()) {
            char c = advance();
            if (c == '"') {
                addToken(GdlTokenType.STRING, value.toString());
                return;
            }
            if (c == '\n') {
                line++;
                column = 1;
            }
            if (c == '\\') {
                if (isAtEnd()) {
                    break;
                }
                char escaped = advance();
                switch (escaped) {
                    case '"':
                        value.append('"');
                        break;
                    case '\\':
                        value.append('\\');
                        break;
                    case 'n':
                        value.append('\n');
                        break;
                    case 't':
                        value.append('\t');
                        break;
                    default:
                        value.append(escaped);
                        break;
                }
                continue;
            }
            value.append(c);
        }
        throw error("Unterminated string literal", tokenLine, tokenColumn, "\"");
    }

    private void addToken(GdlTokenType type) {
        addToken(type, source.substring(start, current));
    }

    private void addToken(GdlTokenType type, Object literal) {
        String lexeme = source.substring(start, current);
        tokens.add(new GdlToken(type, lexeme, literal, tokenLine, tokenColumn, start, current));
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        char c = source.charAt(current++);
        column++;
        return c;
    }

    private char peek() {
        if (isAtEnd()) {
            return '\0';
        }
        return source.charAt(current);
    }

    private char peekNext() {
        if (current + 1 >= source.length()) {
            return '\0';
        }
        return source.charAt(current + 1);
    }

    private boolean isAlpha(char c) {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    private GdlCompileException error(String message, int line, int column, String token) {
        return new GdlCompileException(message, line, column, token, source);
    }
}

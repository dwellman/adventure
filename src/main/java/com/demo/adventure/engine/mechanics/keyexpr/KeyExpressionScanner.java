package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.support.exceptions.KeyExpressionCompileException;

import java.util.*;

/**
 * Scanner for the key expression language tokens.
 *
 * Last verified: 2025-12-21 (Codex — deep clean sweep)
 *
 * ## Grammar (EBNF)
 *
 * ```
 * expr        := orExpr ;
 *
 * orExpr      := andExpr ( '||' andExpr )* ;
 * andExpr     := cmpExpr ( '&&' cmpExpr )* ;
 * cmpExpr     := unaryExpr ( ( '==' | '!=' | '<' | '<=' | '>' | '>=' ) unaryExpr )? ;
 *
 * unaryExpr   := '!' unaryExpr
 *             | primary ;
 *
 * primary     := literal
 *             | identifier
 *             | functionCall
 *             | '(' expr ')' ;
 *
 * literal     := 'true' | 'false' | number | string ;
 * number      := '-'? [0-9]+ ;
 * string      := '"' ... '"' ;
 *
 * functionCall := identifier '(' argList? ')' ;
 * argList     := expr ( ',' expr )* ;
 * ```
 */
public final class KeyExpressionScanner {
    private final Map<String, TokenType> keywords = new HashMap<>();

    private final String source;

    private int current = 0;
    private int start = 0;
    private final int line = 1;

    private final List<Token> tokens = new ArrayList<>();

    public KeyExpressionScanner(String source) throws KeyExpressionCompileException {
        keywords.put("TRUE", TokenType.TRUE);
        keywords.put("FALSE", TokenType.FALSE);

        this.source = source == null ? "" : source;
        scanTokens();
    }

    public List<Token> getTokens() {
        return this.tokens;
    }

    private void scanTokens() throws KeyExpressionCompileException {
        while (!isAtEnd()) {
            start = current;
            char ch = advance();

            switch (ch) {
                case ' ':
                case '\r':
                case '\t':
                case '\n':
                    break;

                case '(':
                    addToken(TokenType.LEFT_PAREN);
                    break;

                case ')':
                    addToken(TokenType.RIGHT_PAREN);
                    break;

                case ',':
                    addToken(TokenType.COMMA);
                    break;

                case '.':
                    addToken(TokenType.DOT);
                    break;

                case '-':
                    if (peek() == '>') {
                        addToken(TokenType.POINTER, "->");
                        advance();
                    } else {
                        addToken(TokenType.MINUS, "-");
                    }
                    break;
                case '+':
                    addToken(TokenType.PLUS, "+");
                    break;
                case '*':
                    addToken(TokenType.STAR, "*");
                    break;

                case '=':
                    if (lookahead() == '=') {
                        advance();
                        addToken(TokenType.EQUAL);
                    } else {
                        throw new KeyExpressionCompileException("Expected '=='", start, source);
                    }
                    break;

                case ':':
                    addToken(TokenType.COLON);
                    break;

                case '{':
                    addToken(TokenType.LEFT_ANGLE_BRACKET);
                    break;

                case '}':
                    addToken(TokenType.RIGHT_ANGLE_BRACKET);
                    break;

                case ']':
                    addToken(TokenType.RIGHT_BRACKET);
                    break;

                case '/':
                    if (peek() == '/') {
                        lineComment();
                    } else {
                        addToken(TokenType.SLASH, "/");
                    }
                    break;

                case '|':
                    if (lookahead() == '|') {
                        advance();
                        addToken(TokenType.OR);
                    } else {
                        throw new KeyExpressionCompileException("Expected '||'", start, source);
                    }

                    break;

                case '&':
                    if (lookahead() == '&') {
                        advance();
                        addToken(TokenType.AND);
                    } else {
                        throw new KeyExpressionCompileException("Expected '&&'", start, source);
                    }
                    break;

                case '!':
                    if (lookahead() == '=') {
                        advance();
                        addToken(TokenType.NOT_EQUAL);
                    } else {
                        addToken(TokenType.NOT);
                    }
                    break;

                case '<':
                    if (lookahead() == '=') {
                        advance();
                        addToken(TokenType.LESS_THAN_OR_EQUAL);
                    } else {
                        addToken(TokenType.LESS_THAN);
                    }
                    break;

                case '>':
                    if (lookahead() == '=') {
                        advance();
                        addToken(TokenType.GREATER_THAN_OR_EQUAL);
                    } else {
                        addToken(TokenType.GREATER_THAN);
                    }
                    break;

                case '"':
                    scanString();
                    break;

                default:
                    if (isDigit(ch)) {
                        number();
                    } else if (isAlphaNumeric(ch)) {
                        identifier();
                    } else {
                        throw new KeyExpressionCompileException("Unexpected character '" + ch + "'", start, source);
                    }
                    break;
            }
        }

        addToken(TokenType.EOL);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);

        if (type == TokenType.LITERAL_STRING) {
            text = "" + literal;
        }

        addToken(new Token(type, text, literal, line, start));
    }

    private void addToken(TokenType type) {
        addToken(type, type.toString());
    }

    private void addToken(Token token) {
        tokens.add(token);
    }

    private char next() {
        if (current + 1 > source.length()) {
            return '\0';
        }

        return source.charAt(current + 1);
    }

    // Reserved for upcoming string literal handling in the scanner.
    private Token readString(int start) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd()) {
            char c = advance();
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        return new Token(TokenType.STRING, sb.toString(), start);
    }

    private void identifier() {
        int end = current;
        while (!isAtEnd() && isAlphaNumeric(peek())) {
            advance();
            end = current;
        }

        String literal = source.substring(start, end).trim();
        String lexeme = literal.toUpperCase(Locale.ROOT);
        TokenType tokenType = keywords.get(lexeme);
        if (peek() == '(') {
            tokenType = TokenType.FUNCTION;
        } else if (tokenType == null) {
            tokenType = TokenType.IDENTIFIER;
        }

        addToken(new Token(tokenType, lexeme, literal, line, start));
    }

    private void number() {
        int end = current;
        while (!isAtEnd() && isDigit(peek())) {
            advance();
            end = current;
        }

        boolean hasDecimal = false;
        if (!isAtEnd() && peek() == '.' && isDigit(next())) {
            hasDecimal = true;
            advance(); // consume '.'
            end = current;
            while (!isAtEnd() && isDigit(peek())) {
                advance();
                end = current;
            }
        }

        String literalText = source.substring(start, end);
        Number literalValue;
        if (hasDecimal) {
            literalValue = Double.parseDouble(literalText);
        } else {
            try {
                literalValue = Long.parseLong(literalText);
            } catch (NumberFormatException ex) {
                literalValue = Double.parseDouble(literalText);
            }
        }
        addToken(new Token(TokenType.NUMBER, literalText, literalValue, line, start));
    }

    private void lineComment() {
        while (!isAtEnd()) {
            if (peek() == '\n') {
                advance();
                break;
            }

            advance();
        }
    }

    // Entry point when encountering the opening quote
    private void scanString() throws KeyExpressionCompileException {
        StringBuilder buffer = new StringBuilder();
        boolean inPattern = false;
        addToken(TokenType.INTERPOLATED_STRING_START);
        while (!isAtEnd()) {
            char ch = advance();

            if (inPattern) {
                if (ch == '.') {
                    addToken(TokenType.DOT);
                    start = current;
                } else if (ch == ')') {
                    inPattern = false;
                    buffer = new StringBuilder();
                } else if (isAlphaNumeric(ch)) {
                    identifier();
                    buffer = new StringBuilder();
                }

                continue;
            }

            if (ch == '"') {
                if (!buffer.isEmpty()) {
                    addToken(TokenType.LITERAL_STRING, buffer.toString());
                }

                addToken(TokenType.INTERPOLATED_STRING_END);
                return;
            } else if (ch == '\\') {
                char c2 = advance();
                if (c2 == '(') {
                    inPattern = true;

                    if (!buffer.isEmpty()) {
                        addToken(TokenType.LITERAL_STRING, buffer.toString());
                        buffer = new StringBuilder();
                        this.start = this.current;
                    }
                } else {
                    buffer.append(c2);
                }
                // End of this interpolation

            } else {
                buffer.append(ch);
            }
        }

        // Unterminated string: emit the partial literal if present to preserve token flow.
        if (!buffer.isEmpty()) {
            addToken(TokenType.LITERAL_STRING, clearReturns(buffer.toString()));
            return;
        }
        throw new KeyExpressionCompileException("Unterminated string", current, buffer.toString());

    }

    private static String clearReturns(String value) {
        StringBuilder result = new StringBuilder();
        boolean clearing = false;

        for (int index = 0; index < value.length(); index++) {
            char ch = value.charAt(index);
            if (ch == '\n') {
                result.append(" ");
                clearing = true;
            } else if (clearing && ch == ' ') {
                // skip it
            } else {
                result.append(ch);
                clearing = false;
            }
        }

        return result.toString();
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    private boolean isAlphaNumeric(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char lookahead() {
        if (current + 1 >= source.length()) {
            return '\0';
        }

        return source.charAt(current);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean isAtEnd() {
        return current >= this.source.length();
    }
}

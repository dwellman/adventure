package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GdlParser {
    private final String source;
    private final List<GdlToken> tokens;
    private int current = 0;

    public GdlParser(String source) throws GdlCompileException {
        this.source = source == null ? "" : source;
        this.tokens = new GdlScanner(this.source).scanTokens();
    }

    public GdlParser(List<GdlToken> tokens, String source) {
        this.source = source == null ? "" : source;
        this.tokens = Objects.requireNonNull(tokens, "tokens");
    }

    public GdlProgram parse() throws GdlCompileException {
        List<GdlDeclaration> declarations = new ArrayList<>();
        while (!isAtEnd()) {
            declarations.add(parseDeclaration());
        }
        return new GdlProgram(declarations);
    }

    private GdlDeclaration parseDeclaration() throws GdlCompileException {
        GdlToken typeToken = advance();
        GdlDeclarationType type = switch (typeToken.type()) {
            case THING -> GdlDeclarationType.THING;
            case ACTOR -> GdlDeclarationType.ACTOR;
            default -> throw error("Expected 'thing' or 'actor'", typeToken);
        };

        consume(GdlTokenType.LEFT_PAREN, "Expected '(' after declaration keyword");
        String subjectId = parseId("Expected identifier or string for declaration id");
        consume(GdlTokenType.RIGHT_PAREN, "Expected ')' after declaration id");
        consume(GdlTokenType.DOT, "Expected '.' after declaration");

        String fixtureId = parseFixtureId();
        consume(GdlTokenType.DOT, "Expected '.' after fixture declaration");

        Map<String, GdlAttribute> attributes = new LinkedHashMap<>();
        parseAttributeAssignment(attributes);
        while (match(GdlTokenType.DOT)) {
            parseAttributeAssignment(attributes);
        }

        return new GdlDeclaration(type, subjectId, fixtureId, Map.copyOf(attributes), typeToken.line(), typeToken.column());
    }

    private String parseFixtureId() throws GdlCompileException {
        GdlToken token = consume(GdlTokenType.FIXTURE, "Expected 'fixture' declaration");
        consume(GdlTokenType.LEFT_PAREN, "Expected '(' after fixture");
        String fixtureId = parseId("Expected identifier or string for fixture id");
        consume(GdlTokenType.RIGHT_PAREN, "Expected ')' after fixture id");
        return fixtureId;
    }

    private void parseAttributeAssignment(Map<String, GdlAttribute> attributes) throws GdlCompileException {
        GdlToken nameToken = consume(GdlTokenType.IDENTIFIER, "Expected attribute name");
        String name = nameToken.literal() == null ? nameToken.lexeme() : nameToken.literal().toString();
        String normalized = name.toLowerCase(java.util.Locale.ROOT);
        consume(GdlTokenType.EQUAL, "Expected '=' after attribute name");
        GdlValue value = parseValue();
        if (attributes.containsKey(normalized)) {
            throw error("Duplicate attribute '" + normalized + "'", nameToken);
        }
        attributes.put(normalized, new GdlAttribute(value, nameToken.line(), nameToken.column()));
    }

    private GdlValue parseValue() throws GdlCompileException {
        if (match(GdlTokenType.STRING)) {
            String value = previous().literal() == null ? "" : previous().literal().toString();
            return new GdlValue.GdlString(value);
        }
        if (match(GdlTokenType.NUMBER)) {
            Object literal = previous().literal();
            double number = literal instanceof Number n ? n.doubleValue() : 0.0;
            return new GdlValue.GdlNumber(number);
        }
        if (match(GdlTokenType.TRUE)) {
            return new GdlValue.GdlBoolean(true);
        }
        if (match(GdlTokenType.FALSE)) {
            return new GdlValue.GdlBoolean(false);
        }
        if (match(GdlTokenType.LEFT_BRACKET)) {
            return parseList();
        }
        GdlToken token = peek();
        throw error("Expected literal value (wrap expressions in quotes)", token);
    }

    private GdlValue parseList() throws GdlCompileException {
        List<GdlValue> values = new ArrayList<>();
        if (!check(GdlTokenType.RIGHT_BRACKET)) {
            do {
                values.add(parseValue());
            } while (match(GdlTokenType.COMMA));
        }
        consume(GdlTokenType.RIGHT_BRACKET, "Expected ']' after list");
        return new GdlValue.GdlList(List.copyOf(values));
    }

    private String parseId(String message) throws GdlCompileException {
        if (match(GdlTokenType.STRING)) {
            Object literal = previous().literal();
            return literal == null ? "" : literal.toString();
        }
        if (match(GdlTokenType.IDENTIFIER)) {
            Object literal = previous().literal();
            return literal == null ? "" : literal.toString();
        }
        throw error(message, peek());
    }

    private boolean match(GdlTokenType type) {
        if (check(type)) {
            advance();
            return true;
        }
        return false;
    }

    private GdlToken consume(GdlTokenType type, String message) throws GdlCompileException {
        if (check(type)) {
            return advance();
        }
        throw error(message, peek());
    }

    private boolean check(GdlTokenType type) {
        if (isAtEnd()) {
            return false;
        }
        return peek().type() == type;
    }

    private GdlToken advance() {
        if (!isAtEnd()) {
            current++;
        }
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type() == GdlTokenType.EOF;
    }

    private GdlToken peek() {
        return tokens.get(current);
    }

    private GdlToken previous() {
        return tokens.get(current - 1);
    }

    private GdlCompileException error(String message, GdlToken token) {
        return new GdlCompileException(
                message,
                token.line(),
                token.column(),
                token.lexeme(),
                source
        );
    }
}

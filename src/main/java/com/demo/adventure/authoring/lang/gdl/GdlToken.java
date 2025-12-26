package com.demo.adventure.authoring.lang.gdl;

public record GdlToken(
        GdlTokenType type,
        String lexeme,
        Object literal,
        int line,
        int column,
        int start,
        int end
) {
}

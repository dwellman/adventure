package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;

import java.util.List;

final class GdlLexingStage {
    private final GdlLexicalValidator validator = new GdlLexicalValidator();

    List<GdlToken> lex(String source) throws GdlCompileException {
        String normalizedSource = source == null ? "" : source;
        List<GdlToken> tokens = new GdlScanner(normalizedSource).scanTokens();
        validator.validate(tokens, normalizedSource);
        return tokens;
    }
}

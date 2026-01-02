package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;

import java.util.List;

final class GdlLexicalValidator {
    void validate(List<GdlToken> tokens, String source) throws GdlCompileException {
        String normalizedSource = source == null ? "" : source;
        if (tokens == null || tokens.isEmpty()) {
            throw new GdlCompileException("No tokens scanned", 1, 1, "", normalizedSource);
        }
        GdlToken last = tokens.get(tokens.size() - 1);
        if (last.type() != GdlTokenType.EOF) {
            throw new GdlCompileException("Scanner did not terminate", last.line(), last.column(), last.lexeme(), normalizedSource);
        }
    }
}

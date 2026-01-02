package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;

import java.util.List;

final class GdlParsingStage {
    private final GdlProgramValidator validator = new GdlProgramValidator();

    GdlProgram parse(List<GdlToken> tokens, String source) throws GdlCompileException {
        String normalizedSource = source == null ? "" : source;
        GdlProgram program = new GdlParser(tokens, normalizedSource).parse();
        validator.validate(program, normalizedSource);
        return program;
    }
}

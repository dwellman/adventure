package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.domain.save.GameSave;

import java.util.List;

public final class GdlCompiler {
    private final GdlLexingStage lexingStage = new GdlLexingStage();
    private final GdlParsingStage parsingStage = new GdlParsingStage();
    private final GdlProgramCompiler programCompiler = new GdlProgramCompiler();

    public GameSave compile(String source) throws GdlCompileException {
        String normalizedSource = source == null ? "" : source;
        List<GdlToken> tokens = lexingStage.lex(normalizedSource);
        GdlProgram program = parsingStage.parse(tokens, normalizedSource);
        return programCompiler.compile(program, normalizedSource);
    }
}

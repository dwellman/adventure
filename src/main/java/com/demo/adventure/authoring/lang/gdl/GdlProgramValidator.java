package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;

final class GdlProgramValidator {
    void validate(GdlProgram program, String source) throws GdlCompileException {
        String normalizedSource = source == null ? "" : source;
        if (program == null) {
            throw new GdlCompileException("Parser did not produce a program", 1, 1, "", normalizedSource);
        }
        if (program.declarations() == null) {
            throw new GdlCompileException("Parser produced null declarations", 1, 1, "", normalizedSource);
        }
    }
}

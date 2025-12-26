package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.domain.save.GameSave;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class GdlLoader {
    private GdlLoader() {
    }

    public static GameSave load(Path path) throws IOException, GdlCompileException {
        Objects.requireNonNull(path, "path");
        String source = Files.readString(path, StandardCharsets.UTF_8);
        return load(source);
    }

    public static GameSave load(String source) throws GdlCompileException {
        return new GdlCompiler().compile(source);
    }
}

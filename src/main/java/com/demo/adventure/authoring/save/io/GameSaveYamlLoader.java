package com.demo.adventure.authoring.save.io;

import com.demo.adventure.domain.save.GameSave;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Parses a YAML game save into a {@link GameSave} using id-based references only.
 */
public final class GameSaveYamlLoader {
    private static final GameSaveYamlReader READER = new GameSaveYamlReader();

    private GameSaveYamlLoader() {
    }

    /**
     * Load a {@link GameSave} from a YAML file.
     *
     * @param path path to YAML document
     * @return parsed save
     * @throws IOException when the file cannot be read
     */
    public static GameSave load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        String yaml = Files.readString(path, StandardCharsets.UTF_8);
        return load(yaml);
    }

    /**
     * Load a {@link GameSave} from YAML text.
     *
     * @param yamlText YAML content
     * @return parsed save
     */
    public static GameSave load(String yamlText) {
        return READER.read(yamlText);
    }
}

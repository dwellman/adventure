package com.demo.adventure.authoring.save.io;

import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.domain.save.GameSave;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Utility to emit a {@link GameSave} into a deterministic YAML document.
 */
public final class GameSaveYamlWriter {
    private GameSaveYamlWriter() {
    }

    /**
     * Write a {@link GameSave} to disk as YAML using deterministic ordering.
     *
     * @param save snapshot to emit
     * @param path target file path
     * @throws IOException when the file cannot be written
     */
    public static void write(GameSave save, Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, toYaml(save), StandardCharsets.UTF_8);
    }

    /**
     * Write a {@link GardenResult} (registry after Gardener pass) to disk as YAML using deterministic ordering.
     *
     * @param result gardened world result
     * @param path   target file path
     * @throws IOException when the file cannot be written
     */
    public static void write(GardenResult result, Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, toYaml(result), StandardCharsets.UTF_8);
    }

    /**
     * Render a {@link GameSave} as a YAML string using deterministic ordering.
     *
     * @param save snapshot to emit
     * @return YAML text
     */
    public static String toYaml(GameSave save) {
        Objects.requireNonNull(save, "save");
        Yaml yaml = new Yaml(GameSaveYamlWriterSupport.options());
        return yaml.dump(GameSaveYamlDocumentBuilder.toDocument(save));
    }

    /**
     * Render a {@link GardenResult} as a YAML string using deterministic ordering.
     *
     * @param result gardened world to emit
     * @return YAML text
     */
    public static String toYaml(GardenResult result) {
        Objects.requireNonNull(result, "result");
        Yaml yaml = new Yaml(GameSaveYamlWriterSupport.options());
        return yaml.dump(GameSaveYamlDocumentBuilder.toDocument(result));
    }
}

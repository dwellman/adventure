package com.demo.adventure.authoring.save.io;

import com.demo.adventure.domain.save.GameSave;
import org.yaml.snakeyaml.Yaml;

import java.util.Map;
import java.util.Objects;

final class GameSaveYamlReader {
    GameSave read(String yamlText) {
        Objects.requireNonNull(yamlText, "yamlText");
        Yaml yaml = new Yaml();
        Object raw = yaml.load(yamlText);
        if (!(raw instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("YAML root must be a mapping");
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rootMap = (Map<String, Object>) root;
        GameSaveYamlInputs inputs = GameSaveYamlInputCollector.collect(rootMap);
        return GameSaveYamlAssembler.assemble(inputs);
    }
}

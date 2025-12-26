package com.demo.adventure.engine.command;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads per-game verb aliases from YAML.
 *
 * Expected shape:
 * aliases:
 *   - alias: EXAMINE
 *     canonical: INSPECT
 */
public final class VerbAliasLoader {
    private VerbAliasLoader() {
    }

    public static Map<String, TokenType> load(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return Map.of();
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return load(text);
    }

    public static Map<String, TokenType> load(InputStream in) throws IOException {
        if (in == null) {
            return Map.of();
        }
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        return load(text);
    }

    public static Map<String, TokenType> load(String yamlText) {
        if (yamlText == null || yamlText.isBlank()) {
            return Map.of();
        }
        Object raw = new Yaml().load(yamlText);
        if (!(raw instanceof Map<?, ?> root)) {
            return Map.of();
        }
        Object aliasesObj = root.get("aliases");
        if (aliasesObj == null) {
            return Map.of();
        }
        Map<String, TokenType> aliases = new LinkedHashMap<>();
        if (aliasesObj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String alias = asString(entry.getKey());
                String canonical = asString(entry.getValue());
                addAlias(aliases, alias, canonical);
            }
        } else if (aliasesObj instanceof List<?> list) {
            for (Object entry : list) {
                if (!(entry instanceof Map<?, ?> map)) {
                    continue;
                }
                String alias = asString(map.get("alias"));
                String canonical = asString(map.get("canonical"));
                addAlias(aliases, alias, canonical);
            }
        }
        if (aliases.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(aliases);
    }

    private static void addAlias(Map<String, TokenType> aliases, String alias, String canonical) {
        if (alias == null || alias.isBlank() || canonical == null || canonical.isBlank()) {
            return;
        }
        TokenType type = parseTokenType(canonical);
        if (type == null) {
            return;
        }
        aliases.put(alias.trim().toUpperCase(Locale.ROOT), type);
    }

    private static TokenType parseTokenType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TokenType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String asString(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}

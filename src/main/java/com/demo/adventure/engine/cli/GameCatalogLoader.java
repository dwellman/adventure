package com.demo.adventure.engine.cli;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class GameCatalogLoader {
    private GameCatalogLoader() {
    }

    static List<GameCatalogEntry> load(String resourcePath) throws IOException {
        Objects.requireNonNull(resourcePath, "resourcePath");
        Path fsPath = Path.of(resourcePath);
        if (Files.exists(fsPath)) {
            return load(fsPath);
        }
        try (InputStream in = GameCatalogLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return parse(text, resourcePath);
        }
    }

    static List<GameCatalogEntry> load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (!Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return parse(text, path.toString());
    }

    private static List<GameCatalogEntry> parse(String yamlText, String source) throws IOException {
        Object raw = new Yaml().load(yamlText);
        if (!(raw instanceof Map<?, ?> root)) {
            throw new IOException("Catalog root must be a map: " + source);
        }
        Object gamesRaw = root.get("games");
        List<Map<?, ?>> entries = new ArrayList<>();
        for (Object entry : list(gamesRaw)) {
            if (entry instanceof Map<?, ?> map) {
                entries.add(map);
            }
        }
        List<GameCatalogEntry> games = new ArrayList<>();
        Set<Integer> indexes = new HashSet<>();
        Set<String> ids = new HashSet<>();
        for (Map<?, ?> map : entries) {
            String id = requireString(map.get("id"), "games.id");
            int index = requireInt(map.get("index"), "games.index");
            String name = requireString(map.get("name"), "games.name");
            String path = requireString(map.get("path"), "games.path");
            String tagline = optionalString(map.get("tagline"), "");
            boolean hidden = bool(map.get("hidden"));
            if (!ids.add(id.toLowerCase())) {
                throw new IOException("Duplicate game id: " + id + " (" + source + ")");
            }
            if (!indexes.add(index)) {
                throw new IOException("Duplicate game index: " + index + " (" + source + ")");
            }
            games.add(new GameCatalogEntry(id, index, name, path, tagline, hidden));
        }
        games.sort((a, b) -> Integer.compare(a.index(), b.index()));
        return games;
    }

    private static List<?> list(Object raw) {
        if (raw instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private static String requireString(Object raw, String field) throws IOException {
        String value = optionalString(raw, null);
        if (value == null || value.isBlank()) {
            throw new IOException("Missing " + field);
        }
        return value;
    }

    private static String optionalString(Object raw, String fallback) {
        if (raw == null) {
            return fallback;
        }
        String value = raw.toString().trim();
        return value.isEmpty() ? fallback : value;
    }

    private static int requireInt(Object raw, String field) throws IOException {
        if (raw == null) {
            throw new IOException("Missing " + field);
        }
        if (raw instanceof Number num) {
            return num.intValue();
        }
        try {
            return Integer.parseInt(raw.toString().trim());
        } catch (NumberFormatException ex) {
            throw new IOException("Invalid " + field + ": " + raw);
        }
    }

    private static boolean bool(Object raw) {
        if (raw instanceof Boolean value) {
            return value;
        }
        if (raw == null) {
            return false;
        }
        return Boolean.parseBoolean(raw.toString().trim());
    }
}

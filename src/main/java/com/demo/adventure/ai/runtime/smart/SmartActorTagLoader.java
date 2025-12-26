package com.demo.adventure.ai.runtime.smart;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class SmartActorTagLoader {
    private SmartActorTagLoader() {
    }

    public static SmartActorTagIndex loadOptional(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return SmartActorTagIndex.empty();
        }
        return load(path);
    }

    public static SmartActorTagIndex load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Map<String, Object> root = readYaml(path);
        return parseIndex(root);
    }

    public static SmartActorTagIndex load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> root = readYaml(text);
        return parseIndex(root);
    }

    private static Map<String, Object> readYaml(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return readYaml(text);
    }

    private static Map<String, Object> readYaml(String text) {
        Object raw = new Yaml().load(text);
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Root must be a map");
        }
        //noinspection unchecked
        return (Map<String, Object>) map;
    }

    private static SmartActorTagIndex parseIndex(Map<String, Object> root) {
        Map<UUID, Set<String>> plotTags = parseTagSection(root.get("plots"), "plot");
        Map<UUID, Set<String>> itemTags = parseTagSection(root.get("items"), "item");
        Map<UUID, Set<String>> fixtureTags = parseTagSection(root.get("fixtures"), "fixture");
        Map<UUID, Set<String>> actorTags = parseTagSection(root.get("actors"), "actor");
        Map<String, Set<String>> questTags = parseQuestSection(root.get("quests"));
        return new SmartActorTagIndex(plotTags, itemTags, fixtureTags, actorTags, questTags);
    }

    private static Map<UUID, Set<String>> parseTagSection(Object raw, String kind) {
        Map<UUID, Set<String>> result = new LinkedHashMap<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String key = requireString(map.get("key"), kind + ".key");
            Set<String> tags = stringSet(map.get("tags"), kind + ".tags");
            if (tags.isEmpty()) {
                continue;
            }
            UUID id = SmartActorIdCodec.uuid(kind, key);
            if (result.putIfAbsent(id, SmartActorTags.normalize(tags)) != null) {
                throw new IllegalArgumentException("Duplicate " + kind + " tag entry for key: " + key);
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Set<String>> parseQuestSection(Object raw) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String key = requireString(map.get("key"), "quests.key");
            Set<String> tags = stringSet(map.get("tags"), "quests.tags");
            if (tags.isEmpty()) {
                continue;
            }
            String normalizedKey = key.trim().toLowerCase(java.util.Locale.ROOT);
            if (result.putIfAbsent(normalizedKey, SmartActorTags.normalize(tags)) != null) {
                throw new IllegalArgumentException("Duplicate quest tag entry for key: " + key);
            }
        }
        return Map.copyOf(result);
    }

    private static List<?> list(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Expected list but got: " + raw.getClass());
    }

    private static String requireString(Object raw, String field) {
        if (raw == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        String value = raw.toString().trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private static Set<String> stringSet(Object raw, String field) {
        if (raw == null) {
            return Set.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new IllegalArgumentException(field + " must be a list");
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String value = entry.toString().trim();
            if (!value.isBlank()) {
                result.add(value);
            }
        }
        return Set.copyOf(result);
    }
}

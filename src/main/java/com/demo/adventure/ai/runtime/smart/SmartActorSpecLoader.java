package com.demo.adventure.ai.runtime.smart;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class SmartActorSpecLoader {
    private SmartActorSpecLoader() {
    }

    public static List<SmartActorSpec> load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        Map<String, Object> root = readYaml(path);
        return parseSpecs(root);
    }

    public static List<SmartActorSpec> load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input");
        String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> root = readYaml(text);
        return parseSpecs(root);
    }

    public static List<SmartActorSpec> load(String yaml) {
        Map<String, Object> root = readYaml(yaml);
        return parseSpecs(root);
    }

    private static List<SmartActorSpec> parseSpecs(Map<String, Object> root) {
        List<SmartActorSpec> specs = new ArrayList<>();
        Set<String> actorKeys = new HashSet<>();
        for (Object entry : list(root.get("smartActors"))) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String actorKey = requireString(map.get("actorKey"), "smartActors.actorKey");
            if (!actorKeys.add(actorKey)) {
                throw new IllegalArgumentException("Duplicate smart actor key: " + actorKey);
            }
            String promptId = requireString(map.get("promptId"), "smartActors.promptId");
            String backstory = optionalString(map.get("backstory"));
            Map<String, Object> persona = mapOf(map.get("persona"));
            Map<String, Object> properties = mapOf(map.get("properties"));
            List<SmartActorMemorySeed> memorySeeds = parseMemorySeeds(map.get("memorySeeds"));
            SmartActorHistorySpec history = parseHistory(map.get("history"));
            SmartActorPolicy policy = parsePolicy(map.get("policy"));
            specs.add(new SmartActorSpec(
                    actorKey,
                    promptId,
                    backstory,
                    persona,
                    properties,
                    memorySeeds,
                    history,
                    policy
            ));
        }
        return List.copyOf(specs);
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

    private static List<?> list(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Expected list but got: " + raw.getClass());
    }

    private static Map<String, Object> mapOf(Object raw) {
        if (raw == null) {
            return Map.of();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            result.put(entry.getKey().toString(), entry.getValue());
        }
        return Map.copyOf(result);
    }

    private static SmartActorHistorySpec parseHistory(Object raw) {
        if (raw == null) {
            return null;
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return null;
        }
        String storeKey = requireString(map.get("storeKey"), "history.storeKey");
        List<SmartActorHistorySeed> seeds = parseHistorySeeds(map.get("seeds"));
        return new SmartActorHistorySpec(storeKey, seeds);
    }

    private static List<SmartActorHistorySeed> parseHistorySeeds(Object raw) {
        List<SmartActorHistorySeed> seeds = new ArrayList<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String id = requireString(map.get("id"), "history.seeds.id");
            String text = requireString(map.get("text"), "history.seeds.text");
            SmartActorHistoryScope scope = parseScope(map.get("scope"), "history.seeds");
            Set<String> tags = stringSet(map.get("tags"));
            seeds.add(new SmartActorHistorySeed(id, text, scope, tags));
        }
        return List.copyOf(seeds);
    }

    private static List<SmartActorMemorySeed> parseMemorySeeds(Object raw) {
        List<SmartActorMemorySeed> seeds = new ArrayList<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String id = requireString(map.get("id"), "memorySeeds.id");
            String text = requireString(map.get("text"), "memorySeeds.text");
            SmartActorHistoryScope scope = parseScope(map.get("scope"), "memorySeeds");
            Set<String> tags = stringSet(map.get("tags"));
            seeds.add(new SmartActorMemorySeed(id, text, scope, tags));
        }
        return List.copyOf(seeds);
    }

    private static SmartActorPolicy parsePolicy(Object raw) {
        if (raw == null) {
            return SmartActorPolicy.empty();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return SmartActorPolicy.empty();
        }
        List<String> allowed = stringList(map.get("allowedVerbs"));
        int maxUtteranceLength = optionalInt(map.get("maxUtteranceLength"));
        int cooldownTurns = optionalInt(map.get("cooldownTurns"));
        int maxColorLines = optionalInt(map.get("maxColorLines"));
        return new SmartActorPolicy(allowed, maxUtteranceLength, cooldownTurns, maxColorLines);
    }

    private static SmartActorHistoryScope parseScope(Object raw, String field) {
        if (raw == null) {
            return SmartActorHistoryScope.ACTOR;
        }
        return SmartActorHistoryScope.parse(raw.toString(), field);
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

    private static String optionalString(Object raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.toString().trim();
        return value;
    }

    private static int optionalInt(Object raw) {
        if (raw == null) {
            return 0;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static List<String> stringList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            return List.of();
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
        return List.copyOf(result);
    }

    private static Set<String> stringSet(Object raw) {
        if (raw == null) {
            return Set.of();
        }
        if (!(raw instanceof List<?> list)) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
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

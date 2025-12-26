package com.demo.adventure.test;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class PlaybookSupport {

    private PlaybookSupport() {
    }

    public record Playbook(String name,
                           String gameResource,
                           String outcome,
                           boolean allowEarlyExit,
                           List<Step> steps,
                           Map<String, List<DecisionSpec>> smartActorDecisions,
                           Map<String, List<DecisionSpec>> smartActorExpectations) {
    }

    public record Step(String command, List<String> expectContains) {
    }

    public record DecisionSpec(String type, String utterance, String color, String rule) {
        public DecisionSpec {
            String normalized = normalizeType(type);
            if (normalized.isBlank()) {
                throw new IllegalArgumentException("Smart actor decision type is required");
            }
            type = normalized;
            utterance = normalizeField(utterance);
            color = normalizeField(color);
            rule = normalizeField(rule);
            if ("UTTERANCE".equals(type) && utterance.isBlank()) {
                throw new IllegalArgumentException("Smart actor decision utterance is required");
            }
            if ("COLOR".equals(type) && color.isBlank()) {
                throw new IllegalArgumentException("Smart actor decision color is required");
            }
            if (containsLineBreak(utterance) || containsLineBreak(color) || containsLineBreak(rule)) {
                throw new IllegalArgumentException("Smart actor decision fields must be single-line");
            }
        }

        public String toJson() {
            List<String> parts = new ArrayList<>();
            parts.add("\"type\":\"" + escapeJson(type) + "\"");
            if (!utterance.isBlank()) {
                parts.add("\"utterance\":\"" + escapeJson(utterance) + "\"");
            }
            if (!color.isBlank()) {
                parts.add("\"color\":\"" + escapeJson(color) + "\"");
            }
            if (!rule.isBlank()) {
                parts.add("\"rule\":\"" + escapeJson(rule) + "\"");
            }
            return "{" + String.join(",", parts) + "}";
        }

        public static DecisionSpec fallbackLook() {
            return new DecisionSpec("UTTERANCE", "look", "", "");
        }

        private static String normalizeType(String type) {
            return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
        }

        private static String normalizeField(String value) {
            return value == null ? "" : value.trim();
        }

        private static boolean containsLineBreak(String value) {
            return value != null && (value.contains("\n") || value.contains("\r"));
        }

        private static String escapeJson(String value) {
            return value
                    .replace("\\", "\\\\")
                    .replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r");
        }
    }

    public static Playbook loadPlaybook(String resource) {
        Path path = Path.of(resource);
        String source = resource;
        if (Files.exists(path)) {
            source = path.toString();
        }
        try (InputStream in = openResource(source)) {
            if (in == null) {
                throw new IllegalStateException("Missing playbook resource: " + source);
            }
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map<?, ?> root)) {
                throw new IllegalStateException("Playbook root must be a mapping: " + source);
            }
            String name = readString(root, "name", source);
            String game = readString(root, "game", source);
            String outcome = optionalString(root.get("outcome"));
            boolean allowEarlyExit = optionalBoolean(root.get("allowEarlyExit"));
            List<Step> steps = readSteps(root.get("steps"), source);
            Map<String, List<DecisionSpec>> decisions = readSmartActorDecisions(root.get("smartActorDecisions"), source, "smartActorDecisions");
            Map<String, List<DecisionSpec>> expectations = readSmartActorDecisions(root.get("smartActorExpectations"), source, "smartActorExpectations");
            if (steps.isEmpty()) {
                throw new IllegalStateException("Playbook steps are required: " + source);
            }
            String gameResource = resolveGameResource(source, game);
            return new Playbook(name, gameResource, outcome, allowEarlyExit, steps, decisions, expectations);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load playbook: " + source, ex);
        }
    }

    public static InputStream openResource(String resource) throws Exception {
        Path path = Path.of(resource);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }
        return PlaybookSupport.class.getClassLoader().getResourceAsStream(resource);
    }

    public static String resolveGameResource(String playbookResource, String game) {
        Path playbookPath = Path.of(playbookResource);
        if (Files.exists(playbookPath)) {
            Path parent = playbookPath.getParent();
            if (parent != null) {
                return parent.resolve(game).toString();
            }
        }
        return game;
    }

    private static String readString(Map<?, ?> root, String field, String resource) {
        Object value = root.get(field);
        if (value == null) {
            throw new IllegalStateException("Missing playbook field '" + field + "': " + resource);
        }
        String text = value.toString().trim();
        if (text.isEmpty()) {
            throw new IllegalStateException("Playbook field '" + field + "' is blank: " + resource);
        }
        return text;
    }

    private static String optionalString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    private static boolean optionalBoolean(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static List<Step> readSteps(Object value, String resource) {
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException("Playbook field 'steps' must be a list: " + resource);
        }
        List<Step> steps = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> stepMap)) {
                continue;
            }
            String command = readString(stepMap, "command", resource);
            List<String> expect = readStringList(stepMap.get("expect"), "steps.expect", resource);
            steps.add(new Step(command, expect));
        }
        return steps;
    }

    private static List<String> readStringList(Object value, String field, String resource) {
        if (value == null) {
            return List.of();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException("Playbook field '" + field + "' must be a list: " + resource);
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String text = entry.toString().trim();
            if (!text.isEmpty()) {
                result.add(text);
            }
        }
        return result;
    }

    private static Map<String, List<DecisionSpec>> readSmartActorDecisions(Object value, String resource, String fieldName) {
        if (value == null) {
            return new HashMap<>();
        }
        if (!(value instanceof List<?> list)) {
            throw new IllegalStateException("Playbook field '" + fieldName + "' must be a list: " + resource);
        }
        Map<String, List<DecisionSpec>> decisions = new HashMap<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> entryMap)) {
                continue;
            }
            String actorKey = readString(entryMap, "actorKey", resource);
            Object decisionList = entryMap.get("decisions");
            if (!(decisionList instanceof List<?> decisionEntries)) {
                throw new IllegalStateException("Playbook field '" + fieldName + ".decisions' must be a list: " + resource);
            }
            List<DecisionSpec> actorDecisions = decisions.computeIfAbsent(actorKey, key -> new ArrayList<>());
            for (Object decisionEntry : decisionEntries) {
                if (!(decisionEntry instanceof Map<?, ?> decisionMap)) {
                    continue;
                }
                actorDecisions.add(decisionFromMap(decisionMap, resource));
            }
        }
        return decisions;
    }

    private static DecisionSpec decisionFromMap(Map<?, ?> decisionMap, String resource) {
        String type = optionalString(decisionMap.get("type"));
        String utterance = optionalString(decisionMap.get("utterance"));
        String color = optionalString(decisionMap.get("color"));
        String rule = optionalString(decisionMap.get("rule"));
        try {
            return new DecisionSpec(type, utterance, color, rule);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex.getMessage() + ": " + resource, ex);
        }
    }
}

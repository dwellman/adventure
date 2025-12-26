package com.demo.adventure.engine.flow.trigger;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loader for trigger configuration YAML.
 *
 * Expected shape:
 * triggers:
 *   - id: time-stone-trap
 *     type: ON_TAKE
 *     target: Time Stone
 *     key: HAS("Time Stone")
 *     actions:
 *       - type: MESSAGE
 *         text: The stone hums.
 *       - type: RESET_LOOP
 *         reason: MANUAL
 */
public final class TriggerConfigLoader {
    private TriggerConfigLoader() {
    }

    public static List<TriggerDefinition> load(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return List.of();
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return load(text);
    }

    public static List<TriggerDefinition> load(InputStream in) throws IOException {
        if (in == null) {
            return List.of();
        }
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        return load(text);
    }

    public static List<TriggerDefinition> load(String yamlText) {
        if (yamlText == null || yamlText.isBlank()) {
            return List.of();
        }
        Object raw = new Yaml().load(yamlText);
        if (!(raw instanceof Map<?, ?> root)) {
            return List.of();
        }
        Object triggersObj = root.get("triggers");
        if (!(triggersObj instanceof List<?> list)) {
            return List.of();
        }
        List<TriggerDefinition> triggers = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            TriggerType type = parseTriggerType(asString(map.get("type")));
            if (type == null) {
                continue;
            }
            String id = asString(map.get("id"));
            String target = asString(map.get("target"));
            String object = asString(map.get("object"));
            String key = asString(map.get("key"));
            List<TriggerAction> actions = parseActions(map.get("actions"));
            triggers.add(new TriggerDefinition(id, type, target, object, key, actions));
        }
        return List.copyOf(triggers);
    }

    private static List<TriggerAction> parseActions(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<TriggerAction> actions = new ArrayList<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            TriggerActionType type = parseActionType(asString(map.get("type")));
            if (type == null) {
                continue;
            }
            String target = asString(map.get("target"));
            String owner = asString(map.get("owner"));
            String text = asString(map.get("text"));
            String key = asString(map.get("key"));
            String visibilityKey = asString(map.get("visibilityKey"));
            String cell = asString(map.get("cell"));
            Long amount = parseLong(map.get("amount"));
            String description = asString(map.get("description"));
            String reason = asString(map.get("reason"));
            Boolean visible = parseBoolean(map.get("visible"));
            actions.add(new TriggerAction(
                    type,
                    target,
                    owner,
                    text,
                    key,
                    visibilityKey,
                    cell,
                    amount,
                    description,
                    reason,
                    visible
            ));
        }
        return List.copyOf(actions);
    }

    private static TriggerType parseTriggerType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TriggerType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static TriggerActionType parseActionType(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TriggerActionType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String asString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private static Long parseLong(Object obj) {
        if (obj instanceof Number n) {
            return n.longValue();
        }
        if (obj != null) {
            String text = obj.toString().trim();
            if (!text.isBlank()) {
                try {
                    return Long.parseLong(text);
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private static Boolean parseBoolean(Object obj) {
        if (obj instanceof Boolean b) {
            return b;
        }
        if (obj != null) {
            String text = obj.toString().trim();
            if (!text.isBlank()) {
                return Boolean.parseBoolean(text);
            }
        }
        return null;
    }
}

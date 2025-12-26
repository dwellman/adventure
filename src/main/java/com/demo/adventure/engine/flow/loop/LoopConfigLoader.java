package com.demo.adventure.engine.flow.loop;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loader for loop configuration YAML.
 */
public final class LoopConfigLoader {
    private LoopConfigLoader() {
    }

    public static LoopConfig load(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return LoopConfig.disabled();
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return load(text);
    }

    public static LoopConfig load(InputStream in) throws IOException {
        if (in == null) {
            return LoopConfig.disabled();
        }
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        return load(text);
    }

    public static LoopConfig load(String yamlText) {
        if (yamlText == null || yamlText.isBlank()) {
            return LoopConfig.disabled();
        }
        Object raw = new Yaml().load(yamlText);
        if (!(raw instanceof Map<?, ?> root)) {
            return LoopConfig.disabled();
        }
        boolean enabled = bool(root.get("enabled"), true);
        int maxTicks = parseInt(root.get("maxTicks"), 0);
        List<String> persistentItems = stringList(root.get("persistentItems"));
        if (!enabled || maxTicks <= 0) {
            return new LoopConfig(false, 0, persistentItems);
        }
        return new LoopConfig(true, maxTicks, persistentItems);
    }

    private static boolean bool(Object raw, boolean fallback) {
        if (raw instanceof Boolean b) {
            return b;
        }
        if (raw != null) {
            String text = raw.toString().trim();
            if (!text.isBlank()) {
                return Boolean.parseBoolean(text);
            }
        }
        return fallback;
    }

    private static int parseInt(Object raw, int fallback) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw != null) {
            String text = raw.toString().trim();
            if (!text.isBlank()) {
                try {
                    return Integer.parseInt(text);
                } catch (NumberFormatException ignored) {
                    return fallback;
                }
            }
        }
        return fallback;
    }

    private static List<String> stringList(Object raw) {
        if (!(raw instanceof List<?> list)) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                continue;
            }
            String text = entry.toString().trim();
            if (!text.isBlank()) {
                out.add(text);
            }
        }
        return List.copyOf(out);
    }
}

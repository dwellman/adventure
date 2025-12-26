package com.demo.adventure.authoring.zone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic description templates keyed by region and anchor role to avoid empty descriptions.
 */
final class DescriptionTemplates {
    private static final String CONFIG_FILE = "templates.yaml";

    private DescriptionTemplates() {}

    static String pickFor(String region, AnchorRole role, long seed, int offset) {
        TemplatesConfig config = TemplatesConfigHolder.CONFIG;
        String reg = region == null ? "" : region.toUpperCase(Locale.ROOT);
        String hook = pick(config.regionHooks().getOrDefault(reg, List.of()), seed + offset);
        List<String> roleOptions = role == null ? List.of() : config.roleHooks().getOrDefault(role, List.of());
        String roleHook = pick(roleOptions, seed + offset + 7);
        if (hook.isBlank() && roleHook.isBlank()) {
            return config.fallback();
        }
        if (hook.isBlank()) {
            return roleHook;
        }
        if (roleHook.isBlank()) {
            return hook;
        }
        return hook + " " + roleHook;
    }

    private static String pick(List<String> options, long seed) {
        if (options == null || options.isEmpty()) {
            return "";
        }
        // Use a derived seed for determinism without mutating ThreadLocalRandom's seed.
        int optionIndex = (int) Math.floorMod(seed, options.size());
        return options.get(optionIndex);
    }

    private static TemplatesConfig loadConfig() {
        Map<String, Object> root = StorybookSharedLoader.load(CONFIG_FILE);
        Map<String, Object> templates = requireMap(root.get("descriptionTemplates"), "descriptionTemplates");
        String fallback = requireString(templates.get("fallback"), "descriptionTemplates.fallback");
        Map<String, List<String>> regions = readStringListMap(templates.get("regions"), "descriptionTemplates.regions");
        Map<AnchorRole, List<String>> roles = readRoleListMap(templates.get("roles"), "descriptionTemplates.roles");
        return new TemplatesConfig(regions, roles, fallback);
    }

    private static Map<String, List<String>> readStringListMap(Object raw, String field) {
        Map<String, Object> map = optionalMap(raw, field);
        Map<String, List<String>> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = normalizeKey(entry.getKey(), field);
            out.put(key, stringList(entry.getValue(), field + "." + key));
        }
        return Map.copyOf(out);
    }

    private static Map<AnchorRole, List<String>> readRoleListMap(Object raw, String field) {
        Map<String, Object> map = optionalMap(raw, field);
        Map<AnchorRole, List<String>> out = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = normalizeKey(entry.getKey(), field);
            AnchorRole role;
            try {
                role = AnchorRole.valueOf(key);
            } catch (IllegalArgumentException ex) {
                throw new IllegalStateException("Unknown anchor role in " + field + ": " + key);
            }
            out.put(role, stringList(entry.getValue(), field + "." + key));
        }
        return Map.copyOf(out);
    }

    private static Map<String, Object> requireMap(Object raw, String field) {
        if (raw instanceof Map<?, ?> map) {
            return castMap(map);
        }
        throw new IllegalStateException("Expected map for " + field);
    }

    private static Map<String, Object> optionalMap(Object raw, String field) {
        if (raw == null) {
            return Map.of();
        }
        if (raw instanceof Map<?, ?> map) {
            return castMap(map);
        }
        throw new IllegalStateException("Expected map for " + field);
    }

    private static Map<String, Object> castMap(Map<?, ?> raw) {
        Map<String, Object> map = new HashMap<>();
        for (Map.Entry<?, ?> entry : raw.entrySet()) {
            if (entry.getKey() != null) {
                map.put(entry.getKey().toString(), entry.getValue());
            }
        }
        return map;
    }

    private static String requireString(Object raw, String field) {
        if (raw == null) {
            throw new IllegalStateException("Expected string for " + field);
        }
        String value = Objects.toString(raw, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Expected string for " + field);
        }
        return value;
    }

    private static List<String> stringList(Object raw, String field) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new IllegalStateException("Expected list for " + field);
        }
        List<String> out = new ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                throw new IllegalStateException("Null entry in " + field);
            }
            out.add(entry.toString());
        }
        return List.copyOf(out);
    }

    private static String normalizeKey(Object raw, String field) {
        String value = Objects.toString(raw, "").trim();
        if (value.isEmpty()) {
            throw new IllegalStateException("Expected key for " + field);
        }
        return value.toUpperCase(Locale.ROOT);
    }

    private record TemplatesConfig(
            Map<String, List<String>> regionHooks,
            Map<AnchorRole, List<String>> roleHooks,
            String fallback
    ) {
    }

    private static final class TemplatesConfigHolder {
        private static final TemplatesConfig CONFIG = loadConfig();
    }
}

package com.demo.adventure.authoring.save.io;

import com.demo.adventure.engine.mechanics.cells.CellSpec;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class GameSaveYamlValues {
    private GameSaveYamlValues() {
    }

    static List<?> list(Object value, String field) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Field '" + field + "' must be a list");
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> map(Object value, String field) {
        if (value instanceof Map<?, ?> raw) {
            return (Map<String, Object>) raw;
        }
        throw new IllegalArgumentException("Field '" + field + "' must be a map");
    }

    static String str(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        return Objects.toString(value);
    }

    @SuppressWarnings("unchecked")
    static String description(Object value, String field) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return Objects.toString(value);
        }
        if (value instanceof List<?> list) {
            String last = "";
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> raw) {
                    Object text = ((Map<String, Object>) raw).get("text");
                    if (text != null) {
                        last = text.toString();
                    }
                } else if (entry != null) {
                    last = entry.toString();
                }
            }
            return last;
        }
        throw new IllegalArgumentException("Field '" + field + "' must be a string or list of descriptions");
    }

    static Map<String, CellSpec> parseCells(Object raw, String field) {
        if (raw == null) {
            return Map.of();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, CellSpec> cells = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String name = entry.getKey().toString();
            if (!(entry.getValue() instanceof Map<?, ?> cellMap)) {
                continue;
            }
            long capacity = parseLong(cellMap.get("capacity"), field + "." + name + ".capacity");
            long amount = optionalLong(cellMap.get("amount"), 0L);
            cells.put(normalizeCellKey(name), new CellSpec(capacity, amount));
        }
        return Map.copyOf(cells);
    }

    static List<String> stringList(Object value, String field) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    result.add(entry.toString());
                }
            }
            return result;
        }
        if (value instanceof String str) {
            return List.of(str);
        }
        throw new IllegalArgumentException("Field '" + field + "' must be a string or list of strings");
    }

    static long parseLong(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Field '" + field + "' is not a number: " + value, ex);
        }
    }

    static long optionalLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    static boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value.toString());
    }

    static double optionalDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    static Integer optionalInt(Object value, String field) {
        if (value == null) {
            return null;
        }
        return (int) parseLong(value, field);
    }

    static String optionalString(Object value, String field) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof List<?> list) {
            return description(list, field);
        }
        return Objects.toString(value);
    }

    static String optionalKey(Object value, Object legacyValue, String fallbackPlotKey) {
        if (value == null) {
            if (legacyValue != null) {
                return key(legacyValue, null, null, "ownerKey");
            }
            return fallbackPlotKey;
        }
        return key(value, null, null, "ownerKey");
    }

    static String optionalKeyOrNull(Object value, String field) {
        if (value == null) {
            return null;
        }
        return key(value, null, null, field);
    }

    static String normalizeCellKey(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }

    static UUID uuid(String kind, String key) {
        return UUID.nameUUIDFromBytes((kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    static String key(Object primary, Object legacy, Object legacy2, String field) {
        Object value = primary != null ? primary : (legacy != null ? legacy : legacy2);
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        String raw = Objects.toString(value).trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        boolean dash = false;
        for (char c : raw.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                dash = false;
            } else {
                if (!dash) {
                    sb.append('-');
                    dash = true;
                }
            }
        }
        String normalized = sb.toString();
        while (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("-")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        return normalized;
    }

    static String keyStringOrDefault(Object value, String field) {
        if (value == null) {
            return "true";
        }
        String text = Objects.toString(value).trim();
        if (text.isEmpty()) {
            return "true";
        }
        return text;
    }
}

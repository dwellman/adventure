package com.demo.adventure.engine.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Alias registry for verb normalization (kept in sync with CommandScanner keywords).
 */
public final class VerbAliases {
    private static final Map<String, String> ALIAS_TO_CANONICAL = buildAliasMap();
    private static final Map<String, List<String>> CANONICAL_TO_ALIASES = buildReverseMap();

    private VerbAliases() {
    }

    public static Map<String, String> aliasMap() {
        return ALIAS_TO_CANONICAL;
    }

    public static String canonicalize(String verb) {
        if (verb == null) {
            return "";
        }
        String trimmed = verb.trim();
        if (trimmed.isEmpty()) {
            return "";
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return ALIAS_TO_CANONICAL.getOrDefault(upper, upper);
    }

    public static List<String> aliasesFor(String canonical) {
        if (canonical == null) {
            return List.of();
        }
        String trimmed = canonical.trim();
        if (trimmed.isEmpty()) {
            return List.of();
        }
        String upper = trimmed.toUpperCase(Locale.ROOT);
        return CANONICAL_TO_ALIASES.getOrDefault(upper, List.of());
    }

    private static Map<String, String> buildAliasMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("GO", "MOVE");
        map.put("CLIMB", "MOVE");
        map.put("CRAFT", "MAKE");
        map.put("GRAB", "TAKE");
        map.put("EXPLORE", "SEARCH");
        map.put("STRIKE", "ATTACK");
        map.put("L", "LOOK");
        map.put("I", "INVENTORY");
        map.put("H", "HELP");
        map.put("?", "HELP");
        map.put("Q", "QUIT");
        map.put("EXIT", "QUIT");
        map.put("RUN", "MOVE");
        return Collections.unmodifiableMap(map);
    }

    private static Map<String, List<String>> buildReverseMap() {
        Map<String, List<String>> reverse = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : ALIAS_TO_CANONICAL.entrySet()) {
            reverse.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }
        reverse.replaceAll((k, v) -> List.copyOf(v));
        return Collections.unmodifiableMap(reverse);
    }
}

package com.demo.adventure.ai.runtime.smart;

import java.util.Locale;

public enum SmartActorHistoryScope {
    ACTOR,
    PLOT,
    GLOBAL;

    public static SmartActorHistoryScope parse(String raw, String field) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(field + " scope is required");
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        for (SmartActorHistoryScope scope : values()) {
            if (scope.name().equals(normalized)) {
                return scope;
            }
        }
        throw new IllegalArgumentException(field + " has unknown scope: " + raw);
    }
}

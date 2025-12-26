package com.demo.adventure.ai.runtime.smart;

import java.util.Locale;

public record SmartActorDecision(
        Type type,
        String utterance,
        String color,
        String rule
) {
    public SmartActorDecision {
        if (type == null) {
            throw new IllegalArgumentException("decision type is required");
        }
        utterance = utterance == null ? "" : utterance.trim();
        color = color == null ? "" : color.trim();
        rule = rule == null ? "" : rule.trim();
    }

    public enum Type {
        UTTERANCE,
        COLOR,
        NONE;

        public static Type parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            String normalized = raw.trim().toUpperCase(Locale.ROOT);
            for (Type type : values()) {
                if (type.name().equals(normalized)) {
                    return type;
                }
            }
            return null;
        }
    }
}

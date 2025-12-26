package com.demo.adventure.ai.runtime.smart;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public record SmartActorPolicy(
        List<String> allowedVerbs,
        int maxUtteranceLength,
        int cooldownTurns,
        int maxColorLines
) {
    public SmartActorPolicy {
        allowedVerbs = normalizeVerbs(allowedVerbs);
    }

    public static SmartActorPolicy empty() {
        return new SmartActorPolicy(List.of(), 0, 0, 0);
    }

    private static List<String> normalizeVerbs(List<String> verbs) {
        if (verbs == null || verbs.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String verb : verbs) {
            if (verb == null || verb.isBlank()) {
                continue;
            }
            normalized.add(verb.trim().toUpperCase(Locale.ROOT));
        }
        return List.copyOf(normalized);
    }
}

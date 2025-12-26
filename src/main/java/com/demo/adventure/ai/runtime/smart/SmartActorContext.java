package com.demo.adventure.ai.runtime.smart;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public record SmartActorContext(
        SmartActorSpec spec,
        UUID actorId,
        UUID plotId,
        Set<String> contextTags,
        List<SmartActorHistoryEntry> historySnippets
) {
    public SmartActorContext {
        if (spec == null) {
            throw new IllegalArgumentException("spec is required");
        }
        contextTags = contextTags == null ? Set.of() : Set.copyOf(contextTags);
        historySnippets = historySnippets == null ? List.of() : List.copyOf(historySnippets);
    }
}

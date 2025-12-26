package com.demo.adventure.ai.runtime.smart;

import java.util.Set;

public record SmartActorHistoryEntry(
        String id,
        String text,
        Set<String> tags,
        SmartActorHistoryScope scope,
        long timestamp,
        String source,
        boolean pinned
) {
    public SmartActorHistoryEntry {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("history entry id is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("history entry text is required");
        }
        if (scope == null) {
            throw new IllegalArgumentException("history entry scope is required");
        }
        tags = SmartActorTags.normalize(tags);
        source = source == null ? "" : source.trim();
    }
}

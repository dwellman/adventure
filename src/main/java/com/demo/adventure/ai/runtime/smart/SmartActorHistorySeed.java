package com.demo.adventure.ai.runtime.smart;

import java.util.Set;

public record SmartActorHistorySeed(
        String id,
        String text,
        SmartActorHistoryScope scope,
        Set<String> tags
) {
    public SmartActorHistorySeed {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("history seed id is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("history seed text is required");
        }
        if (scope == null) {
            throw new IllegalArgumentException("history seed scope is required");
        }
        tags = SmartActorTags.normalize(tags);
    }
}

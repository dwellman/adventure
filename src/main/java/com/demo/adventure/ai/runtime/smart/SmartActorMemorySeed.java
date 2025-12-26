package com.demo.adventure.ai.runtime.smart;

import java.util.Set;

public record SmartActorMemorySeed(
        String id,
        String text,
        SmartActorHistoryScope scope,
        Set<String> tags
) {
    public SmartActorMemorySeed {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("memory seed id is required");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("memory seed text is required");
        }
        if (scope == null) {
            throw new IllegalArgumentException("memory seed scope is required");
        }
        tags = SmartActorTags.normalize(tags);
    }
}

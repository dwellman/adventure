package com.demo.adventure.ai.runtime.smart;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

final class SmartActorTags {
    private SmartActorTags() {
    }

    static Set<String> normalize(Collection<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String tag : tags) {
            String normalizedTag = normalizeTag(tag);
            if (!normalizedTag.isBlank()) {
                normalized.add(normalizedTag);
            }
        }
        return Set.copyOf(normalized);
    }

    static String normalizeTag(String tag) {
        if (tag == null) {
            return "";
        }
        return tag.trim().toUpperCase(Locale.ROOT);
    }
}

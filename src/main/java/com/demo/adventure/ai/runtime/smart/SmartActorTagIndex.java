package com.demo.adventure.ai.runtime.smart;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SmartActorTagIndex {
    private final Map<UUID, Set<String>> plotTags;
    private final Map<UUID, Set<String>> itemTags;
    private final Map<UUID, Set<String>> fixtureTags;
    private final Map<UUID, Set<String>> actorTags;
    private final Map<String, Set<String>> questTags;

    public SmartActorTagIndex(Map<UUID, Set<String>> plotTags,
                              Map<UUID, Set<String>> itemTags,
                              Map<UUID, Set<String>> fixtureTags,
                              Map<UUID, Set<String>> actorTags,
                              Map<String, Set<String>> questTags) {
        this.plotTags = normalizeUuidMap(plotTags);
        this.itemTags = normalizeUuidMap(itemTags);
        this.fixtureTags = normalizeUuidMap(fixtureTags);
        this.actorTags = normalizeUuidMap(actorTags);
        this.questTags = normalizeQuestMap(questTags);
    }

    public static SmartActorTagIndex empty() {
        return new SmartActorTagIndex(Map.of(), Map.of(), Map.of(), Map.of(), Map.of());
    }

    public Set<String> tagsForPlot(UUID plotId) {
        return tagsFor(plotTags, plotId);
    }

    public Set<String> tagsForItem(UUID itemId) {
        return tagsFor(itemTags, itemId);
    }

    public Set<String> tagsForFixture(UUID fixtureId) {
        return tagsFor(fixtureTags, fixtureId);
    }

    public Set<String> tagsForActor(UUID actorId) {
        return tagsFor(actorTags, actorId);
    }

    public Set<String> tagsForQuestKey(String questKey) {
        if (questKey == null || questKey.isBlank()) {
            return Set.of();
        }
        return questTags.getOrDefault(normalizeKey(questKey), Set.of());
    }

    private static Set<String> tagsFor(Map<UUID, Set<String>> source, UUID id) {
        if (id == null || source == null) {
            return Set.of();
        }
        return source.getOrDefault(id, Set.of());
    }

    private static Map<UUID, Set<String>> normalizeUuidMap(Map<UUID, Set<String>> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Set<String>> result = new LinkedHashMap<>();
        for (Map.Entry<UUID, Set<String>> entry : input.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            Set<String> tags = SmartActorTags.normalize(entry.getValue());
            if (!tags.isEmpty()) {
                result.put(entry.getKey(), tags);
            }
        }
        return Map.copyOf(result);
    }

    private static Map<String, Set<String>> normalizeQuestMap(Map<String, Set<String>> input) {
        if (input == null || input.isEmpty()) {
            return Map.of();
        }
        Map<String, Set<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : input.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            String key = normalizeKey(entry.getKey());
            Set<String> tags = SmartActorTags.normalize(entry.getValue());
            if (!key.isBlank() && !tags.isEmpty()) {
                result.put(key, tags);
            }
        }
        return Map.copyOf(result);
    }

    private static String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase(Locale.ROOT);
    }
}

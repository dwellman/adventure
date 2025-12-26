package com.demo.adventure.ai.runtime.smart;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SmartActorContextInputBuilder {
    private final SmartActorTagIndex tagIndex;

    public SmartActorContextInputBuilder(SmartActorTagIndex tagIndex) {
        if (tagIndex == null) {
            throw new IllegalArgumentException("tagIndex is required");
        }
        this.tagIndex = tagIndex;
    }

    public SmartActorContextInput build(KernelRegistry registry,
                                        UUID actorId,
                                        UUID plotId,
                                        Set<String> questTags) {
        Set<String> plotTags = tagIndex.tagsForPlot(plotId);
        Set<String> itemTags = collectItemTags(registry, plotId, actorId);
        return new SmartActorContextInput(actorId, plotId, plotTags, itemTags, questTags);
    }

    private Set<String> collectItemTags(KernelRegistry registry, UUID plotId, UUID actorId) {
        if (registry == null) {
            return Set.of();
        }
        Set<String> tags = new LinkedHashSet<>();
        for (Object value : registry.getEverything().values()) {
            if (!(value instanceof Item item)) {
                continue;
            }
            if (!item.isVisible()) {
                continue;
            }
            UUID ownerId = item.getOwnerId();
            if (ownerId == null || (!ownerId.equals(plotId) && !ownerId.equals(actorId))) {
                continue;
            }
            Set<String> itemTags = item.isFixture()
                    ? tagIndex.tagsForFixture(item.getId())
                    : tagIndex.tagsForItem(item.getId());
            tags.addAll(itemTags);
        }
        return Set.copyOf(tags);
    }
}

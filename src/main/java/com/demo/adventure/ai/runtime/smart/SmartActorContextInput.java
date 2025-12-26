package com.demo.adventure.ai.runtime.smart;

import java.util.Set;
import java.util.UUID;

public record SmartActorContextInput(
        UUID actorId,
        UUID plotId,
        Set<String> plotTags,
        Set<String> itemTags,
        Set<String> questTags
) {
    public SmartActorContextInput {
        plotTags = SmartActorTags.normalize(plotTags);
        itemTags = SmartActorTags.normalize(itemTags);
        questTags = SmartActorTags.normalize(questTags);
    }
}

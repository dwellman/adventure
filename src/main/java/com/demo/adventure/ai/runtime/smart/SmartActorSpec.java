package com.demo.adventure.ai.runtime.smart;

import java.util.List;
import java.util.Map;

public record SmartActorSpec(
        String actorKey,
        String promptId,
        String backstory,
        Map<String, Object> persona,
        Map<String, Object> properties,
        List<SmartActorMemorySeed> memorySeeds,
        SmartActorHistorySpec history,
        SmartActorPolicy policy
) {
    public SmartActorSpec {
        if (actorKey == null || actorKey.isBlank()) {
            throw new IllegalArgumentException("actorKey is required");
        }
        if (promptId == null || promptId.isBlank()) {
            throw new IllegalArgumentException("promptId is required");
        }
        backstory = backstory == null ? "" : backstory.trim();
        persona = persona == null ? Map.of() : Map.copyOf(persona);
        properties = properties == null ? Map.of() : Map.copyOf(properties);
        memorySeeds = memorySeeds == null ? List.of() : List.copyOf(memorySeeds);
        policy = policy == null ? SmartActorPolicy.empty() : policy;
    }
}

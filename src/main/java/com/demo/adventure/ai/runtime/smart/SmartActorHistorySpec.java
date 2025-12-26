package com.demo.adventure.ai.runtime.smart;

import java.util.List;

public record SmartActorHistorySpec(
        String storeKey,
        List<SmartActorHistorySeed> seeds
) {
    public SmartActorHistorySpec {
        if (storeKey == null || storeKey.isBlank()) {
            throw new IllegalArgumentException("history storeKey is required");
        }
        seeds = seeds == null ? List.of() : List.copyOf(seeds);
    }
}

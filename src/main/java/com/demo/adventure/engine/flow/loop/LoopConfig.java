package com.demo.adventure.engine.flow.loop;

import java.util.List;

/**
 * Configuration for loop-enabled worlds (time resets, persistent items).
 */
public record LoopConfig(boolean enabled, int maxTicks, List<String> persistentItems) {
    public LoopConfig {
        if (maxTicks < 0) {
            throw new IllegalArgumentException("maxTicks must be >= 0");
        }
        persistentItems = persistentItems == null ? List.of() : List.copyOf(persistentItems);
    }

    public static LoopConfig disabled() {
        return new LoopConfig(false, 0, List.of());
    }
}

package com.demo.adventure.engine.integrity;

public record GameIntegrityConfig(
        int maxDepth,
        int maxStates,
        int maxActionsPerState
) {
    public static GameIntegrityConfig defaults() {
        return new GameIntegrityConfig(80, 5000, 200);
    }
}

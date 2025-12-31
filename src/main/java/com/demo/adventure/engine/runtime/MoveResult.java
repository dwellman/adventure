package com.demo.adventure.engine.runtime;

import java.util.UUID;

public record MoveResult(UUID nextPlotId, String blockedReason) {
    public static MoveResult moved(UUID nextPlotId) {
        return new MoveResult(nextPlotId, "");
    }

    public static MoveResult blocked(String reason) {
        return new MoveResult(null, reason == null ? "" : reason);
    }

    public static MoveResult none() {
        return new MoveResult(null, "");
    }

    public boolean moved() {
        return nextPlotId != null;
    }

    public boolean blocked() {
        return blockedReason != null && !blockedReason.isBlank();
    }
}

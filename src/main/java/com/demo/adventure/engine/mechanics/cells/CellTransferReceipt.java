package com.demo.adventure.engine.mechanics.cells;

import java.util.UUID;

public record CellTransferReceipt(
        UUID fromThingId,
        UUID toThingId,
        String cellName,
        long requestedDelta,
        long appliedDelta
) {
}

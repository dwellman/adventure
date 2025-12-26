package com.demo.adventure.engine.mechanics.cells;

import java.util.UUID;

public record CellMutationReceipt(
        UUID thingId,
        String cellName,
        String field,
        long beforeAmount,
        long afterAmount,
        double beforeVolume,
        double afterVolume,
        Long requestedDelta,
        Long appliedDelta,
        CellMutationReason reason
) {
}

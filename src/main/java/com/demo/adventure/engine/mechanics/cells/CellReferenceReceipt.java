package com.demo.adventure.engine.mechanics.cells;

import java.util.UUID;

public record CellReferenceReceipt(
        UUID thingId,
        String cellName,
        String field,
        CellReferenceStatus status
) {
}

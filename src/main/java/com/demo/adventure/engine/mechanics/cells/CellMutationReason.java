package com.demo.adventure.engine.mechanics.cells;

public enum CellMutationReason {
    APPLIED,
    NO_OP,
    CLAMPED_TO_ZERO,
    CLAMPED_TO_CAPACITY,
    MISSING_CELL
}

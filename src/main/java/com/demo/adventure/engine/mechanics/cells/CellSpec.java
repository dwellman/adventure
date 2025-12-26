package com.demo.adventure.engine.mechanics.cells;

public record CellSpec(long capacity, long amount) {
    public CellSpec {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Cell capacity must be > 0");
        }
        amount = clamp(amount, 0, capacity);
    }

    public Cell toCell() {
        return new Cell(capacity, amount);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}

package com.demo.adventure.engine.mechanics.cells;

public final class Cell {
    private final long capacity;
    private long amount;

    public Cell(long capacity, long amount) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Cell capacity must be > 0");
        }
        this.capacity = capacity;
        this.amount = clamp(amount, 0, capacity);
    }

    public long getCapacity() {
        return capacity;
    }

    public long getAmount() {
        return amount;
    }

    long setAmount(long value) {
        long before = amount;
        amount = clamp(value, 0, capacity);
        return before;
    }

    public double getVolume() {
        return volumeFor(amount, capacity);
    }

    public static double volumeFor(long amount, long capacity) {
        if (capacity <= 0) {
            return 0.0;
        }
        double raw = (double) amount / (double) capacity;
        return clamp(raw, 0.0, 1.0);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}

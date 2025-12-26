package com.demo.adventure.engine.mechanics.cells;

public record CellReadResult(
        String cellName,
        boolean missing,
        long capacity,
        long amount,
        double volume,
        String name
) {
    public static CellReadResult missing(String cellName) {
        return new CellReadResult(cellName, true, 0L, 0L, 0.0, cellName == null ? "" : cellName);
    }

    public static CellReadResult of(String cellName, Cell cell) {
        if (cell == null) {
            return missing(cellName);
        }
        return new CellReadResult(cellName, false, cell.getCapacity(), cell.getAmount(), cell.getVolume(), cellName);
    }
}

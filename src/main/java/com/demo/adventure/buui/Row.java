package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Row {
    private final List<Cell> cells;

    public Row(List<Cell> cells) {
        this.cells = Collections.unmodifiableList(new ArrayList<>(cells));
    }

    public static Row of(Object... values) {
        var cells = new ArrayList<Cell>();
        for (var value : values) {
            cells.add(new Cell(value));
        }
        return new Row(cells);
    }

    public List<Cell> cells() {
        return cells;
    }
}

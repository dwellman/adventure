package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TableBuilder {
    private final List<Column> columns = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private String title;
    private RenderStyle style = RenderStyle.defaults();
    private FormatterRegistry formatterRegistry = FormatterRegistry.defaults();

    public TableBuilder title(String title) {
        this.title = title;
        return this;
    }

    public TableBuilder style(RenderStyle style) {
        this.style = Objects.requireNonNull(style);
        return this;
    }

    public TableBuilder formatterRegistry(FormatterRegistry formatterRegistry) {
        this.formatterRegistry = Objects.requireNonNull(formatterRegistry);
        return this;
    }

    public TableBuilder showRowNumbers(boolean show) {
        this.style = this.style.withShowRowNumbers(show);
        return this;
    }

    public TableBuilder showHeader(boolean show) {
        this.style = this.style.withShowHeader(show);
        return this;
    }

    public TableBuilder showRowSeparators(boolean show) {
        this.style = this.style.withShowRowSeparators(show);
        return this;
    }

    public TableBuilder wrapCells(boolean wrap) {
        this.style = this.style.withWrapCells(wrap);
        return this;
    }

    public TableBuilder cellPadding(int padding) {
        this.style = this.style.withCellPadding(padding);
        return this;
    }

    public TableBuilder border(BorderCharacters border) {
        this.style = this.style.withBorder(border);
        return this;
    }

    public TableBuilder addColumn(Column column) {
        this.columns.add(column);
        return this;
    }

    public TableBuilder addColumn(String name) {
        return addColumn(Column.builder(name).build());
    }

    public TableBuilder addRow(Row row) {
        this.rows.add(row);
        return this;
    }

    public TableBuilder addRow(Object... values) {
        return addRow(Row.of(values));
    }

    public Table build() {
        if (columns.isEmpty()) {
            throw new IllegalStateException("Add at least one column");
        }

        var normalizedRows = new ArrayList<Row>();
        for (Row row : rows) {
            var cells = new ArrayList<Cell>();
            for (int i = 0; i < columns.size(); i++) {
                if (i < row.cells().size()) {
                    cells.add(row.cells().get(i));
                } else {
                    cells.add(new Cell(""));
                }
            }
            normalizedRows.add(new Row(cells));
        }

        return new Table(columns, normalizedRows, title, style, formatterRegistry);
    }
}

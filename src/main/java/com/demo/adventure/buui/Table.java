package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Table {
    private final List<Column> columns;
    private final List<Row> rows;
    private final String title;
    private final RenderStyle style;
    private final FormatterRegistry formatterRegistry;

    Table(List<Column> columns,
          List<Row> rows,
          String title,
          RenderStyle style,
          FormatterRegistry formatterRegistry) {
        this.columns = Collections.unmodifiableList(new ArrayList<>(columns));
        this.rows = Collections.unmodifiableList(new ArrayList<>(rows));
        this.title = title;
        this.style = style;
        this.formatterRegistry = formatterRegistry;
    }

    public List<Column> columns() {
        return columns;
    }

    public List<Row> rows() {
        return rows;
    }

    public String title() {
        return title;
    }

    public RenderStyle style() {
        return style;
    }

    public FormatterRegistry formatterRegistry() {
        return formatterRegistry;
    }

    public static TableBuilder builder() {
        return new TableBuilder();
    }

    public static Table fromLists(List<String> headers, List<List<?>> rows) {
        var builder = Table.builder();
        for (String header : headers) {
            builder.addColumn(Column.builder(header).build());
        }
        for (List<?> row : rows) {
            builder.addRow(row.toArray());
        }
        return builder.build();
    }
}

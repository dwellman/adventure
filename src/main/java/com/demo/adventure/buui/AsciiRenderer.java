package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class AsciiRenderer implements Renderer {

    @Override
    public String render(Table table) {
        RenderStyle style = table.style();
        BorderCharacters border = style.border();
        int padding = style.cellPadding();
        int rowNumberWidth = style.showRowNumbers() ? String.valueOf(Math.max(1, table.rows().size())).length() : 0;
        int[] columnWidths = computeColumnWidths(table, style);

        StringBuilder builder = new StringBuilder();

        // When a title is present, render a continuous top line (no column separators) for cleaner title framing.
        if (style.showTitle() && table.title() != null && !table.title().isBlank()) {
            builder.append(fullWidthLine(border.topLeft, border.horizontal, border.topRight, rowNumberWidth, padding, columnWidths)).append("\n");
        } else {
            builder.append(horizontalLine(border.topLeft, border.headerSeparator, border.topRight, border.horizontal, rowNumberWidth, padding, columnWidths)).append("\n");
        }

        if (style.showTitle() && table.title() != null && !table.title().isBlank()) {
            builder.append(titleLine(table.title(), style, columnWidths, rowNumberWidth)).append("\n");
            builder.append(horizontalLine(border.leftSeparator, border.headerSeparator, border.rightSeparator, border.horizontal, rowNumberWidth, padding, columnWidths)).append("\n");
        }

        if (style.showHeader()) {
            builder.append(headerLine(table, padding, rowNumberWidth, columnWidths)).append("\n");
            builder.append(horizontalLine(border.leftSeparator, border.intersection, border.rightSeparator, border.horizontal, rowNumberWidth, padding, columnWidths)).append("\n");
        }

        for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            Row row = table.rows().get(rowIndex);
            builder.append(dataRow(rowIndex, row, table, padding, rowNumberWidth, columnWidths));
            builder.append("\n");
            if (rowIndex < table.rows().size() - 1 && style.showRowSeparators()) {
                builder.append(horizontalLine(border.leftSeparator, border.intersection, border.rightSeparator, border.horizontal, rowNumberWidth, padding, columnWidths));
                builder.append("\n");
            }
        }

        if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '\n') {
            builder.append("\n");
        }
        builder.append(horizontalLine(border.bottomLeft, border.bottomCenter, border.bottomRight, border.horizontal, rowNumberWidth, padding, columnWidths));

        // Ensure a trailing newline so subsequent console output is positioned on the next line.
        if (builder.length() == 0 || builder.charAt(builder.length() - 1) != '\n') {
            builder.append("\n");
        }

        return builder.toString();
    }

    private int[] computeColumnWidths(Table table, RenderStyle style) {
        int[] widths = new int[table.columns().size()];
        for (int i = 0; i < table.columns().size(); i++) {
            Column column = table.columns().get(i);
            if (column.isFixedWidth()) {
                widths[i] = column.maxWidth();
                continue;
            }
            int width = Math.max(column.title().length(), column.minWidth());
            boolean wrapColumn = style.wrapCells() && column.wrap();
            for (Row row : table.rows()) {
                Cell cell = row.cells().get(i);
                String formatted = cell.render(table.formatterRegistry(), column.formatter());
                List<String> lines;
                if (wrapColumn) {
                    lines = TextUtils.wrap(formatted, column.maxWidth());
                } else {
                    lines = List.of(TextUtils.truncate(formatted, column.maxWidth()));
                }
                for (String line : lines) {
                    width = Math.max(width, Math.min(column.maxWidth(), line.length()));
                }
            }
            width = Math.max(column.minWidth(), Math.min(column.maxWidth(), width));
            widths[i] = width;
        }
        return widths;
    }

    private String horizontalLine(String left, String intersect, String right, String lineChar, int rowNumberWidth, int padding, int[] columnWidths) {
        StringBuilder builder = new StringBuilder();
        builder.append(left);

        if (rowNumberWidth > 0) {
            builder.append(lineChar.repeat(rowNumberWidth + padding * 2)).append(intersect);
        }

        for (int i = 0; i < columnWidths.length; i++) {
            builder.append(lineChar.repeat(columnWidths[i] + padding * 2));
            builder.append(i == columnWidths.length - 1 ? right : intersect);
        }

        return builder.toString();
    }

    private String titleLine(String title, RenderStyle style, int[] columnWidths, int rowNumberWidth) {
        BorderCharacters border = style.border();
        int separators = Math.max(0, columnWidths.length - 1);
        int totalWidth = IntStream.of(columnWidths).sum() + (style.cellPadding() * 2 * columnWidths.length) + separators;
        if (rowNumberWidth > 0) {
            totalWidth += rowNumberWidth + style.cellPadding() * 2 + 1; // include separator after row numbers
        }
        String paddedTitle = TextUtils.pad(TextUtils.truncate(title, totalWidth), totalWidth, Alignment.CENTER);
        return border.vertical + paddedTitle + border.vertical;
    }

    private String headerLine(Table table, int padding, int rowNumberWidth, int[] columnWidths) {
        BorderCharacters border = table.style().border();
        StringBuilder builder = new StringBuilder();
        builder.append(border.vertical);

        if (rowNumberWidth > 0) {
            builder.append(" ".repeat(padding)).append(" ".repeat(rowNumberWidth)).append(" ".repeat(padding)).append(border.vertical);
        }

        for (int i = 0; i < table.columns().size(); i++) {
            Column column = table.columns().get(i);
            builder.append(" ".repeat(padding));
            builder.append(TextUtils.pad(column.title(), columnWidths[i], column.alignment()));
            builder.append(" ".repeat(padding));
            builder.append(border.vertical);
        }

        return builder.toString();
    }

    private String dataRow(int rowIndex, Row row, Table table, int padding, int rowNumberWidth, int[] columnWidths) {
        BorderCharacters border = table.style().border();
        RenderStyle style = table.style();
        List<List<String>> formattedCells = new ArrayList<>();
        int height = 1;

        for (int i = 0; i < row.cells().size(); i++) {
            Column column = table.columns().get(i);
            Cell cell = row.cells().get(i);
            String formatted = cell.render(table.formatterRegistry(), column.formatter());
            List<String> lines;
            boolean wrapColumn = style.wrapCells() && column.wrap();
            if (wrapColumn) {
                lines = TextUtils.wrap(formatted, columnWidths[i]);
            } else {
                lines = List.of(TextUtils.truncate(formatted, columnWidths[i]));
            }
            height = Math.max(height, lines.size());
            formattedCells.add(lines);
        }

        StringBuilder builder = new StringBuilder();
        for (int lineIndex = 0; lineIndex < height; lineIndex++) {
            builder.append(border.vertical);
            if (rowNumberWidth > 0) {
                String number = lineIndex == 0 ? Integer.toString(rowIndex + 1) : "";
                builder.append(" ".repeat(padding));
                builder.append(TextUtils.pad(number, rowNumberWidth, Alignment.RIGHT));
                builder.append(" ".repeat(padding));
                builder.append(border.vertical);
            }
            for (int col = 0; col < table.columns().size(); col++) {
                Column column = table.columns().get(col);
                List<String> lines = formattedCells.get(col);
                String content = lineIndex < lines.size() ? lines.get(lineIndex) : "";
                builder.append(" ".repeat(padding));
                builder.append(TextUtils.pad(content, columnWidths[col], column.alignment()));
                builder.append(" ".repeat(padding));
                builder.append(border.vertical);
            }
            if (lineIndex < height - 1) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    private String fullWidthLine(String left, String lineChar, String right, int rowNumberWidth, int padding, int[] columnWidths) {
        StringBuilder builder = new StringBuilder();
        builder.append(left);
        int separators = Math.max(0, columnWidths.length - 1);
        int total = IntStream.of(columnWidths).sum() + (padding * 2 * columnWidths.length) + separators;
        if (rowNumberWidth > 0) {
            total += rowNumberWidth + padding * 2 + 1;
        }
        builder.append(lineChar.repeat(total));
        builder.append(right);
        return builder.toString();
    }
}

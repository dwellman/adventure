package com.demo.adventure.buui;

import java.util.List;

public final class ConsolePrinter {

    private static final int DEFAULT_COLUMNS = 80;
    private static final int EDGE_PADDING = 2;
    private static final int LEFT_GUTTER = 2;
    private static volatile boolean muted;

    private ConsolePrinter() {
    }

    public static void setMuted(boolean muted) {
        ConsolePrinter.muted = muted;
    }

    public static boolean isMuted() {
        return muted;
    }

    public static void printWrapped(String text) {
        printWrapped(text, resolveColumns(), EDGE_PADDING);
    }

    public static void print(String text) {
        print(text, resolveColumns(), EDGE_PADDING);
    }

    public static void print(String text, int columns, int edgePadding) {
        if (muted) {
            return;
        }
        if (text == null) {
            return;
        }
        int gutter = LEFT_GUTTER;
        int adjustedColumns = Math.max(1, columns - gutter);
        List<String> lines = MarkdownRenderer.isMarkdown(text)
                ? MarkdownRenderer.renderLines(text, adjustedColumns, edgePadding)
                : wrapLines(text, adjustedColumns, edgePadding);
        for (String line : applyLeftGutter(lines, gutter)) {
            System.out.println(line);
        }
    }

    public static void printWrapped(String text, int columns) {
        printWrapped(text, columns, EDGE_PADDING);
    }

    public static void printWrapped(String text, int columns, int edgePadding) {
        if (muted) {
            return;
        }
        List<String> lines = wrapLines(text, columns, edgePadding);
        for (String line : lines) {
            System.out.println(line);
        }
    }

    public static String renderWrapped(String text) {
        return renderWrapped(text, resolveColumns(), EDGE_PADDING);
    }

    public static String renderWrapped(String text, int columns) {
        return renderWrapped(text, columns, EDGE_PADDING);
    }

    public static String renderWrapped(String text, int columns, int edgePadding) {
        List<String> lines = wrapLines(text, columns, edgePadding);
        return String.join("\n", lines);
    }

    private static List<String> wrapLines(String text, int columns, int edgePadding) {
        int width = effectiveWidth(columns, edgePadding);
        if (text == null) {
            return List.of("");
        }
        List<String> lines = new java.util.ArrayList<>();
        String[] rawLines = text.split("\\R", -1);
        for (String rawLine : rawLines) {
            if (rawLine == null) {
                lines.add("");
                continue;
            }
            if (rawLine.isEmpty()) {
                lines.add("");
                continue;
            }
            String indent = leadingWhitespace(rawLine);
            String content = rawLine.substring(indent.length());
            if (content.isEmpty()) {
                lines.add(indent);
                continue;
            }
            int available = Math.max(1, width - indent.length());
            for (String wrapped : TextUtils.wrap(content, available)) {
                lines.add(indent + wrapped);
            }
        }
        return lines;
    }

    private static List<String> applyLeftGutter(List<String> lines, int gutter) {
        if (lines == null || gutter <= 0) {
            return lines == null ? List.of() : lines;
        }
        String pad = " ".repeat(gutter);
        List<String> out = new java.util.ArrayList<>(lines.size());
        for (String line : lines) {
            if (line == null || line.isEmpty()) {
                out.add("");
                continue;
            }
            out.add(pad + line);
        }
        return out;
    }

    private static int effectiveWidth(int columns, int edgePadding) {
        int resolvedColumns = columns > 0 ? columns : DEFAULT_COLUMNS;
        int padding = Math.max(0, edgePadding);
        int width = resolvedColumns - padding;
        return Math.max(1, width);
    }

    private static int resolveColumns() {
        Integer envColumns = parsePositiveInt(System.getenv("COLUMNS"));
        if (envColumns != null) {
            return envColumns;
        }
        Integer propColumns = parsePositiveInt(System.getProperty("COLUMNS"));
        if (propColumns != null) {
            return propColumns;
        }
        return DEFAULT_COLUMNS;
    }

    private static Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String leadingWhitespace(String text) {
        int idx = 0;
        while (idx < text.length()) {
            char c = text.charAt(idx);
            if (c != ' ' && c != '\t') {
                break;
            }
            idx++;
        }
        return idx == 0 ? "" : text.substring(0, idx);
    }
}

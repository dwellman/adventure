package com.demo.adventure.buui;

import java.util.List;

public final class ConsolePrinter {

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
        printWrapped(text, BuuiLayout.columns(), BuuiLayout.edgePadding());
    }

    public static void print(String text) {
        print(text, BuuiLayout.columns(), BuuiLayout.edgePadding());
    }

    public static void printNarration(String text) {
        printNarration(text, BuuiLayout.columns(), BuuiLayout.edgePadding());
    }

    public static void print(String text, int columns, int edgePadding) {
        if (muted) {
            return;
        }
        if (text == null) {
            return;
        }
        int gutter = BuuiLayout.leftGutter();
        int adjustedColumns = Math.max(1, columns - gutter);
        List<String> lines;
        try {
            lines = MarkdownRenderer.renderLines(text, adjustedColumns, edgePadding);
        } catch (MarkdownValidationException ex) {
            reportMarkdownError(ex);
            return;
        }
        List<String> normalized = stripAnsiIfDisabled(lines);
        for (String line : applyLeftGutter(normalized, gutter)) {
            System.out.println(line);
        }
    }

    public static void println(String text) {
        println(text, BuuiLayout.columns(), BuuiLayout.edgePadding());
    }

    public static void println(String text, int columns, int edgePadding) {
        if (muted) {
            return;
        }
        if (text == null) {
            return;
        }
        int gutter = BuuiLayout.leftGutter();
        int adjustedColumns = Math.max(1, columns - gutter);
        List<String> lines = wrapLines(text, adjustedColumns, edgePadding);
        lines = stripAnsiIfDisabled(lines);
        for (String line : applyLeftGutter(lines, gutter)) {
            System.out.println(line);
        }
    }

    public static void printCompiled(MarkdownDocument document) {
        if (document == null) {
            return;
        }
        printCompiledLines(document.lines());
    }

    public static void printCompiledLines(List<String> lines) {
        if (muted) {
            return;
        }
        List<String> normalized = stripAnsiIfDisabled(lines);
        for (String line : applyLeftGutter(normalized, BuuiLayout.leftGutter())) {
            System.out.println(line);
        }
    }

    public static void printNarration(String text, int columns, int edgePadding) {
        if (muted) {
            return;
        }
        if (text == null) {
            return;
        }
        int gutter = BuuiLayout.leftGutter();
        int adjustedColumns = Math.max(1, columns - gutter);
        List<String> lines;
        try {
            lines = BuuiMarkdown.renderLines(text, adjustedColumns, edgePadding);
        } catch (MarkdownValidationException ex) {
            reportMarkdownError(ex);
            return;
        }
        lines = stripAnsiIfDisabled(lines);
        for (String line : applyNarrationGutter(lines, gutter)) {
            System.out.println(line);
        }
    }

    public static void printWrapped(String text, int columns) {
        printWrapped(text, columns, BuuiLayout.edgePadding());
    }

    public static void printWrapped(String text, int columns, int edgePadding) {
        if (muted) {
            return;
        }
        List<String> lines = wrapLines(text, columns, edgePadding);
        lines = stripAnsiIfDisabled(lines);
        for (String line : lines) {
            System.out.println(line);
        }
    }

    public static String renderWrapped(String text) {
        return renderWrapped(text, BuuiLayout.columns(), BuuiLayout.edgePadding());
    }

    public static String renderWrapped(String text, int columns) {
        return renderWrapped(text, columns, BuuiLayout.edgePadding());
    }

    public static String renderWrapped(String text, int columns, int edgePadding) {
        List<String> lines = wrapLines(text, columns, edgePadding);
        lines = stripAnsiIfDisabled(lines);
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

    private static List<String> stripAnsiIfDisabled(List<String> lines) {
        if (AnsiStyle.isEnabled()) {
            return lines;
        }
        if (lines == null || lines.isEmpty()) {
            return lines == null ? List.of() : lines;
        }
        List<String> out = new java.util.ArrayList<>(lines.size());
        for (String line : lines) {
            out.add(AnsiStyle.strip(line));
        }
        return out;
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

    private static List<String> applyNarrationGutter(List<String> lines, int gutter) {
        if (lines == null || gutter <= 0) {
            return lines == null ? List.of() : lines;
        }
        int safeGutter = Math.max(1, gutter);
        String bullet = ListRenderer.BULLET;
        String bulletPrefix = bullet + " ".repeat(Math.max(0, safeGutter - 1));
        String continuationPrefix = " ".repeat(safeGutter);
        List<String> out = new java.util.ArrayList<>(lines.size());
        boolean newParagraph = true;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                out.add("");
                newParagraph = true;
                continue;
            }
            if (newParagraph) {
                out.add(bulletPrefix + line);
                newParagraph = false;
            } else {
                out.add(continuationPrefix + line);
            }
        }
        return out;
    }

    private static int effectiveWidth(int columns, int edgePadding) {
        int resolvedColumns = columns > 0 ? columns : BuuiLayout.columns();
        int padding = Math.max(0, edgePadding);
        int width = resolvedColumns - padding;
        return Math.max(1, width);
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

    private static void reportMarkdownError(MarkdownValidationException ex) {
        String message = ex == null ? "Unknown markdown validation error." : ex.getMessage();
        System.err.println("Markdown style error: " + message);
    }
}

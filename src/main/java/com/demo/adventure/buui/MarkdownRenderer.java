package com.demo.adventure.buui;

import java.util.List;

public final class MarkdownRenderer {

    private MarkdownRenderer() {
    }

    public static boolean isMarkdown(String text) {
        return MarkdownScanner.hasMarkup(text);
    }

    public static String render(String markdown, int columns, int edgePadding) {
        return MarkdownCompiler.render(markdown, columns, edgePadding);
    }

    public static String render(String markdown, int columns, int edgePadding, MarkdownStyleSheet styles) {
        return MarkdownCompiler.render(markdown, columns, edgePadding, styles);
    }

    public static List<String> renderLines(String markdown, int columns, int edgePadding) {
        return MarkdownCompiler.renderLines(markdown, columns, edgePadding);
    }

    public static List<String> renderLines(String markdown, int columns, int edgePadding, MarkdownStyleSheet styles) {
        return MarkdownCompiler.renderLines(markdown, columns, edgePadding, styles);
    }
}

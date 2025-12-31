package com.demo.adventure.buui;

import java.util.List;

public final class BuuiMarkdown {

    private BuuiMarkdown() {
    }

    public static String render(String text) {
        return render(text, BuuiLayout.columns(), BuuiLayout.edgePadding());
    }

    public static String render(String text, int columns, int edgePadding) {
        List<String> lines = renderLines(text, columns, edgePadding);
        return String.join("\n", lines);
    }

    public static List<String> renderLines(String text, int columns, int edgePadding) {
        String markdown = normalizeNarration(text);
        return MarkdownRenderer.renderLines(markdown, columns, edgePadding);
    }

    private static String normalizeNarration(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (MarkdownScanner.hasMarkup(text)) {
            return text;
        }
        String normalized = text.replace("\r\n", "\n");
        String[] paragraphs = normalized.split("\\n\\s*\\n");
        StringBuilder sb = new StringBuilder();
        for (int p = 0; p < paragraphs.length; p++) {
            String paragraph = paragraphs[p];
            if (paragraph == null || paragraph.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            String[] lines = paragraph.split("\\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i] == null ? "" : lines[i].trim();
                sb.append("> ").append(line);
                if (i < lines.length - 1) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

}

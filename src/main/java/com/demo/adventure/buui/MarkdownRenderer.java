package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownRenderer {

    private static final String FENCE = "```";
    private static final int PARAGRAPH_INDENT = 4;
    private static final int HEADING_SPACING_LINES = 2;

    private MarkdownRenderer() {
    }

    public static boolean isMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String[] lines = text.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i] == null ? "" : lines[i];
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith(FENCE)) {
                return true;
            }
            if (isSceneBreakLine(trimmed)) {
                return true;
            }
            if (isHeadingLine(trimmed) || isListLine(raw)) {
                return true;
            }
            if (looksLikeTable(lines, i)) {
                return true;
            }
        }
        return false;
    }

    public static String render(String markdown, int columns, int edgePadding) {
        List<String> lines = renderLines(markdown, columns, edgePadding);
        return String.join("\n", lines);
    }

    public static List<String> renderLines(String markdown, int columns, int edgePadding) {
        List<String> out = new ArrayList<>();
        if (markdown == null) {
            return out;
        }
        int width = effectiveWidth(columns, edgePadding);
        String[] lines = markdown.split("\\R", -1);
        int i = 0;
        while (i < lines.length) {
            String raw = lines[i] == null ? "" : lines[i];
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                out.add("");
                i++;
                continue;
            }
            if (trimmed.startsWith(FENCE)) {
                i = renderFence(lines, i, out);
                continue;
            }
            if (isSceneBreakLine(trimmed)) {
                renderSceneBreak(width, out);
                i++;
                continue;
            }
            if (looksLikeTable(lines, i)) {
                int next = renderTable(lines, i, out);
                i = next;
                continue;
            }
            if (isListLine(raw)) {
                int next = renderList(lines, i, width, out);
                i = next;
                continue;
            }
            if (isHeadingLine(trimmed)) {
                renderHeading(trimmed, width, out);
                i++;
                continue;
            }

            i = renderParagraph(lines, i, width, out);
        }
        return out;
    }

    private static int renderFence(String[] lines, int startIndex, List<String> out) {
        int i = startIndex + 1;
        while (i < lines.length) {
            String raw = lines[i] == null ? "" : lines[i];
            String trimmed = raw.trim();
            if (trimmed.startsWith(FENCE)) {
                return i + 1;
            }
            out.add(raw);
            i++;
        }
        return i;
    }

    private static int renderTable(String[] lines, int startIndex, List<String> out) {
        String headerLine = lines[startIndex] == null ? "" : lines[startIndex];
        List<String> headers = splitTableCells(headerLine);
        List<List<String>> rows = new ArrayList<>();
        int i = startIndex + 2;
        while (i < lines.length) {
            String raw = lines[i] == null ? "" : lines[i];
            if (raw.trim().isEmpty()) {
                break;
            }
            if (!raw.contains("|")) {
                break;
            }
            rows.add(splitTableCells(raw));
            i++;
        }
        int columns = headers.size();
        for (List<String> row : rows) {
            columns = Math.max(columns, row.size());
        }
        while (headers.size() < columns) {
            headers.add("");
        }
        List<List<?>> normalized = new ArrayList<>();
        for (List<String> row : rows) {
            List<String> padded = new ArrayList<>(row);
            while (padded.size() < columns) {
                padded.add("");
            }
            normalized.add(new ArrayList<>(padded));
        }
        Table table = Table.fromLists(headers, normalized);
        String rendered = new AsciiRenderer().render(table);
        String trimmed = trimTrailingNewlines(rendered);
        if (!trimmed.isEmpty()) {
            for (String line : trimmed.split("\\R", -1)) {
                out.add(line);
            }
        }
        return i;
    }

    private static int renderList(String[] lines, int startIndex, int width, List<String> out) {
        List<ListRenderer.ListItem> items = new ArrayList<>();
        int i = startIndex;
        while (i < lines.length) {
            String raw = lines[i] == null ? "" : lines[i];
            if (!isListLine(raw)) {
                break;
            }
            items.add(parseListItem(raw));
            i++;
        }
        out.addAll(ListRenderer.render(items, width));
        return i;
    }

    private static int renderParagraph(String[] lines, int startIndex, int width, List<String> out) {
        StringBuilder sb = new StringBuilder();
        int i = startIndex;
        while (i < lines.length) {
            String raw = lines[i] == null ? "" : lines[i];
            String trimmed = raw.trim();
            if (trimmed.isEmpty() || isBlockStart(lines, i)) {
                break;
            }
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(stripInlineMarkers(trimmed));
            i++;
        }
        if (!sb.isEmpty()) {
            out.addAll(wrapParagraph(sb.toString(), width, PARAGRAPH_INDENT));
            if (i < lines.length && !lines[i].trim().isEmpty()) {
                out.add("");
            }
        }
        return i;
    }

    private static void renderHeading(String trimmed, int width, List<String> out) {
        int level = headingLevel(trimmed);
        String text = stripInlineMarkers(trimmed.substring(level).trim());
        if (text.isEmpty()) {
            return;
        }
        if (!out.isEmpty() && !out.get(out.size() - 1).isEmpty()) {
            out.add("");
        }
        String centered = centerLine(text, width);
        out.add(centered);
        for (int i = 0; i < HEADING_SPACING_LINES; i++) {
            out.add("");
        }
    }

    private static boolean isBlockStart(String[] lines, int index) {
        String raw = lines[index] == null ? "" : lines[index];
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        if (trimmed.startsWith(FENCE)) {
            return true;
        }
        if (isHeadingLine(trimmed) || isListLine(raw)) {
            return true;
        }
        return looksLikeTable(lines, index);
    }

    private static boolean looksLikeTable(String[] lines, int index) {
        if (index + 1 >= lines.length) {
            return false;
        }
        String header = lines[index] == null ? "" : lines[index];
        String divider = lines[index + 1] == null ? "" : lines[index + 1];
        return header.contains("|") && isTableDivider(divider);
    }

    private static boolean isSceneBreakLine(String trimmed) {
        if (trimmed == null || trimmed.isEmpty()) {
            return false;
        }
        return switch (trimmed) {
            case "***", "* * *", "---", "- - -", "###", "# # #" -> true;
            default -> false;
        };
    }

    private static void renderSceneBreak(int width, List<String> out) {
        if (!out.isEmpty() && !out.get(out.size() - 1).isEmpty()) {
            out.add("");
        }
        String marker = ListRenderer.BULLET + " " + ListRenderer.BULLET + " " + ListRenderer.BULLET;
        out.add(centerLine(marker, width));
        out.add("");
    }

    private static boolean isTableDivider(String line) {
        if (line == null) {
            return false;
        }
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        String noPipes = stripOuterPipes(trimmed).replace("|", "");
        boolean hasDash = false;
        for (int i = 0; i < noPipes.length(); i++) {
            char c = noPipes.charAt(i);
            if (c == '-') {
                hasDash = true;
                continue;
            }
            if (c == ':' || c == ' ' || c == '\t') {
                continue;
            }
            return false;
        }
        return hasDash;
    }

    private static List<String> splitTableCells(String line) {
        String trimmed = stripOuterPipes(line == null ? "" : line.trim());
        String[] parts = trimmed.split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            cells.add(stripInlineMarkers(part.trim()));
        }
        if (cells.isEmpty()) {
            cells.add("");
        }
        return cells;
    }

    private static String stripOuterPipes(String line) {
        String trimmed = line == null ? "" : line.trim();
        if (trimmed.startsWith("|")) {
            trimmed = trimmed.substring(1);
        }
        if (trimmed.endsWith("|")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static boolean isHeadingLine(String trimmed) {
        return trimmed.startsWith("#");
    }

    private static int headingLevel(String trimmed) {
        int level = 0;
        while (level < trimmed.length() && trimmed.charAt(level) == '#') {
            level++;
        }
        return level == 0 ? 1 : level;
    }

    private static boolean isListLine(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        char first = trimmed.charAt(0);
        if ((first == '-' || first == '*' || first == '+') && trimmed.length() > 1 && Character.isWhitespace(trimmed.charAt(1))) {
            return true;
        }
        int dot = trimmed.indexOf('.');
        if (dot > 0) {
            String prefix = trimmed.substring(0, dot);
            if (isDigits(prefix) && trimmed.length() > dot + 1 && Character.isWhitespace(trimmed.charAt(dot + 1))) {
                return true;
            }
        }
        return false;
    }

    private static ListRenderer.ListItem parseListItem(String raw) {
        String line = raw == null ? "" : raw;
        int indent = leadingWhitespace(line);
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new ListRenderer.ListItem(indent, "-", "");
        }
        char first = trimmed.charAt(0);
        if (first == '-' || first == '*' || first == '+') {
            String text = trimmed.substring(1).trim();
            return new ListRenderer.ListItem(indent, String.valueOf(first), stripInlineMarkers(text));
        }
        int dot = trimmed.indexOf('.');
        if (dot > 0) {
            String prefix = trimmed.substring(0, dot);
            if (isDigits(prefix)) {
                String text = trimmed.substring(dot + 1).trim();
                return new ListRenderer.ListItem(indent, prefix + ".", stripInlineMarkers(text));
            }
        }
        return new ListRenderer.ListItem(indent, "-", stripInlineMarkers(trimmed));
    }

    private static int leadingWhitespace(String text) {
        if (text == null) {
            return 0;
        }
        int idx = 0;
        while (idx < text.length()) {
            char c = text.charAt(idx);
            if (c != ' ' && c != '\t') {
                break;
            }
            idx++;
        }
        return idx;
    }

    private static boolean isDigits(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private static String stripInlineMarkers(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("**", "")
                .replace("__", "")
                .replace("`", "");
    }

    private static String trimTrailingNewlines(String text) {
        if (text == null) {
            return "";
        }
        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == '\n' || text.charAt(end - 1) == '\r')) {
            end--;
        }
        return text.substring(0, end);
    }

    private static int effectiveWidth(int columns, int edgePadding) {
        int resolvedColumns = columns > 0 ? columns : 80;
        int padding = Math.max(0, edgePadding);
        int width = resolvedColumns - padding;
        return Math.max(1, width);
    }

    private static List<String> wrapParagraph(String text, int width, int indent) {
        int safeIndent = Math.max(0, indent);
        int available = Math.max(1, width - safeIndent);
        List<String> wrapped = TextUtils.wrap(text, available);
        if (wrapped.isEmpty()) {
            return wrapped;
        }
        List<String> out = new ArrayList<>();
        out.add(" ".repeat(safeIndent) + wrapped.get(0));
        for (int i = 1; i < wrapped.size(); i++) {
            out.add(wrapped.get(i));
        }
        return out;
    }

    private static String centerLine(String text, int width) {
        if (text == null) {
            return "";
        }
        int total = Math.max(text.length(), width);
        int padding = Math.max(0, total - text.length());
        int left = padding / 2;
        return " ".repeat(left) + text;
    }
}

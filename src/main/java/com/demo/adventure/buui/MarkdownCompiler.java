package com.demo.adventure.buui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MarkdownCompiler {

    private static final int PARAGRAPH_INDENT = 0;
    private static final int HEADING_SPACING_LINES = 1;

    private MarkdownCompiler() {
    }

    public static String render(String markdown, int columns, int edgePadding) {
        return render(markdown, columns, edgePadding, MarkdownStyleSheet.defaults());
    }

    public static String render(String markdown, int columns, int edgePadding, MarkdownStyleSheet styles) {
        List<String> lines = renderLines(markdown, columns, edgePadding, styles);
        return String.join("\n", lines);
    }

    public static List<String> renderLines(String markdown, int columns, int edgePadding) {
        return renderLines(markdown, columns, edgePadding, MarkdownStyleSheet.defaults());
    }

    public static List<String> renderLines(String markdown, int columns, int edgePadding, MarkdownStyleSheet styles) {
        List<MarkdownToken> tokens = MarkdownScanner.scan(markdown);
        return renderLines(tokens, columns, edgePadding, styles);
    }

    public static MarkdownDocument compile(String markdown, int columns, int edgePadding) {
        return compile(markdown, columns, edgePadding, MarkdownStyleSheet.defaults());
    }

    public static MarkdownDocument compile(String markdown, int columns, int edgePadding, MarkdownStyleSheet styles) {
        String source = markdown == null ? "" : markdown;
        List<MarkdownToken> tokens = MarkdownScanner.scan(source);
        List<String> lines = renderLines(tokens, columns, edgePadding, styles);
        return new MarkdownDocument(source, tokens, lines);
    }

    public static MarkdownDocument compile(Path path, int columns, int edgePadding) throws IOException {
        return compile(path, columns, edgePadding, MarkdownStyleSheet.defaults());
    }

    public static MarkdownDocument compile(Path path, int columns, int edgePadding, MarkdownStyleSheet styles) throws IOException {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        return compile(source, columns, edgePadding, styles);
    }

    static List<String> renderLines(List<MarkdownToken> tokens, int columns, int edgePadding, MarkdownStyleSheet styles) {
        List<String> out = new ArrayList<>();
        if (tokens == null || tokens.isEmpty()) {
            return out;
        }
        MarkdownStyleSheet sheet = styles == null ? MarkdownStyleSheet.defaults() : styles;
        int width = effectiveWidth(columns, edgePadding);
        int i = 0;
        while (i < tokens.size()) {
            MarkdownToken token = tokens.get(i);
            MarkdownTokenType type = token.type();
            switch (type) {
                case BLANK -> {
                    out.add("");
                    i++;
                }
                case FENCE -> i = renderFence(tokens, i, sheet, out);
                case SCENE_BREAK -> {
                    renderSceneBreak(width, sheet, out);
                    i++;
                }
                case TABLE_HEADER -> i = renderTable(tokens, i, sheet, out);
                case SECTION_LABEL -> {
                    renderSectionLabel(token.text(), sheet, out);
                    i++;
                }
                case EXIT_LINE -> {
                    renderExitLine(token.text(), sheet, out);
                    i++;
                }
                case BLOCKQUOTE -> i = renderBlockquote(tokens, i, width, sheet, out);
                case LIST_ITEM -> i = renderList(tokens, i, width, sheet, out);
                case HEADING -> {
                    renderHeading(token.text(), width, sheet, out);
                    i++;
                }
                case INLINE, TEXT -> i = renderParagraph(tokens, i, width, sheet, out);
                default -> i = renderFallback(tokens, i, width, sheet, out);
            }
        }
        return out;
    }

    private static int renderFence(List<MarkdownToken> tokens, int startIndex, MarkdownStyleSheet styles, List<String> out) {
        int i = startIndex + 1;
        while (i < tokens.size()) {
            MarkdownToken token = tokens.get(i);
            if (token.type() == MarkdownTokenType.FENCE) {
                return i + 1;
            }
            out.add(MarkdownStyleMap.apply(styles.codeBlockStyle(), safeText(token.text())));
            i++;
        }
        return i;
    }

    private static int renderTable(List<MarkdownToken> tokens, int startIndex, MarkdownStyleSheet styles, List<String> out) {
        String headerLine = safeText(tokens.get(startIndex).text());
        List<String> headers = splitTableCells(headerLine);
        for (int idx = 0; idx < headers.size(); idx++) {
            headers.set(idx, InlineMarkdown.format(headers.get(idx), styles.tableHeaderStyle(), styles));
        }
        List<List<String>> rows = new ArrayList<>();
        int i = startIndex + 2;
        while (i < tokens.size()) {
            MarkdownToken token = tokens.get(i);
            if (token.type() != MarkdownTokenType.TABLE_ROW) {
                break;
            }
            List<String> rawCells = splitTableCells(safeText(token.text()));
            List<String> formatted = new ArrayList<>(rawCells.size());
            for (String cell : rawCells) {
                formatted.add(InlineMarkdown.format(cell, styles.tableCellStyle(), styles));
            }
            rows.add(formatted);
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
            out.addAll(Arrays.asList(trimmed.split("\\R", -1)));
        }
        return i;
    }

    private static int renderList(List<MarkdownToken> tokens, int startIndex, int width, MarkdownStyleSheet styles, List<String> out) {
        List<ListRenderer.ListItem> items = new ArrayList<>();
        int i = startIndex;
        while (i < tokens.size()) {
            MarkdownToken token = tokens.get(i);
            if (token.type() != MarkdownTokenType.LIST_ITEM) {
                break;
            }
            items.add(parseListItem(safeText(token.text()), styles));
            i++;
        }
        out.addAll(ListRenderer.render(items, width));
        if (i < tokens.size() && tokens.get(i).type() != MarkdownTokenType.BLANK) {
            out.add("");
        }
        return i;
    }

    private static int renderBlockquote(List<MarkdownToken> tokens, int startIndex, int width, MarkdownStyleSheet styles, List<String> out) {
        StringBuilder sb = new StringBuilder();
        int i = startIndex;
        while (i < tokens.size()) {
            MarkdownToken token = tokens.get(i);
            if (token.type() != MarkdownTokenType.BLOCKQUOTE) {
                break;
            }
            String stripped = stripBlockquoteMarker(safeText(token.text()));
            if (!stripped.isBlank()) {
                if (!sb.isEmpty()) {
                    sb.append(" ");
                }
                sb.append(stripped.trim());
            }
            i++;
        }
        if (!sb.isEmpty()) {
            String formatted = InlineMarkdown.format(sb.toString(), styles.blockquoteStyle(), styles);
            out.addAll(wrapParagraph(formatted, width, 0));
            if (i < tokens.size() && tokens.get(i).type() != MarkdownTokenType.BLANK) {
                out.add("");
            }
        }
        return i;
    }

    private static int renderParagraph(List<MarkdownToken> tokens, int startIndex, int width, MarkdownStyleSheet styles, List<String> out) {
        StringBuilder sb = new StringBuilder();
        int i = startIndex;
        while (i < tokens.size()) {
            MarkdownToken token = tokens.get(i);
            MarkdownTokenType type = token.type();
            if (type == MarkdownTokenType.TEXT || type == MarkdownTokenType.INLINE) {
                String trimmed = safeText(token.text()).trim();
                if (!trimmed.isEmpty()) {
                    if (!sb.isEmpty()) {
                        sb.append(" ");
                    }
                    sb.append(trimmed);
                }
                i++;
                continue;
            }
            if (type == MarkdownTokenType.BLANK || isBlockStartType(type)) {
                break;
            }
            break;
        }
        if (!sb.isEmpty()) {
            String formatted = InlineMarkdown.format(sb.toString(), styles.paragraphStyle(), styles);
            out.addAll(wrapParagraph(formatted, width));
            if (i < tokens.size() && tokens.get(i).type() != MarkdownTokenType.BLANK) {
                out.add("");
            }
        }
        return i;
    }

    private static int renderFallback(List<MarkdownToken> tokens, int startIndex, int width, MarkdownStyleSheet styles, List<String> out) {
        MarkdownToken token = tokens.get(startIndex);
        String text = safeText(token.text()).trim();
        if (!text.isEmpty()) {
            String formatted = InlineMarkdown.format(text, styles.paragraphStyle(), styles);
            out.addAll(wrapParagraph(formatted, width));
        }
        return startIndex + 1;
    }

    private static void renderHeading(String raw, int width, MarkdownStyleSheet styles, List<String> out) {
        String trimmed = raw == null ? "" : raw.trim();
        int level = headingLevel(trimmed);
        BuuiStyle style = styles.headingStyle(level);
        String text = InlineMarkdown.format(trimmed.substring(level).trim(), style, styles);
        if (text.isEmpty()) {
            return;
        }
        if (!out.isEmpty() && !out.get(out.size() - 1).isEmpty()) {
            out.add("");
        }
        out.add(text);
        for (int i = 0; i < HEADING_SPACING_LINES; i++) {
            out.add("");
        }
    }

    private static void renderSectionLabel(String raw, MarkdownStyleSheet styles, List<String> out) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (!out.isEmpty() && !out.get(out.size() - 1).isEmpty()) {
            out.add("");
        }
        out.add(InlineMarkdown.format(trimmed, styles.sectionLabelStyle(), styles));
    }

    private static void renderExitLine(String raw, MarkdownStyleSheet styles, List<String> out) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        out.add(InlineMarkdown.format(trimmed, styles.exitLineStyle(), styles));
    }

    private static void renderSceneBreak(int width, MarkdownStyleSheet styles, List<String> out) {
        if (!out.isEmpty() && !out.get(out.size() - 1).isEmpty()) {
            out.add("");
        }
        String marker = ListRenderer.BULLET + " " + ListRenderer.BULLET + " " + ListRenderer.BULLET;
        String styled = MarkdownStyleMap.apply(styles.sceneBreakStyle(), marker);
        out.add(centerLine(styled, width));
        out.add("");
    }

    private static List<String> splitTableCells(String line) {
        String trimmed = stripOuterPipes(line == null ? "" : line.trim());
        String[] parts = trimmed.split("\\|", -1);
        List<String> cells = new ArrayList<>();
        for (String part : parts) {
            cells.add(part.trim());
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

    private static ListRenderer.ListItem parseListItem(String raw, MarkdownStyleSheet styles) {
        String line = raw == null ? "" : raw;
        int indent = leadingWhitespace(line);
        String trimmed = line.trim();
        if (trimmed.isEmpty()) {
            return new ListRenderer.ListItem(indent, "-", "");
        }
        char first = trimmed.charAt(0);
        if (first == '-' || first == '*' || first == '+') {
            String text = trimmed.substring(1).trim();
            String bullet = MarkdownStyleMap.apply(styles.listBulletStyle(), ListRenderer.BULLET);
            return new ListRenderer.ListItem(indent, bullet, InlineMarkdown.format(text, styles.listItemStyle(), styles));
        }
        int dot = trimmed.indexOf('.');
        if (dot > 0) {
            String prefix = trimmed.substring(0, dot);
            if (isDigits(prefix)) {
                String text = trimmed.substring(dot + 1).trim();
                String bullet = MarkdownStyleMap.apply(styles.listBulletStyle(), prefix + ".");
                return new ListRenderer.ListItem(indent, bullet, InlineMarkdown.format(text, styles.listItemStyle(), styles));
            }
        }
        String bullet = MarkdownStyleMap.apply(styles.listBulletStyle(), ListRenderer.BULLET);
        return new ListRenderer.ListItem(indent, bullet, InlineMarkdown.format(trimmed, styles.listItemStyle(), styles));
    }

    private static String stripBlockquoteMarker(String raw) {
        if (raw == null) {
            return "";
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith(">")) {
            return trimmed;
        }
        String rest = trimmed.substring(1);
        if (!rest.isEmpty() && rest.charAt(0) == ' ') {
            return rest.substring(1);
        }
        return rest;
    }

    private static int headingLevel(String trimmed) {
        int level = 0;
        while (level < trimmed.length() && trimmed.charAt(level) == '#') {
            level++;
        }
        return level == 0 ? 1 : level;
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

    private static List<String> wrapParagraph(String text, int width) {
        return wrapParagraph(text, width, PARAGRAPH_INDENT);
    }

    private static List<String> wrapParagraph(String text, int width, int indent) {
        int safeIndent = Math.max(0, indent);
        int available = Math.max(1, width - safeIndent);
        List<String> wrapped = TextUtils.wrap(text, available);
        if (wrapped.isEmpty()) {
            return wrapped;
        }
        List<String> out = new ArrayList<>();
        String indentPrefix = safeIndent == 0 ? "" : " ".repeat(safeIndent);
        out.add(indentPrefix + wrapped.get(0));
        for (int i = 1; i < wrapped.size(); i++) {
            out.add(wrapped.get(i));
        }
        return out;
    }

    private static String centerLine(String text, int width) {
        if (text == null) {
            return "";
        }
        int length = TextUtils.visibleLength(text);
        int total = Math.max(length, width);
        int padding = Math.max(0, total - length);
        int left = padding / 2;
        return " ".repeat(left) + text;
    }

    private static boolean isBlockStartType(MarkdownTokenType type) {
        return switch (type) {
            case FENCE, HEADING, LIST_ITEM, BLOCKQUOTE, TABLE_HEADER, SCENE_BREAK, SECTION_LABEL, EXIT_LINE -> true;
            default -> false;
        };
    }

    private static String safeText(String text) {
        return text == null ? "" : text;
    }
}

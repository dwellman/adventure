package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.List;

public final class MarkdownScanner {

    private static final String FENCE = "```";

    private MarkdownScanner() {
    }

    public static boolean hasMarkup(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (MarkdownToken token : scan(text)) {
            if (token.type() != MarkdownTokenType.TEXT && token.type() != MarkdownTokenType.BLANK) {
                return true;
            }
        }
        return false;
    }

    public static List<MarkdownToken> scan(String text) {
        List<MarkdownToken> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        String[] lines = text.split("\\R", -1);
        boolean inFence = false;
        int i = 0;
        while (i < lines.length) {
            String raw = lines[i] == null ? "" : lines[i];
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) {
                tokens.add(new MarkdownToken(MarkdownTokenType.BLANK, raw, i));
                i++;
                continue;
            }
            if (trimmed.startsWith(FENCE)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.FENCE, raw, i));
                inFence = !inFence;
                i++;
                continue;
            }
            if (inFence) {
                tokens.add(new MarkdownToken(MarkdownTokenType.CODE_LINE, raw, i));
                i++;
                continue;
            }
            if (isSceneBreakLine(trimmed)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.SCENE_BREAK, raw, i));
                i++;
                continue;
            }
            if (looksLikeTable(lines, i)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.TABLE_HEADER, raw, i));
                tokens.add(new MarkdownToken(MarkdownTokenType.TABLE_DIVIDER, lines[i + 1], i + 1));
                i += 2;
                while (i < lines.length) {
                    String row = lines[i] == null ? "" : lines[i];
                    if (row.trim().isEmpty() || !row.contains("|")) {
                        break;
                    }
                    tokens.add(new MarkdownToken(MarkdownTokenType.TABLE_ROW, row, i));
                    i++;
                }
                continue;
            }
            if (isBlockquoteLine(raw)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.BLOCKQUOTE, raw, i));
                i++;
                continue;
            }
            if (isListLine(raw)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.LIST_ITEM, raw, i));
                i++;
                continue;
            }
            if (isHeadingLine(trimmed)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.HEADING, raw, i));
                i++;
                continue;
            }
            if (isSectionLabelLine(trimmed)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.SECTION_LABEL, raw, i));
                i++;
                continue;
            }
            if (isExitLine(trimmed)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.EXIT_LINE, raw, i));
                i++;
                continue;
            }
            if (InlineMarkdown.hasInlineMarkup(trimmed)) {
                tokens.add(new MarkdownToken(MarkdownTokenType.INLINE, raw, i));
                i++;
                continue;
            }
            tokens.add(new MarkdownToken(MarkdownTokenType.TEXT, raw, i));
            i++;
        }
        return tokens;
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

    private static boolean isSectionLabelLine(String trimmed) {
        if (trimmed == null || trimmed.isEmpty()) {
            return false;
        }
        String normalized = trimmed.toLowerCase(java.util.Locale.ROOT);
        return normalized.equals("items:")
                || normalized.equals("fixtures:")
                || normalized.equals("you see:")
                || normalized.equals("you are carrying:");
    }

    private static boolean isExitLine(String trimmed) {
        if (trimmed == null || trimmed.isEmpty()) {
            return false;
        }
        return trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("exits:");
    }

    private static boolean isBlockquoteLine(String raw) {
        if (raw == null) {
            return false;
        }
        String trimmed = raw.trim();
        return trimmed.startsWith(">");
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
            return isDigits(prefix) && trimmed.length() > dot + 1 && Character.isWhitespace(trimmed.charAt(dot + 1));
        }
        return false;
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
}

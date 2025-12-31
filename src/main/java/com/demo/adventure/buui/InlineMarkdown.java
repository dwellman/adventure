package com.demo.adventure.buui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

public final class InlineMarkdown {

    private InlineMarkdown() {
    }

    public static boolean hasInlineMarkup(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (hasClosingMarker(text, 0, "**") || hasClosingMarker(text, 0, "__")) {
            return true;
        }
        if (hasClosingMarker(text, 0, "*") || hasClosingMarker(text, 0, "_")) {
            return true;
        }
        if (text.indexOf('`') >= 0) {
            return true;
        }
        return hasColorTag(text);
    }

    public static String format(String text) {
        return format(text, BuuiStyle.none(), MarkdownStyleSheet.defaults());
    }

    public static String format(String text, BuuiStyle baseStyle) {
        return format(text, baseStyle, MarkdownStyleSheet.defaults());
    }

    public static String format(String text, BuuiStyle baseStyle, MarkdownStyleSheet styles) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        BuuiStyle base = baseStyle == null ? BuuiStyle.none() : baseStyle;
        MarkdownStyleSheet sheet = styles == null ? MarkdownStyleSheet.defaults() : styles;
        BuuiStyle strongStyle = sheet.strongStyle();
        BuuiStyle emphasisStyle = sheet.emphasisStyle();
        BuuiStyle inlineCodeStyle = sheet.inlineCodeStyle();
        boolean ansiEnabled = AnsiStyle.isEnabled();
        boolean allowBold = strongStyle.bold();
        boolean allowItalic = emphasisStyle.italic();
        boolean allowInlineColors = false;
        if (!hasInlineMarkup(text) && base.isEmpty()) {
            return ansiEnabled ? text : AnsiStyle.strip(text);
        }

        StringBuilder out = new StringBuilder(text.length());
        Deque<AnsiColor> colorStack = new ArrayDeque<>();
        boolean bold = base.bold();
        String boldMarker = null;
        boolean italic = base.italic();
        char italicMarker = '\0';
        AnsiColor color = base.color();
        if (color != null) {
            colorStack.push(color);
        }
        boolean appliedBold = false;
        boolean appliedItalic = false;
        AnsiColor appliedColor = null;
        boolean code = false;
        StringBuilder codeBuffer = new StringBuilder();

        if (!base.isEmpty()) {
            emitStyleChange(out, ansiEnabled, bold, italic, color,
                    appliedBold, appliedItalic, appliedColor);
            appliedBold = bold;
            appliedItalic = italic;
            appliedColor = color;
        }

        int i = 0;
        while (i < text.length()) {
            int ansiEnd = AnsiStyle.consumeAnsiSequence(text, i);
            if (ansiEnd != -1) {
                if (code) {
                    codeBuffer.append(text, i, ansiEnd);
                } else {
                    out.append(text, i, ansiEnd);
                }
                i = ansiEnd;
                continue;
            }
            char c = text.charAt(i);
            if (code) {
                if (c == '`') {
                    code = false;
                    String codeText = codeBuffer.toString();
                    codeBuffer.setLength(0);
                    if (!codeText.isEmpty()) {
                        out.append(MarkdownStyleMap.apply(inlineCodeStyle, codeText));
                        appliedBold = false;
                        appliedItalic = false;
                        appliedColor = null;
                        emitStyleChange(out, ansiEnabled, bold, italic, color,
                                appliedBold, appliedItalic, appliedColor);
                        appliedBold = bold;
                        appliedItalic = italic;
                        appliedColor = color;
                    }
                } else {
                    codeBuffer.append(c);
                }
                i++;
                continue;
            }
            if (c == '\\') {
                if (i + 1 < text.length()) {
                    out.append(text.charAt(i + 1));
                    i += 2;
                } else {
                    out.append(c);
                    i++;
                }
                continue;
            }
            if (c == '`') {
                code = true;
                i++;
                continue;
            }
            if (startsWithMarker(text, i, "**")) {
                if ("**".equals(boldMarker)) {
                    boldMarker = null;
                    if (allowBold) {
                        bold = !bold;
                        emitStyleChange(out, ansiEnabled, bold, italic, color,
                                appliedBold, appliedItalic, appliedColor);
                        appliedBold = bold;
                        appliedItalic = italic;
                        appliedColor = color;
                    }
                    i += 2;
                    continue;
                }
                if (boldMarker == null) {
                    int closing = findClosingMarker(text, i + 2, "**");
                    if (closing != -1 && hasNonBlank(text, i + 2, closing)) {
                        boldMarker = "**";
                        if (allowBold) {
                            bold = !bold;
                            emitStyleChange(out, ansiEnabled, bold, italic, color,
                                    appliedBold, appliedItalic, appliedColor);
                            appliedBold = bold;
                            appliedItalic = italic;
                            appliedColor = color;
                        }
                        i += 2;
                        continue;
                    }
                }
            }
            if (startsWithMarker(text, i, "__")) {
                if ("__".equals(boldMarker)) {
                    boldMarker = null;
                    if (allowBold) {
                        bold = !bold;
                        emitStyleChange(out, ansiEnabled, bold, italic, color,
                                appliedBold, appliedItalic, appliedColor);
                        appliedBold = bold;
                        appliedItalic = italic;
                        appliedColor = color;
                    }
                    i += 2;
                    continue;
                }
                if (boldMarker == null) {
                    int closing = findClosingMarker(text, i + 2, "__");
                    if (closing != -1 && hasNonBlank(text, i + 2, closing)) {
                        boldMarker = "__";
                        if (allowBold) {
                            bold = !bold;
                            emitStyleChange(out, ansiEnabled, bold, italic, color,
                                    appliedBold, appliedItalic, appliedColor);
                            appliedBold = bold;
                            appliedItalic = italic;
                            appliedColor = color;
                        }
                        i += 2;
                        continue;
                    }
                }
            }
            if (c == '*' || c == '_') {
                if (italicMarker == c) {
                    italicMarker = '\0';
                    if (allowItalic) {
                        italic = !italic;
                        emitStyleChange(out, ansiEnabled, bold, italic, color,
                                appliedBold, appliedItalic, appliedColor);
                        appliedBold = bold;
                        appliedItalic = italic;
                        appliedColor = color;
                    }
                    i++;
                    continue;
                }
                if (italicMarker == '\0') {
                    String marker = String.valueOf(c);
                    int closing = findClosingMarker(text, i + 1, marker);
                    if (closing != -1 && hasNonBlank(text, i + 1, closing)) {
                        italicMarker = c;
                        if (allowItalic) {
                            italic = !italic;
                            emitStyleChange(out, ansiEnabled, bold, italic, color,
                                    appliedBold, appliedItalic, appliedColor);
                            appliedBold = bold;
                            appliedItalic = italic;
                            appliedColor = color;
                        }
                        i++;
                        continue;
                    }
                }
            }
            ColorToken token = parseColorToken(text, i);
            if (token != null) {
                if (!allowInlineColors) {
                    throw inlineColorTagError(text, i, token.endIndex);
                }
                if (token.reset) {
                    colorStack.clear();
                    if (base.color() != null) {
                        colorStack.push(base.color());
                    }
                } else if (token.closing) {
                    if (!colorStack.isEmpty()) {
                        colorStack.pop();
                    }
                    if (colorStack.isEmpty() && base.color() != null) {
                        colorStack.push(base.color());
                    }
                } else if (token.color != null) {
                    colorStack.push(token.color);
                }
                color = colorStack.peek();
                emitStyleChange(out, ansiEnabled, bold, italic, color,
                        appliedBold, appliedItalic, appliedColor);
                appliedBold = bold;
                appliedItalic = italic;
                appliedColor = color;
                i = token.endIndex;
                continue;
            }
            out.append(c);
            i++;
        }

        if (code && !codeBuffer.isEmpty()) {
            out.append(MarkdownStyleMap.apply(inlineCodeStyle, codeBuffer.toString()));
            appliedBold = false;
            appliedItalic = false;
            appliedColor = null;
            emitStyleChange(out, ansiEnabled, bold, italic, color,
                    appliedBold, appliedItalic, appliedColor);
            appliedBold = bold;
            appliedItalic = italic;
            appliedColor = color;
        }

        if (ansiEnabled && (appliedBold || appliedItalic || appliedColor != null)) {
            out.append(AnsiStyle.reset());
        }
        String result = out.toString();
        return ansiEnabled ? result : AnsiStyle.strip(result);
    }

    private static boolean hasNonBlank(String text, int start, int end) {
        if (text == null) {
            return false;
        }
        int safeEnd = Math.min(end, text.length());
        for (int i = Math.max(0, start); i < safeEnd; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWithMarker(String text, int index, String marker) {
        return text != null && marker != null && index >= 0
                && index + marker.length() <= text.length()
                && text.startsWith(marker, index);
    }

    private static int findClosingMarker(String text, int fromIndex, String marker) {
        if (text == null || marker == null || marker.isEmpty()) {
            return -1;
        }
        boolean code = false;
        int i = Math.max(0, fromIndex);
        while (i <= text.length() - marker.length()) {
            int ansiEnd = AnsiStyle.consumeAnsiSequence(text, i);
            if (ansiEnd != -1) {
                i = ansiEnd;
                continue;
            }
            char c = text.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == '`') {
                code = !code;
                i++;
                continue;
            }
            if (code) {
                i++;
                continue;
            }
            if (text.startsWith(marker, i)) {
                return i;
            }
            i++;
        }
        return -1;
    }

    private static boolean hasClosingMarker(String text, int fromIndex, String marker) {
        return findClosingMarker(text, fromIndex, marker) != -1;
    }

    private static boolean hasColorTag(String text) {
        if (text == null) {
            return false;
        }
        int idx = text.indexOf('{');
        while (idx != -1) {
            ColorToken token = parseColorToken(text, idx);
            if (token != null) {
                return true;
            }
            idx = text.indexOf('{', idx + 1);
        }
        return false;
    }

    private static ColorToken parseColorToken(String text, int startIndex) {
        if (text == null || startIndex < 0 || startIndex >= text.length()) {
            return null;
        }
        if (text.charAt(startIndex) != '{') {
            return null;
        }
        int end = text.indexOf('}', startIndex + 1);
        if (end == -1) {
            return null;
        }
        String raw = text.substring(startIndex + 1, end).trim();
        if (raw.isEmpty()) {
            return null;
        }
        String normalized = raw.toLowerCase(Locale.ROOT);
        if (normalized.equals("/") || normalized.equals("reset")) {
            return new ColorToken(null, true, true, end + 1);
        }
        boolean closing = normalized.startsWith("/");
        if (closing) {
            normalized = normalized.substring(1).trim();
        }
        AnsiColor color = AnsiColor.fromTag(normalized);
        if (color == null) {
            return null;
        }
        return new ColorToken(color, closing, false, end + 1);
    }

    private static MarkdownValidationException inlineColorTagError(String text, int startIndex, int endIndex) {
        int safeStart = Math.max(0, startIndex);
        int safeEnd = Math.min(text.length(), Math.max(endIndex, safeStart + 1));
        String snippet = text.substring(safeStart, safeEnd);
        int lineStart = text.lastIndexOf('\n', safeStart);
        int column = lineStart == -1 ? safeStart + 1 : safeStart - lineStart;
        return new MarkdownValidationException(
                "Inline color tags are not allowed: " + snippet + " (col " + column + ")"
        );
    }

    private static void emitStyleChange(
            StringBuilder out,
            boolean ansiEnabled,
            boolean bold,
            boolean italic,
            AnsiColor color,
            boolean appliedBold,
            boolean appliedItalic,
            AnsiColor appliedColor
    ) {
        if (!ansiEnabled) {
            return;
        }
        if (bold == appliedBold && italic == appliedItalic && color == appliedColor) {
            return;
        }
        out.append(AnsiStyle.reset());
        String sequence = AnsiStyle.sequence(bold, italic, color);
        if (!sequence.isEmpty()) {
            out.append(sequence);
        }
    }

    private record ColorToken(AnsiColor color, boolean closing, boolean reset, int endIndex) {
    }
}

package com.demo.adventure.buui;

import java.util.EnumMap;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownStyleParser {

    private static final Pattern BLOCK_PATTERN = Pattern.compile("([^\\{]+)\\{([^}]*)\\}", Pattern.DOTALL);
    private static final Pattern COMMENT_PATTERN = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);

    private MarkdownStyleParser() {
    }

    public static EnumMap<MarkdownStyleSelector, MarkdownStyleRule> parse(String css) {
        EnumMap<MarkdownStyleSelector, MarkdownStyleRule> rules = new EnumMap<>(MarkdownStyleSelector.class);
        if (css == null || css.isBlank()) {
            return rules;
        }
        String cleaned = COMMENT_PATTERN.matcher(css).replaceAll("");
        Matcher matcher = BLOCK_PATTERN.matcher(cleaned);
        int lastEnd = 0;
        while (matcher.find()) {
            if (hasNonWhitespace(cleaned, lastEnd, matcher.start())) {
                throw new MarkdownValidationException("Invalid markdown style syntax near: "
                        + snippet(cleaned, lastEnd, matcher.start()));
            }
            String selectorBlock = matcher.group(1).trim();
            String body = matcher.group(2);
            if (selectorBlock.isEmpty()) {
                throw new MarkdownValidationException("Missing markdown style selector before '{'.");
            }
            MarkdownStyleRule rule = parseRuleBody(body);
            for (String selectorRaw : selectorBlock.split(",")) {
                MarkdownStyleSelector selector = MarkdownStyleSelector.fromSelector(selectorRaw);
                if (selector == null) {
                    throw new MarkdownValidationException("Unknown markdown style selector: " + selectorRaw.trim());
                }
                rules.computeIfAbsent(selector, key -> new MarkdownStyleRule()).merge(rule);
            }
            lastEnd = matcher.end();
        }
        if (hasNonWhitespace(cleaned, lastEnd, cleaned.length())) {
            throw new MarkdownValidationException("Invalid markdown style syntax near: "
                    + snippet(cleaned, lastEnd, cleaned.length()));
        }
        return rules;
    }

    private static MarkdownStyleRule parseRuleBody(String body) {
        MarkdownStyleRule rule = new MarkdownStyleRule();
        if (body == null || body.isBlank()) {
            return rule;
        }
        String[] parts = body.split(";");
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            int colon = trimmed.indexOf(':');
            if (colon <= 0) {
                throw new MarkdownValidationException("Invalid markdown style property: " + trimmed);
            }
            String property = trimmed.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = trimmed.substring(colon + 1).trim();
            applyProperty(rule, property, value);
        }
        return rule;
    }

    private static void applyProperty(MarkdownStyleRule rule, String property, String value) {
        switch (property) {
            case "font-weight", "weight", "bold" -> rule.setBold(parseBoolean(value, "font-weight"));
            case "font-style", "style", "italic" -> rule.setItalic(parseBoolean(value, "font-style"));
            case "color" -> applyColor(rule, value);
            default -> throw new MarkdownValidationException("Unknown markdown style property: " + property);
        }
    }

    private static Boolean parseBoolean(String value, String property) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "bold", "italic", "on", "yes" -> true;
            case "false", "normal", "off", "no" -> false;
            default -> throw new MarkdownValidationException("Invalid value for " + property + ": " + value);
        };
    }

    private static void applyColor(MarkdownStyleRule rule, String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new MarkdownValidationException("Invalid value for color: " + value);
        }
        if (normalized.equals("none") || normalized.equals("default") || normalized.equals("reset")) {
            rule.setColor(null);
            return;
        }
        if (normalized.equals("inherit")) {
            return;
        }
        AnsiColor color = AnsiColor.fromTag(normalized);
        if (color == null) {
            throw new MarkdownValidationException("Unknown color: " + value);
        }
        rule.setColor(color);
    }

    private static boolean hasNonWhitespace(String text, int start, int end) {
        int safeStart = Math.max(0, start);
        int safeEnd = Math.min(text.length(), Math.max(safeStart, end));
        for (int i = safeStart; i < safeEnd; i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private static String snippet(String text, int start, int end) {
        int safeStart = Math.max(0, start);
        int safeEnd = Math.min(text.length(), Math.max(safeStart, end));
        String slice = text.substring(safeStart, safeEnd).trim();
        if (slice.isEmpty()) {
            return "<empty>";
        }
        String normalized = slice.replaceAll("\\s+", " ");
        if (normalized.length() > 60) {
            return normalized.substring(0, 60) + "...";
        }
        return normalized;
    }
}

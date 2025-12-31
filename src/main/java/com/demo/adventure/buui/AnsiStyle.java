package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.List;

public final class AnsiStyle {

    private static final char ESC = '\u001b';
    private static final String CSI = "\u001b[";
    private static final String RESET = CSI + "0m";
    private static volatile Boolean enabledOverride;

    private AnsiStyle() {
    }

    public static boolean isEnabled() {
        if (enabledOverride != null) {
            return enabledOverride;
        }
        String env = System.getenv("NO_COLOR");
        if (env != null && !env.isBlank()) {
            return false;
        }
        String prop = System.getProperty("NO_COLOR");
        if (prop != null && !prop.isBlank()) {
            return false;
        }
        return true;
    }

    static void setEnabledOverride(Boolean enabled) {
        enabledOverride = enabled;
    }

    public static String reset() {
        return RESET;
    }

    public static String sequence(boolean bold, boolean italic, AnsiColor color) {
        if (!isEnabled()) {
            return "";
        }
        List<String> codes = new ArrayList<>();
        if (bold) {
            codes.add("1");
        }
        if (italic) {
            codes.add("3");
        }
        if (color != null) {
            codes.add(Integer.toString(color.code()));
        }
        if (codes.isEmpty()) {
            return "";
        }
        return CSI + String.join(";", codes) + "m";
    }

    public static String wrap(boolean bold, boolean italic, AnsiColor color, String text) {
        String safe = text == null ? "" : text;
        if (!isEnabled()) {
            return safe;
        }
        String seq = sequence(bold, italic, color);
        if (seq.isEmpty()) {
            return safe;
        }
        return seq + safe + RESET;
    }

    public static boolean containsAnsi(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            int end = consumeAnsiSequence(text, i);
            if (end != -1) {
                return true;
            }
        }
        return false;
    }

    public static String strip(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int end = consumeAnsiSequence(text, i);
            if (end != -1) {
                i = end;
                continue;
            }
            out.append(text.charAt(i));
            i++;
        }
        return out.toString();
    }

    static int consumeAnsiSequence(String text, int startIndex) {
        if (text == null || startIndex < 0 || startIndex >= text.length()) {
            return -1;
        }
        if (text.charAt(startIndex) != ESC) {
            return -1;
        }
        int i = startIndex + 1;
        if (i >= text.length() || text.charAt(i) != '[') {
            return -1;
        }
        i++;
        while (i < text.length()) {
            char c = text.charAt(i);
            if ((c >= '0' && c <= '9') || c == ';') {
                i++;
                continue;
            }
            if (c == 'm') {
                return i + 1;
            }
            return -1;
        }
        return -1;
    }
}

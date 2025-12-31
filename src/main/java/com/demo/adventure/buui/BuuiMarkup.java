package com.demo.adventure.buui;

public final class BuuiMarkup {

    private BuuiMarkup() {
    }

    public static String bold(String text) {
        return wrap("**", text);
    }

    public static String italic(String text) {
        return wrap("_", text);
    }

    public static String color(AnsiColor color, String text) {
        if (color == null) {
            return text == null ? "" : text;
        }
        return colorTag(color) + (text == null ? "" : text) + resetTag();
    }

    public static String colorTag(AnsiColor color) {
        if (color == null) {
            return "";
        }
        return "{" + color.tag() + "}";
    }

    public static String colorCloseTag(AnsiColor color) {
        if (color == null) {
            return "";
        }
        return "{/" + color.tag() + "}";
    }

    public static String resetTag() {
        return "{/}";
    }

    private static String wrap(String marker, String text) {
        String safe = text == null ? "" : text;
        if (marker == null || marker.isEmpty()) {
            return safe;
        }
        return marker + safe + marker;
    }
}

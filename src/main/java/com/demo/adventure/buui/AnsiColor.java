package com.demo.adventure.buui;

import java.util.Locale;

public enum AnsiColor {
    BLACK("black", 30),
    RED("red", 31),
    GREEN("green", 32),
    YELLOW("yellow", 33),
    BLUE("blue", 34),
    MAGENTA("magenta", 35),
    CYAN("cyan", 36),
    WHITE("white", 37),
    BRIGHT_BLACK("bright_black", 90),
    BRIGHT_RED("bright_red", 91),
    BRIGHT_GREEN("bright_green", 92),
    BRIGHT_YELLOW("bright_yellow", 93),
    BRIGHT_BLUE("bright_blue", 94),
    BRIGHT_MAGENTA("bright_magenta", 95),
    BRIGHT_CYAN("bright_cyan", 96),
    BRIGHT_WHITE("bright_white", 97);

    private final String tag;
    private final int code;

    AnsiColor(String tag, int code) {
        this.tag = tag;
        this.code = code;
    }

    public String tag() {
        return tag;
    }

    public int code() {
        return code;
    }

    public static AnsiColor fromTag(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        normalized = switch (normalized) {
            case "gray", "grey", "bright_gray", "bright_grey" -> "bright_black";
            default -> normalized;
        };
        for (AnsiColor color : values()) {
            if (color.tag.equals(normalized)) {
                return color;
            }
        }
        return null;
    }
}

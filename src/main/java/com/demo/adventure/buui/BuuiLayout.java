package com.demo.adventure.buui;

public final class BuuiLayout {

    private static final int DEFAULT_COLUMNS = 80;
    private static final int DEFAULT_EDGE_PADDING = 2;
    private static final int DEFAULT_LEFT_GUTTER = 2;
    private static final String COLUMNS_ENV = "COLUMNS";
    private static final String COLUMNS_PROP = "COLUMNS";
    private static final String GUTTER_ENV = "BUUI_GUTTER";
    private static final String GUTTER_PROP = "BUUI_GUTTER";
    private static final String EDGE_ENV = "BUUI_EDGE_PADDING";
    private static final String EDGE_PROP = "BUUI_EDGE_PADDING";

    private BuuiLayout() {
    }

    public static int columns() {
        Integer envColumns = parsePositiveInt(System.getenv(COLUMNS_ENV));
        if (envColumns != null) {
            return envColumns;
        }
        Integer propColumns = parsePositiveInt(System.getProperty(COLUMNS_PROP));
        if (propColumns != null) {
            return propColumns;
        }
        return DEFAULT_COLUMNS;
    }

    public static int edgePadding() {
        Integer propPadding = parseNonNegativeInt(System.getProperty(EDGE_PROP));
        if (propPadding != null) {
            return propPadding;
        }
        Integer envPadding = parseNonNegativeInt(System.getenv(EDGE_ENV));
        if (envPadding != null) {
            return envPadding;
        }
        return DEFAULT_EDGE_PADDING;
    }

    public static int leftGutter() {
        Integer propGutter = parseNonNegativeInt(System.getProperty(GUTTER_PROP));
        if (propGutter != null) {
            return propGutter;
        }
        Integer envGutter = parseNonNegativeInt(System.getenv(GUTTER_ENV));
        if (envGutter != null) {
            return envGutter;
        }
        return DEFAULT_LEFT_GUTTER;
    }

    private static Integer parsePositiveInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Integer parseNonNegativeInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

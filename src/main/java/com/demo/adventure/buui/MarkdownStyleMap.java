package com.demo.adventure.buui;

public final class MarkdownStyleMap {

    private static final BuuiStyle HEADING_1 = new BuuiStyle(true, false, AnsiColor.BRIGHT_BLUE);
    private static final BuuiStyle HEADING_2 = new BuuiStyle(true, false, AnsiColor.BRIGHT_CYAN);
    private static final BuuiStyle HEADING_3 = new BuuiStyle(true, false, AnsiColor.BRIGHT_MAGENTA);
    private static final BuuiStyle HEADING_4 = new BuuiStyle(true, false, AnsiColor.BRIGHT_WHITE);
    private static final BuuiStyle TABLE_HEADER = new BuuiStyle(true, false, AnsiColor.BRIGHT_WHITE);
    private static final BuuiStyle TABLE_CELL = BuuiStyle.none();
    private static final BuuiStyle PARAGRAPH = BuuiStyle.none();
    private static final BuuiStyle LIST_ITEM = BuuiStyle.none();
    private static final BuuiStyle STRONG = new BuuiStyle(true, false, null);
    private static final BuuiStyle EMPHASIS = new BuuiStyle(false, true, null);
    private static final BuuiStyle LIST_BULLET = new BuuiStyle(false, false, AnsiColor.BRIGHT_CYAN);
    private static final BuuiStyle BLOCKQUOTE = new BuuiStyle(false, true, AnsiColor.BRIGHT_BLACK);
    private static final BuuiStyle CODE_BLOCK = new BuuiStyle(false, false, AnsiColor.BRIGHT_BLACK);
    private static final BuuiStyle INLINE_CODE = new BuuiStyle(false, false, AnsiColor.BRIGHT_BLACK);
    private static final BuuiStyle SCENE_BREAK = new BuuiStyle(false, false, AnsiColor.BRIGHT_BLACK);

    private MarkdownStyleMap() {
    }

    public static BuuiStyle headingStyle(int level) {
        if (level <= 1) {
            return HEADING_1;
        }
        if (level == 2) {
            return HEADING_2;
        }
        if (level == 3) {
            return HEADING_3;
        }
        return HEADING_4;
    }

    public static BuuiStyle tableHeaderStyle() {
        return TABLE_HEADER;
    }

    public static BuuiStyle tableCellStyle() {
        return TABLE_CELL;
    }

    public static BuuiStyle paragraphStyle() {
        return PARAGRAPH;
    }

    public static BuuiStyle listItemStyle() {
        return LIST_ITEM;
    }

    public static BuuiStyle strongStyle() {
        return STRONG;
    }

    public static BuuiStyle emphasisStyle() {
        return EMPHASIS;
    }

    public static BuuiStyle listBulletStyle() {
        return LIST_BULLET;
    }

    public static BuuiStyle blockquoteStyle() {
        return BLOCKQUOTE;
    }

    public static BuuiStyle codeBlockStyle() {
        return CODE_BLOCK;
    }

    public static BuuiStyle inlineCodeStyle() {
        return INLINE_CODE;
    }

    public static BuuiStyle sceneBreakStyle() {
        return SCENE_BREAK;
    }

    public static BuuiStyle sectionLabelStyle() {
        return PARAGRAPH;
    }

    public static BuuiStyle exitLineStyle() {
        return PARAGRAPH;
    }

    public static String apply(BuuiStyle style, String text) {
        if (style == null || style.isEmpty()) {
            return text == null ? "" : text;
        }
        return AnsiStyle.wrap(style.bold(), style.italic(), style.color(), text == null ? "" : text);
    }
}

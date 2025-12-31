package com.demo.adventure.buui;

import java.util.Locale;

public enum MarkdownStyleSelector {
    HEADING_1("heading-1"),
    HEADING_2("heading-2"),
    HEADING_3("heading-3"),
    HEADING_4("heading-4"),
    TABLE_HEADER("table-header"),
    TABLE_CELL("table-cell"),
    PARAGRAPH("paragraph"),
    LIST_ITEM("list-item"),
    LIST_BULLET("list-bullet"),
    BLOCKQUOTE("blockquote"),
    CODE_BLOCK("code-block"),
    INLINE_CODE("inline-code"),
    SCENE_BREAK("scene-break"),
    STRONG("strong"),
    EMPHASIS("emphasis"),
    SECTION_LABEL("section-label"),
    EXIT_LINE("exit-line");

    private final String selector;

    MarkdownStyleSelector(String selector) {
        this.selector = selector;
    }

    public String selector() {
        return selector;
    }

    public static MarkdownStyleSelector fromSelector(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "h1", "heading1", "heading-1" -> HEADING_1;
            case "h2", "heading2", "heading-2" -> HEADING_2;
            case "h3", "heading3", "heading-3" -> HEADING_3;
            case "h4", "heading4", "heading-4", "heading", "heading-default" -> HEADING_4;
            case "th", "table-header", "tableheader" -> TABLE_HEADER;
            case "td", "table-cell", "tablecell" -> TABLE_CELL;
            case "p", "paragraph", "body" -> PARAGRAPH;
            case "li", "list-item", "listitem" -> LIST_ITEM;
            case "list-bullet", "bullet", "list-marker", "listmarker" -> LIST_BULLET;
            case "blockquote", "quote" -> BLOCKQUOTE;
            case "pre", "code-block", "codeblock", "code-fence" -> CODE_BLOCK;
            case "code", "inline-code", "inlinecode" -> INLINE_CODE;
            case "hr", "scene-break", "scenebreak" -> SCENE_BREAK;
            case "strong", "bold" -> STRONG;
            case "em", "emphasis", "italic" -> EMPHASIS;
            case "section-label", "sectionlabel" -> SECTION_LABEL;
            case "exit-line", "exitline", "exits" -> EXIT_LINE;
            default -> null;
        };
    }
}

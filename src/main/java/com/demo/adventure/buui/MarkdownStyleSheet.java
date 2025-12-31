package com.demo.adventure.buui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.EnumMap;
import java.util.Map;

public final class MarkdownStyleSheet {

    private static final String STYLE_ENV = "BUUI_STYLE";
    private static final String STYLE_PROP = "BUUI_STYLE";
    private static final Object DEFAULT_LOCK = new Object();
    private static volatile MarkdownStyleSheet DEFAULT;
    private static volatile MarkdownValidationException DEFAULT_ERROR;

    private final EnumMap<MarkdownStyleSelector, BuuiStyle> styles;
    private final boolean plain;

    private MarkdownStyleSheet(EnumMap<MarkdownStyleSelector, BuuiStyle> styles, boolean plain) {
        this.styles = styles;
        this.plain = plain;
    }

    public static MarkdownStyleSheet defaults() {
        MarkdownStyleSheet cached = DEFAULT;
        if (cached != null) {
            return cached;
        }
        MarkdownValidationException cachedError = DEFAULT_ERROR;
        if (cachedError != null) {
            throw cachedError;
        }
        synchronized (DEFAULT_LOCK) {
            if (DEFAULT != null) {
                return DEFAULT;
            }
            if (DEFAULT_ERROR != null) {
                throw DEFAULT_ERROR;
            }
            try {
                MarkdownStyleSheet loaded = loadConfiguredOrPlain();
                DEFAULT = loaded;
                return loaded;
            } catch (MarkdownValidationException ex) {
                DEFAULT_ERROR = ex;
                throw ex;
            }
        }
    }

    public static MarkdownStyleSheet plain() {
        EnumMap<MarkdownStyleSelector, BuuiStyle> map = new EnumMap<>(MarkdownStyleSelector.class);
        for (MarkdownStyleSelector selector : EnumSet.allOf(MarkdownStyleSelector.class)) {
            map.put(selector, BuuiStyle.none());
        }
        return new MarkdownStyleSheet(map, true);
    }

    public static MarkdownStyleSheet parse(String css) {
        MarkdownStyleSheet base = plain();
        EnumMap<MarkdownStyleSelector, BuuiStyle> merged = new EnumMap<>(base.styles);
        Map<MarkdownStyleSelector, MarkdownStyleRule> overrides = MarkdownStyleParser.parse(css);
        for (Map.Entry<MarkdownStyleSelector, MarkdownStyleRule> entry : overrides.entrySet()) {
            MarkdownStyleSelector selector = entry.getKey();
            MarkdownStyleRule rule = entry.getValue();
            BuuiStyle baseStyle = merged.getOrDefault(selector, BuuiStyle.none());
            merged.put(selector, applyRule(baseStyle, rule));
        }
        return new MarkdownStyleSheet(merged, false);
    }

    public static MarkdownStyleSheet load(Path path) throws IOException {
        String css = Files.readString(path, StandardCharsets.UTF_8);
        return parse(css);
    }

    public BuuiStyle headingStyle(int level) {
        if (level <= 1) {
            return style(MarkdownStyleSelector.HEADING_1);
        }
        if (level == 2) {
            return style(MarkdownStyleSelector.HEADING_2);
        }
        if (level == 3) {
            return style(MarkdownStyleSelector.HEADING_3);
        }
        return style(MarkdownStyleSelector.HEADING_4);
    }

    public BuuiStyle tableHeaderStyle() {
        return style(MarkdownStyleSelector.TABLE_HEADER);
    }

    public BuuiStyle tableCellStyle() {
        return style(MarkdownStyleSelector.TABLE_CELL);
    }

    public BuuiStyle paragraphStyle() {
        return style(MarkdownStyleSelector.PARAGRAPH);
    }

    public BuuiStyle listItemStyle() {
        return style(MarkdownStyleSelector.LIST_ITEM);
    }

    public BuuiStyle listBulletStyle() {
        return style(MarkdownStyleSelector.LIST_BULLET);
    }

    public BuuiStyle blockquoteStyle() {
        return style(MarkdownStyleSelector.BLOCKQUOTE);
    }

    public BuuiStyle codeBlockStyle() {
        return style(MarkdownStyleSelector.CODE_BLOCK);
    }

    public BuuiStyle inlineCodeStyle() {
        return style(MarkdownStyleSelector.INLINE_CODE);
    }

    public BuuiStyle sceneBreakStyle() {
        return style(MarkdownStyleSelector.SCENE_BREAK);
    }

    public BuuiStyle strongStyle() {
        return style(MarkdownStyleSelector.STRONG);
    }

    public BuuiStyle emphasisStyle() {
        return style(MarkdownStyleSelector.EMPHASIS);
    }

    public BuuiStyle sectionLabelStyle() {
        return style(MarkdownStyleSelector.SECTION_LABEL);
    }

    public BuuiStyle exitLineStyle() {
        return style(MarkdownStyleSelector.EXIT_LINE);
    }

    public BuuiStyle style(MarkdownStyleSelector selector) {
        if (selector == null) {
            return BuuiStyle.none();
        }
        return styles.getOrDefault(selector, BuuiStyle.none());
    }

    public boolean isPlain() {
        return plain;
    }

    private static BuuiStyle applyRule(BuuiStyle base, MarkdownStyleRule rule) {
        if (rule == null) {
            return base;
        }
        boolean bold = rule.bold() == null ? base.bold() : rule.bold();
        boolean italic = rule.italic() == null ? base.italic() : rule.italic();
        AnsiColor color = rule.colorSet() ? rule.color() : base.color();
        return new BuuiStyle(bold, italic, color);
    }

    private static MarkdownStyleSheet loadConfiguredOrPlain() {
        String path = resolveStylePath();
        if (path == null) {
            return plain();
        }
        Path stylePath = Path.of(path);
        try {
            String css = Files.readString(stylePath, StandardCharsets.UTF_8);
            return parse(css);
        } catch (IOException ex) {
            throw new MarkdownValidationException("Failed to read style file " + stylePath + ": " + ex.getMessage(), ex);
        } catch (MarkdownValidationException ex) {
            throw new MarkdownValidationException("Invalid style file " + stylePath + ": " + ex.getMessage(), ex);
        }
    }

    private static String resolveStylePath() {
        String prop = normalizePath(System.getProperty(STYLE_PROP));
        if (prop != null) {
            return prop;
        }
        String env = normalizePath(System.getenv(STYLE_ENV));
        if (env != null) {
            return env;
        }
        return null;
    }

    private static String normalizePath(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

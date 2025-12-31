package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class BuuiMenu {

    public record MenuItem(String key, String label, String description) {
        public MenuItem {
            if (key == null) {
                key = "";
            }
            if (label == null) {
                label = "";
            }
            if (description == null) {
                description = "";
            }
        }
    }

    private final String title;
    private final String keyHeader;
    private final String itemHeader;
    private final String descriptionHeader;
    private final int minKeyWidth;
    private final List<MenuItem> items;
    private final RenderStyle style;

    private BuuiMenu(
            String title,
            String keyHeader,
            String itemHeader,
            String descriptionHeader,
            int minKeyWidth,
            List<MenuItem> items,
            RenderStyle style
    ) {
        this.title = title;
        this.keyHeader = keyHeader;
        this.itemHeader = itemHeader;
        this.descriptionHeader = descriptionHeader;
        this.minKeyWidth = minKeyWidth;
        this.items = items;
        this.style = style;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static String prompt(String noun, int count, String exitKey) {
        return prompt(noun, count, exitKey, defaultExitLabel(exitKey));
    }

    public static String prompt(String noun, int count, String exitKey, String exitLabel) {
        String safeNoun = noun == null || noun.isBlank() ? "item" : noun.trim();
        String safeExitKey = exitKey == null || exitKey.isBlank() ? "q" : exitKey.trim();
        String safeExitLabel = exitLabel == null || exitLabel.isBlank() ? "quit" : exitLabel.trim();
        int safeCount = Math.max(0, count);
        return "Select a " + safeNoun + " (1-" + safeCount + ", or " + safeExitKey + " to " + safeExitLabel + "): ";
    }

    public Table buildTable() {
        TableBuilder builder = Table.builder()
                .title(title)
                .showRowNumbers(false)
                .style(style);

        int keyWidth = computeKeyWidth();
        builder.addColumn(Column.builder(keyHeader)
                .width(keyWidth)
                .alignment(Alignment.RIGHT)
                .wrap(false)
                .build());
        builder.addColumn(Column.builder(itemHeader).build());
        builder.addColumn(Column.builder(descriptionHeader).build());

        for (MenuItem item : items) {
            builder.addRow(item.key(), item.label(), item.description());
        }

        return builder.build();
    }

    public String render() {
        return new AsciiRenderer().render(buildTable());
    }

    private int computeKeyWidth() {
        int width = Math.max(minKeyWidth, safeLength(keyHeader));
        for (MenuItem item : items) {
            width = Math.max(width, safeLength(item.key()));
        }
        return Math.max(1, width);
    }

    private static int safeLength(String text) {
        return text == null ? 0 : text.length();
    }

    private static String defaultExitLabel(String exitKey) {
        if (exitKey == null || exitKey.isBlank()) {
            return "quit";
        }
        String normalized = exitKey.trim().toLowerCase(Locale.ROOT);
        if ("q".equals(normalized)) {
            return "quit";
        }
        if ("b".equals(normalized)) {
            return "go back";
        }
        return "exit";
    }

    public static final class Builder {
        private String title = "";
        private String keyHeader = "#";
        private String itemHeader = "Item";
        private String descriptionHeader = "Description";
        private int minKeyWidth = 3;
        private final List<MenuItem> items = new ArrayList<>();
        private RenderStyle style = RenderStyle.defaults();

        private Builder() {
        }

        public Builder title(String title) {
            this.title = title == null ? "" : title;
            return this;
        }

        public Builder keyHeader(String keyHeader) {
            if (keyHeader != null && !keyHeader.isBlank()) {
                this.keyHeader = keyHeader;
            }
            return this;
        }

        public Builder itemHeader(String itemHeader) {
            if (itemHeader != null && !itemHeader.isBlank()) {
                this.itemHeader = itemHeader;
            }
            return this;
        }

        public Builder descriptionHeader(String descriptionHeader) {
            if (descriptionHeader != null && !descriptionHeader.isBlank()) {
                this.descriptionHeader = descriptionHeader;
            }
            return this;
        }

        public Builder minKeyWidth(int minKeyWidth) {
            if (minKeyWidth > 0) {
                this.minKeyWidth = minKeyWidth;
            }
            return this;
        }

        public Builder style(RenderStyle style) {
            if (style != null) {
                this.style = style;
            }
            return this;
        }

        public Builder addItem(String key, String label, String description) {
            items.add(new MenuItem(key, label, description));
            return this;
        }

        public BuuiMenu build() {
            return new BuuiMenu(
                    title,
                    keyHeader,
                    itemHeader,
                    descriptionHeader,
                    minKeyWidth,
                    List.copyOf(items),
                    style
            );
        }
    }
}

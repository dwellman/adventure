package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.List;

public final class ListRenderer {

    private ListRenderer() {
    }

    public static final String BULLET = "•";

    public record ListItem(int indent, String bullet, String text) {
        public ListItem {
            if (bullet == null) {
                bullet = BULLET;
            }
            if ("-".equals(bullet) || "*".equals(bullet) || "+".equals(bullet)) {
                bullet = BULLET;
            }
            if (text == null) {
                text = "";
            }
        }
    }

    public static List<String> render(List<ListItem> items, int width) {
        List<String> lines = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return lines;
        }
        int effectiveWidth = Math.max(1, width);
        for (ListItem item : items) {
            if (item == null) {
                continue;
            }
            int indent = Math.max(0, item.indent());
            String indentPrefix = " ".repeat(indent);
            String bullet = item.bullet();
            String text = TextUtils.trimWhitespace(item.text());
            int bulletVisible = TextUtils.visibleLength(bullet);
            String prefix = indentPrefix + bullet + " ";
            if (text.isEmpty()) {
                lines.add(indentPrefix + bullet);
                continue;
            }
            int available = Math.max(1, effectiveWidth - (indent + bulletVisible + 1));
            List<String> wrapped = TextUtils.wrap(text, available);
            for (int i = 0; i < wrapped.size(); i++) {
                String line = wrapped.get(i);
                if (i == 0) {
                    lines.add(prefix + line);
                } else {
                    String pad = indentPrefix + " ".repeat(bulletVisible + 1);
                    lines.add(pad + line);
                }
            }
        }
        return lines;
    }
}

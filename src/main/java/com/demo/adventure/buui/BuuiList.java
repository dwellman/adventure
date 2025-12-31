package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class BuuiList {

    private static final int LIST_INDENT = 2;

    private BuuiList() {
    }

    public static String render(String title, List<String> options) {
        return render(title, options, BuuiLayout.columns(), BuuiLayout.edgePadding());
    }

    public static String render(String title, String... options) {
        List<String> list = options == null ? List.of() : Arrays.asList(options);
        return render(title, list);
    }

    public static void print(String title, List<String> options) {
        String rendered = render(title, options);
        if (rendered.isBlank()) {
            return;
        }
        ConsolePrinter.printWrapped(rendered, BuuiLayout.columns(), BuuiLayout.edgePadding());
    }

    public static void print(String title, String... options) {
        List<String> list = options == null ? List.of() : Arrays.asList(options);
        print(title, list);
    }

    static String render(String title, List<String> options, int columns, int edgePadding) {
        int width = effectiveWidth(columns, edgePadding);
        List<String> lines = new ArrayList<>();

        String safeTitle = title == null ? "" : title.trim();
        if (!safeTitle.isBlank()) {
            String formattedTitle = InlineMarkdown.format(safeTitle);
            lines.addAll(TextUtils.wrap(formattedTitle, width));
        }

        List<ListRenderer.ListItem> items = new ArrayList<>();
        if (options != null) {
            int index = 1;
            for (String option : options) {
                String text = option == null ? "" : option;
                items.add(new ListRenderer.ListItem(LIST_INDENT, index + ".", InlineMarkdown.format(text)));
                index++;
            }
        }

        if (!lines.isEmpty() && !items.isEmpty()) {
            lines.add("");
        }
        if (!items.isEmpty()) {
            lines.addAll(ListRenderer.render(items, width));
        }

        return String.join("\n", lines);
    }

    private static int effectiveWidth(int columns, int edgePadding) {
        int resolvedColumns = columns > 0 ? columns : BuuiLayout.columns();
        int padding = Math.max(0, edgePadding);
        int width = resolvedColumns - padding;
        return Math.max(1, width);
    }
}

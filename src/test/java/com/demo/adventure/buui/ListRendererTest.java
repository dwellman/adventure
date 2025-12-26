package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListRendererTest {

    @Test
    void renderHandlesEmptyInput() {
        assertThat(ListRenderer.render(null, 10)).isEmpty();
        assertThat(ListRenderer.render(List.of(), 10)).isEmpty();
    }

    @Test
    void renderNormalizesBulletAndIndent() {
        ListRenderer.ListItem item = new ListRenderer.ListItem(-2, "-", "alpha beta");
        List<String> lines = ListRenderer.render(List.of(item), 10);

        assertThat(lines).containsExactly("\u2022 alpha", "  beta");
    }

    @Test
    void renderSupportsEmptyTextAndSkipsNullItems() {
        ListRenderer.ListItem item = new ListRenderer.ListItem(2, "*", " ");
        List<ListRenderer.ListItem> items = Arrays.asList(null, item);
        List<String> lines = ListRenderer.render(items, 20);

        assertThat(lines).containsExactly("  \u2022");
    }
}

package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuuiMarkdownTest {

    @Test
    void renderWrapsPlainTextAsNarration() {
        String rendered = BuuiMarkdown.render("Alpha beta", 60, 0);

        assertThat(AnsiStyle.strip(rendered)).isEqualTo("Alpha beta");
    }

    @Test
    void renderLeavesMarkdownUntouched() {
        String rendered = BuuiMarkdown.render("- alpha", 60, 0);

        assertThat(AnsiStyle.strip(rendered)).isEqualTo("\u2022 alpha");
    }
}

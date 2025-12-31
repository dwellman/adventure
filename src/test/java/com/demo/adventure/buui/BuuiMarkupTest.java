package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuuiMarkupTest {

    @Test
    void wrapsBoldAndItalic() {
        assertThat(BuuiMarkup.bold("Bold")).isEqualTo("**Bold**");
        assertThat(BuuiMarkup.italic("Ital")).isEqualTo("_Ital_");
    }

    @Test
    void wrapsColorsWithTags() {
        assertThat(BuuiMarkup.color(AnsiColor.RED, "Alert")).isEqualTo("{red}Alert{/}");
        assertThat(BuuiMarkup.colorTag(AnsiColor.BRIGHT_BLUE)).isEqualTo("{bright_blue}");
        assertThat(BuuiMarkup.colorCloseTag(AnsiColor.BRIGHT_BLUE)).isEqualTo("{/bright_blue}");
        assertThat(BuuiMarkup.resetTag()).isEqualTo("{/}");
    }
}

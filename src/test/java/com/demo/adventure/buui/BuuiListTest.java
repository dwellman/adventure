package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class BuuiListTest {

    @Test
    void renderFormatsTitleAndOptions() {
        String output = BuuiList.render(
                "Next steps:",
                List.of("alpha beta gamma delta", "one two three four five"),
                20,
                0
        );

        String expected = "Next steps:\n\n"
                + "  1. alpha beta\n"
                + "     gamma delta\n"
                + "  2. one two three\n"
                + "     four five";

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void renderHandlesMissingTitleOrOptions() {
        assertThat(BuuiList.render(null, List.of(), 20, 0)).isEqualTo("");
        assertThat(BuuiList.render("Next steps:", null, 20, 0)).isEqualTo("Next steps:");
    }
}

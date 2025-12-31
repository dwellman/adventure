package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererComprehensiveTest {

    @Test
    void rendersComprehensiveMarkdownStyles() {
        AnsiStyle.setEnabledOverride(true);
        try {
            String markdown = """
                    # Heading One
                    ## Heading Two
                    ### Heading Three
                    #### Heading Four

                    Paragraph with **bold** and _italic_ and `inline` plus green.

                    - Bullet item
                    1. Ordered item

                    > Quoted line

                    ```
                    fenced code
                    ```

                    ***

                    | Head | Val |
                    | --- | --- |
                    | Cell | green |
                    """;

            String rendered = MarkdownRenderer.render(markdown, 80, 0);
            String plain = AnsiStyle.strip(rendered);

            assertThat(AnsiStyle.containsAnsi(rendered)).isTrue();
            assertThat(plain).contains("Heading One");
            assertThat(plain).contains("Heading Two");
            assertThat(plain).contains("Heading Three");
            assertThat(plain).contains("Heading Four");
            assertThat(plain).contains("bold");
            assertThat(plain).contains("italic");
            assertThat(plain).contains("inline");
            assertThat(plain).contains("Bullet item");
            assertThat(plain).contains("Ordered item");
            assertThat(plain).contains("Quoted line");
            assertThat(plain).contains("fenced code");
            assertThat(plain).contains("• • •");
            assertThat(plain).contains("Head");
            assertThat(plain).contains("Val");
            assertThat(plain).contains("Cell");
            assertThat(plain).contains("green");
            assertThat(plain).doesNotContain("```");

            assertThat(rendered).contains(AnsiStyle.sequence(true, false, AnsiColor.BRIGHT_BLUE) + "Heading One");
            assertThat(rendered).contains(AnsiStyle.sequence(true, false, AnsiColor.BRIGHT_CYAN) + "Heading Two");
            assertThat(rendered).contains(AnsiStyle.sequence(true, false, AnsiColor.BRIGHT_MAGENTA) + "Heading Three");
            assertThat(rendered).contains(AnsiStyle.sequence(true, false, AnsiColor.BRIGHT_WHITE) + "Heading Four");
            assertThat(rendered).contains(AnsiStyle.sequence(false, false, AnsiColor.BRIGHT_CYAN) + ListRenderer.BULLET);
            assertThat(rendered).contains(AnsiStyle.sequence(false, false, AnsiColor.BRIGHT_CYAN) + "1.");
            assertThat(rendered).contains(AnsiStyle.sequence(false, true, AnsiColor.BRIGHT_BLACK) + "Quoted line");
            assertThat(rendered).contains(AnsiStyle.sequence(false, false, AnsiColor.BRIGHT_BLACK) + "fenced code");
            assertThat(rendered).contains(AnsiStyle.sequence(false, false, AnsiColor.BRIGHT_BLACK) + "inline");
            assertThat(rendered).contains(AnsiStyle.sequence(false, false, AnsiColor.BRIGHT_BLACK) + "• • •");
            assertThat(rendered).contains(AnsiStyle.sequence(true, false, null) + "bold");
            assertThat(rendered).contains(AnsiStyle.sequence(false, true, null) + "italic");
            assertThat(rendered).contains(AnsiStyle.sequence(true, false, AnsiColor.BRIGHT_WHITE) + "Head");
            assertThat(rendered).contains(AnsiStyle.sequence(true, false, AnsiColor.BRIGHT_WHITE) + "Val");
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }
}

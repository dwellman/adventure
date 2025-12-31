package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownRendererTest {

    @Test
    void rendersMarkdownListWithWrapping() {
        String markdown = "- alpha beta gamma";
        String rendered = MarkdownRenderer.render(markdown, 10, 0);

        assertThat(AnsiStyle.strip(rendered)).isEqualTo("\u2022 alpha\n  beta\n  gamma");
    }

    @Test
    void rendersMarkdownTableWithBuuiTable() {
        String markdown = """
                | Name | Value |
                | --- | --- |
                | Alpha | Beta |
                """;

        String rendered = MarkdownRenderer.render(markdown, 80, 0).trim();
        Table expectedTable = Table.fromLists(
                List.of("Name", "Value"),
                List.of(List.of("Alpha", "Beta"))
        );
        String expected = new AsciiRenderer().render(expectedTable).trim();

        assertThat(AnsiStyle.strip(rendered)).isEqualTo(expected);
    }

    @Test
    void addsBlankLineAfterParagraphBeforeList() {
        String markdown = "Alpha beta.\n- item";

        String rendered = MarkdownRenderer.render(markdown, 40, 0);

        assertThat(AnsiStyle.strip(rendered)).isEqualTo("Alpha beta.\n\n\u2022 item");
    }

    @Test
    void scannerDetectsMarkdownSignals() {
        assertThat(MarkdownScanner.hasMarkup("Plain text")).isFalse();
        assertThat(MarkdownScanner.hasMarkup("# Title")).isTrue();
        assertThat(MarkdownScanner.hasMarkup("- item")).isTrue();
        assertThat(MarkdownScanner.hasMarkup("1. item")).isTrue();
        assertThat(MarkdownScanner.hasMarkup("```\ncode\n```")).isTrue();
        assertThat(MarkdownScanner.hasMarkup("| a | b |\n| --- | --- |")).isTrue();
        assertThat(MarkdownScanner.hasMarkup("***")).isTrue();
        assertThat(MarkdownScanner.hasMarkup("This is **bold**.")).isTrue();
        assertThat(MarkdownScanner.hasMarkup("{red}Alert{/}")).isTrue();
        assertThat(MarkdownScanner.hasMarkup("> Quote")).isTrue();
    }

    @Test
    void rendersFencedBlocksAndStripsMarkers() {
        String markdown = """
                ```
                code line
                ```
                This is **bold** and `code`.
                """;

        String rendered = MarkdownRenderer.render(markdown, 40, 0);

        assertThat(rendered).contains("code line");
        assertThat(rendered).doesNotContain("```");
        assertThat(AnsiStyle.strip(rendered)).contains("This is bold and code.");
    }

    @Test
    void rendersInlineFormatting() {
        AnsiStyle.setEnabledOverride(true);
        try {
            String markdown = "This is **bold** and _italic_.";
            String rendered = MarkdownRenderer.render(markdown, 60, 0);

            assertThat(AnsiStyle.strip(rendered)).contains("This is bold and italic.");
            assertThat(AnsiStyle.containsAnsi(rendered)).isTrue();
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }

    @Test
    void rejectsInlineColorTags() {
        assertThatThrownBy(() -> MarkdownRenderer.render("Alert {red}danger{/}.", 60, 0))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("Inline color tags are not allowed");
    }

    @Test
    void rendersHeadingsAndSceneBreaks() {
        String markdown = "# Heading\n\n***\n\nParagraph.";

        String rendered = MarkdownRenderer.render(markdown, 40, 0);

        assertThat(AnsiStyle.strip(rendered)).contains("Heading");
        assertThat(AnsiStyle.strip(rendered)).contains("\u2022 \u2022 \u2022");
    }

    @Test
    void rendersOrderedLists() {
        String markdown = "1. First\n2. Second";

        String rendered = MarkdownRenderer.render(markdown, 40, 0);

        assertThat(AnsiStyle.strip(rendered)).contains("1. First");
        assertThat(AnsiStyle.strip(rendered)).contains("2. Second");
    }

    @Test
    void rendersBlockquotesAndCodeBlocks() {
        AnsiStyle.setEnabledOverride(true);
        try {
            String markdown = """
                    > quoted text here

                    ```
                    code line
                    ```
                    """;

            String rendered = MarkdownRenderer.render(markdown, 40, 0);

            assertThat(AnsiStyle.strip(rendered)).contains("quoted text here");
            assertThat(AnsiStyle.strip(rendered)).contains("code line");
            assertThat(AnsiStyle.containsAnsi(rendered)).isTrue();
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }

    @Test
    void rendersSectionsWithBlankLineAndFlushExits() {
        String markdown = """
                Beach
                A place.
                Items:
                - Rags
                Exits: EAST
                """;

        String rendered = MarkdownRenderer.render(markdown, 60, 0);

        assertThat(AnsiStyle.strip(rendered).stripTrailing()).isEqualTo(String.join("\n",
                "Beach A place.",
                "",
                "Items:",
                "\u2022 Rags",
                "",
                "Exits: EAST"));
    }
}

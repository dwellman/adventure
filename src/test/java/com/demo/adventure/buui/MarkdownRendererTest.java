package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownRendererTest {

    @Test
    void rendersMarkdownListWithWrapping() {
        String markdown = "- alpha beta gamma";
        String rendered = MarkdownRenderer.render(markdown, 10, 0);

        assertThat(rendered).isEqualTo("\u2022 alpha\n  beta\n  gamma");
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

        assertThat(rendered).isEqualTo(expected);
    }

    @Test
    void addsBlankLineAfterParagraphBeforeList() {
        String markdown = "Alpha beta.\n- item";

        String rendered = MarkdownRenderer.render(markdown, 40, 0);

        assertThat(rendered).isEqualTo("    Alpha beta.\n\n\u2022 item");
    }

    @Test
    void detectsMarkdownSignals() {
        assertThat(MarkdownRenderer.isMarkdown("Plain text")).isFalse();
        assertThat(MarkdownRenderer.isMarkdown("# Title")).isTrue();
        assertThat(MarkdownRenderer.isMarkdown("- item")).isTrue();
        assertThat(MarkdownRenderer.isMarkdown("1. item")).isTrue();
        assertThat(MarkdownRenderer.isMarkdown("```\ncode\n```")).isTrue();
        assertThat(MarkdownRenderer.isMarkdown("| a | b |\n| --- | --- |")).isTrue();
        assertThat(MarkdownRenderer.isMarkdown("***")).isTrue();
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
        assertThat(rendered).contains("This is bold and code.");
    }

    @Test
    void rendersHeadingsAndSceneBreaks() {
        String markdown = "# Heading\n\n***\n\nParagraph.";

        String rendered = MarkdownRenderer.render(markdown, 40, 0);

        assertThat(rendered).contains("Heading");
        assertThat(rendered).contains("\u2022 \u2022 \u2022");
    }

    @Test
    void rendersOrderedLists() {
        String markdown = "1. First\n2. Second";

        String rendered = MarkdownRenderer.render(markdown, 40, 0);

        assertThat(rendered).contains("1. First");
        assertThat(rendered).contains("2. Second");
    }
}

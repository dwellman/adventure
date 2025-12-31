package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownScannerTest {

    @Test
    void scansTableTokens() {
        String markdown = """
                | Name | Value |
                | --- | --- |
                | Alpha | Beta |
                """;

        List<MarkdownToken> tokens = MarkdownScanner.scan(markdown);

        assertThat(tokens).extracting(MarkdownToken::type)
                .containsSequence(
                        MarkdownTokenType.TABLE_HEADER,
                        MarkdownTokenType.TABLE_DIVIDER,
                        MarkdownTokenType.TABLE_ROW
                );
    }

    @Test
    void scansSectionLabelsAndExits() {
        String markdown = """
                Items:
                - Rags
                Exits: EAST
                """;

        List<MarkdownToken> tokens = MarkdownScanner.scan(markdown);

        assertThat(tokens).extracting(MarkdownToken::type)
                .contains(MarkdownTokenType.SECTION_LABEL, MarkdownTokenType.LIST_ITEM, MarkdownTokenType.EXIT_LINE);
    }
}

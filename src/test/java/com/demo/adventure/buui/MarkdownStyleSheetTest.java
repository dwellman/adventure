package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MarkdownStyleSheetTest {

    @Test
    void parseOverridesDefaults() {
        String css = """
                heading-1 { color: red; font-weight: normal; }
                blockquote { font-style: normal; color: none; }
                """;

        MarkdownStyleSheet sheet = MarkdownStyleSheet.parse(css);

        BuuiStyle heading = sheet.headingStyle(1);
        assertThat(heading.bold()).isFalse();
        assertThat(heading.color()).isEqualTo(AnsiColor.RED);

        BuuiStyle quote = sheet.blockquoteStyle();
        assertThat(quote.italic()).isFalse();
        assertThat(quote.color()).isNull();
    }

    @Test
    void parseRejectsUnknownSelectors() {
        String css = "unknown { color: red; }";

        assertThatThrownBy(() -> MarkdownStyleSheet.parse(css))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("Unknown markdown style selector");
    }

    @Test
    void parseRejectsTrailingContent() {
        String css = "heading-1 { color: red; } trailing";

        assertThatThrownBy(() -> MarkdownStyleSheet.parse(css))
                .isInstanceOf(MarkdownValidationException.class)
                .hasMessageContaining("Invalid markdown style syntax");
    }

    @Test
    void plainSheetHasNoFormatting() {
        MarkdownStyleSheet plain = MarkdownStyleSheet.plain();

        assertThat(plain.isPlain()).isTrue();
        assertThat(plain.headingStyle(1).isEmpty()).isTrue();
        assertThat(plain.blockquoteStyle().isEmpty()).isTrue();
        assertThat(plain.strongStyle().isEmpty()).isTrue();
    }
}

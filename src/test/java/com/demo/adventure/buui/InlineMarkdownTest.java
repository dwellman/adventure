package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InlineMarkdownTest {

    @Test
    void formatAddsAnsiWhenEnabled() {
        AnsiStyle.setEnabledOverride(true);
        try {
            String input = "**Bold** _ital_ Alert";
            String formatted = InlineMarkdown.format(input);

            assertThat(AnsiStyle.containsAnsi(formatted)).isTrue();
            assertThat(AnsiStyle.strip(formatted)).isEqualTo("Bold ital Alert");
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }

    @Test
    void formatStripsMarkersWhenDisabled() {
        AnsiStyle.setEnabledOverride(false);
        try {
            String input = "**Bold** _ital_ Alert";
            String formatted = InlineMarkdown.format(input);

            assertThat(formatted).isEqualTo("Bold ital Alert");
            assertThat(AnsiStyle.containsAnsi(formatted)).isFalse();
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }

    @Test
    void formatStylesInlineCode() {
        AnsiStyle.setEnabledOverride(true);
        try {
            String formatted = InlineMarkdown.format("Use `code` here.");

            assertThat(AnsiStyle.strip(formatted)).isEqualTo("Use code here.");
            assertThat(AnsiStyle.containsAnsi(formatted)).isTrue();
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }

    @Test
    void formatAppliesBaseStyle() {
        AnsiStyle.setEnabledOverride(true);
        try {
            BuuiStyle base = new BuuiStyle(true, false, AnsiColor.BRIGHT_BLUE);
            String formatted = InlineMarkdown.format("Title", base);

            assertThat(AnsiStyle.strip(formatted)).isEqualTo("Title");
            assertThat(AnsiStyle.containsAnsi(formatted)).isTrue();
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }

    @Test
    void formatStripsMarkersInsideBaseStyles() {
        AnsiStyle.setEnabledOverride(true);
        try {
            BuuiStyle base = new BuuiStyle(true, true, AnsiColor.BRIGHT_BLACK);
            String formatted = InlineMarkdown.format("Use _italics_ and **bold** here.", base);

            assertThat(AnsiStyle.strip(formatted)).isEqualTo("Use italics and bold here.");
            assertThat(AnsiStyle.containsAnsi(formatted)).isTrue();
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }

    @Test
    void formatStripsMarkersWhenPlainStyleSheet() {
        AnsiStyle.setEnabledOverride(true);
        try {
            MarkdownStyleSheet plain = MarkdownStyleSheet.plain();
            assertThatThrownBy(() -> InlineMarkdown.format("**Bold** _ital_ {red}Alert{/}.", BuuiStyle.none(), plain))
                    .isInstanceOf(MarkdownValidationException.class)
                    .hasMessageContaining("Inline color tags are not allowed");
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }

    @Test
    void formatStripsInlineColorTagsWhenStyled() {
        AnsiStyle.setEnabledOverride(true);
        try {
            MarkdownStyleSheet styles = MarkdownStyleSheet.defaults();
            assertThatThrownBy(() -> InlineMarkdown.format("{red}Alert{/}.", BuuiStyle.none(), styles))
                    .isInstanceOf(MarkdownValidationException.class)
                    .hasMessageContaining("Inline color tags are not allowed");
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }
}

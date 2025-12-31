package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextUtilsTest {

    @Test
    void wrapHandlesNullAndZeroWidth() {
        assertThat(TextUtils.wrap(null, 10)).containsExactly("");
        assertThat(TextUtils.wrap("alpha", 0)).containsExactly("");
    }

    @Test
    void wrapSplitsLongWordsAndLines() {
        assertThat(TextUtils.wrap("alpha", 2)).containsExactly("al", "ph", "a");
        assertThat(TextUtils.wrap("alpha beta gamma", 10)).containsExactly("alpha beta", "gamma");
    }

    @Test
    void truncateHonorsBounds() {
        assertThat(TextUtils.truncate(null, 5)).isEqualTo("");
        assertThat(TextUtils.truncate("alpha", 0)).isEqualTo("");
        assertThat(TextUtils.truncate("alpha", 3)).isEqualTo("alp");
        assertThat(TextUtils.truncate("alpha", 4)).isEqualTo("a...");
        assertThat(TextUtils.truncate("alpha", 10)).isEqualTo("alpha");
    }

    @Test
    void padAlignsText() {
        assertThat(TextUtils.pad("a", 3, Alignment.LEFT)).isEqualTo("a  ");
        assertThat(TextUtils.pad("a", 3, Alignment.RIGHT)).isEqualTo("  a");
        assertThat(TextUtils.pad("a", 3, Alignment.CENTER)).isEqualTo(" a ");
    }

    @Test
    void wrapAndTruncateIgnoreAnsiCodes() {
        AnsiStyle.setEnabledOverride(true);
        try {
            String styled = InlineMarkdown.format("**alpha beta**");
            List<String> lines = TextUtils.wrap(styled, 5);

            assertThat(TextUtils.visibleLength(styled)).isEqualTo("alpha beta".length());
            assertThat(lines.stream().map(AnsiStyle::strip).toList())
                    .containsExactly("alpha", "beta");

            String truncated = TextUtils.truncate(styled, 3);
            assertThat(AnsiStyle.strip(truncated)).isEqualTo("alp");
        } finally {
            AnsiStyle.setEnabledOverride(null);
        }
    }
}

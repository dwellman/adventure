package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AsciiRendererTest {

    @Test
    void renderWithTitleUsesFullWidthLine() {
        RenderStyle style = RenderStyle.defaults().withBorder(BorderCharacters.boxDrawing());
        Table table = Table.builder()
                .title("Title")
                .style(style)
                .addColumn(Column.builder("Name").build())
                .addColumn(Column.builder("Value").build())
                .addRow("Alpha", "Beta")
                .build();

        String[] lines = new AsciiRenderer().render(table).split("\\R");

        assertThat(lines[0]).startsWith(style.border().topLeft);
        assertThat(lines[0]).doesNotContain(style.border().headerSeparator);
        assertThat(lines[1]).contains("Title");
    }

    @Test
    void renderWithoutTitleUsesHeaderSeparators() {
        RenderStyle style = RenderStyle.defaults()
                .withBorder(BorderCharacters.boxDrawing())
                .withShowTitle(false);
        Table table = Table.builder()
                .style(style)
                .addColumn(Column.builder("Name").build())
                .addColumn(Column.builder("Value").build())
                .addRow("Alpha", "Beta")
                .build();

        String[] lines = new AsciiRenderer().render(table).split("\\R");

        assertThat(lines[0]).contains(style.border().headerSeparator);
    }

    @Test
    void renderShowsRowNumbersAndTruncatesWhenWrapDisabled() {
        RenderStyle style = RenderStyle.defaults()
                .withBorder(BorderCharacters.ascii())
                .withShowRowNumbers(true)
                .withWrapCells(false)
                .withShowRowSeparators(false);
        Table table = Table.builder()
                .style(style)
                .addColumn(Column.builder("Name").width(4).wrap(true).build())
                .addRow("alphabet")
                .addRow("beta")
                .build();

        String rendered = new AsciiRenderer().render(table);

        assertThat(rendered).contains("| 1 ");
        assertThat(rendered).contains("a...");
    }
}

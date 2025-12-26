package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BuuiMenuTest {

    @Test
    void buildsMenuTableWithDefaults() {
        BuuiMenu menu = BuuiMenu.builder()
                .title("Menu")
                .itemHeader("Game")
                .addItem("1", "Island", "Escape room adventure.")
                .addItem("q", "Quit", "")
                .build();

        Table table = menu.buildTable();

        assertThat(table.title()).isEqualTo("Menu");
        assertThat(table.columns()).hasSize(3);
        assertThat(table.columns().get(0).title()).isEqualTo("#");
        assertThat(table.columns().get(0).minWidth()).isEqualTo(3);
        assertThat(table.columns().get(0).maxWidth()).isEqualTo(3);
        assertThat(table.rows()).hasSize(2);
        assertThat(table.rows().get(0).cells().get(0).value()).isEqualTo("1");
        assertThat(table.rows().get(0).cells().get(1).value()).isEqualTo("Island");
    }

    @Test
    void expandsKeyWidthWhenKeysAreLonger() {
        BuuiMenu menu = BuuiMenu.builder()
                .addItem("LONG", "Alpha", "Beta")
                .build();

        Table table = menu.buildTable();

        assertThat(table.columns().get(0).minWidth()).isEqualTo(4);
        assertThat(table.columns().get(0).maxWidth()).isEqualTo(4);
    }

    @Test
    void buildsPromptWithDefaultExitLabels() {
        assertThat(BuuiMenu.prompt("game", 3, "q"))
                .isEqualTo("Select a game (1-3, or q to quit): ");
        assertThat(BuuiMenu.prompt("mini", 2, "b"))
                .isEqualTo("Select a mini (1-2, or b to go back): ");
    }

    @Test
    void promptHandlesMissingInputs() {
        assertThat(BuuiMenu.prompt(null, -1, null, null))
                .isEqualTo("Select a item (1-0, or q to quit): ");
        assertThat(BuuiMenu.prompt("choice", 5, "x"))
                .isEqualTo("Select a choice (1-5, or x to exit): ");
    }

    @Test
    void builderUsesDefaultsWhenBlank() {
        BuuiMenu menu = BuuiMenu.builder()
                .title(null)
                .keyHeader(" ")
                .itemHeader("")
                .descriptionHeader(null)
                .minKeyWidth(-4)
                .addItem(null, null, null)
                .build();

        Table table = menu.buildTable();

        assertThat(table.columns().get(0).title()).isEqualTo("#");
        assertThat(table.columns().get(1).title()).isEqualTo("Item");
        assertThat(table.columns().get(2).title()).isEqualTo("Description");
        assertThat(table.rows().get(0).cells().get(0).value()).isEqualTo("");
    }
}

package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranslatorPromptCoverageTest {

    @Test
    void promptIncludesVisibleListsAndCommandSurface() {
        TranslatorService service = new TranslatorService(true, "test");
        String prompt = buildPrompt(
                service,
                "attack goblin",
                List.of("Lantern"),
                List.of("Stick", "Flint"),
                List.of("Coin"),
                "Cave\nExits: north"
        );

        assertThat(prompt).contains("VISIBLE_FIXTURES: Lantern");
        assertThat(prompt).contains("VISIBLE_ITEMS: Stick, Flint");
        assertThat(prompt).contains("INVENTORY_ITEMS: Coin");
        assertThat(prompt).contains("PLAYER_TEXT: attack goblin");

        assertThat(prompt).contains("attack <target>");
        assertThat(prompt).contains("talk <actor>");
        assertThat(prompt).contains("run away");
        assertThat(prompt).contains("inspect <thing>");
        assertThat(prompt).contains("open <thing>");
        assertThat(prompt).contains("use <thing>");
        assertThat(prompt).contains("put <item> <preposition> <object>");
        assertThat(prompt).contains("dice(<sides>,<target>)");
        assertThat(prompt).contains("where am i");
        assertThat(prompt).contains("shares a word");
    }

    @Test
    void promptSanitizesInputsAndDefaultsMissingLists() {
        TranslatorService service = new TranslatorService(true, "test");
        String prompt = buildPrompt(
                service,
                "take torch\nnow",
                null,
                List.of(),
                null,
                ""
        );

        assertThat(prompt).contains("VISIBLE_FIXTURES: (none)");
        assertThat(prompt).contains("VISIBLE_ITEMS: (none)");
        assertThat(prompt).contains("INVENTORY_ITEMS: (none)");
        assertThat(prompt).contains("PLAYER_TEXT: take torch now");
        assertThat(prompt).contains("SCENE_CONTEXT (last engine output / narration, may be \"(none)\"): (none)");
    }

    private String buildPrompt(
            TranslatorService service,
            String playerText,
            List<String> fixtures,
            List<String> visibleItems,
            List<String> inventoryItems,
            String sceneContext
    ) {
        try {
            Method m = TranslatorService.class.getDeclaredMethod(
                    "buildTranslatorPrompt",
                    String.class,
                    List.class,
                    List.class,
                    List.class,
                    String.class
            );
            m.setAccessible(true);
            return (String) m.invoke(service, playerText, fixtures, visibleItems, inventoryItems, sceneContext);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

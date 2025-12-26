package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranslatorPromptGoldenTest {

    @Test
    void promptMatchesGolden() throws Exception {
        TranslatorService service = new TranslatorService(true, "test");
        String prompt = buildPrompt(
                service,
                "attack goblin",
                List.of("Lantern", "Desk"),
                List.of("Stick", "Flint"),
                List.of("Coin"),
                "Cave\nExits: north"
        );

        Path path = Path.of("src/test/resources/ai/translator/prompt-golden.txt");
        String expected = Files.readString(path, StandardCharsets.UTF_8);
        assertThat(prompt).isEqualTo(expected);
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

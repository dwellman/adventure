package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranslatorServiceTest {

    private final TranslatorService service = new TranslatorService(true, "test");

    @Test
    void parsesCommandWhenFieldsMatchContract() {
        TranslatorService.TranslationResult result = parse("move NORTH");

        assertThat(result.type()).isEqualTo(TranslatorService.TranslationResult.Type.COMMAND);
        assertThat(result.command()).isEqualTo("move NORTH");
    }

    @Test
    void rejectsMissingCommand() {
        TranslatorService.TranslationResult result = parse("");

        assertThat(result.type()).isEqualTo(TranslatorService.TranslationResult.Type.ERROR);
        assertThat(result.error()).isEqualTo("Missing command");
    }

    @Test
    void rejectsMultilineOutputs() {
        TranslatorService.TranslationResult result = parse("look\nEXTRA");

        assertThat(result.type()).isEqualTo(TranslatorService.TranslationResult.Type.ERROR);
        assertThat(result.error()).isEqualTo("Translator emitted multiple lines");
    }

    @Test
    void parsesEmoteOutput() {
        TranslatorService.TranslationResult result = parse("EMOTE: Do a little dance.");

        assertThat(result.type()).isEqualTo(TranslatorService.TranslationResult.Type.EMOTE);
        assertThat(result.command()).isEqualTo("EMOTE: Do a little dance.");
    }

    @Test
    void rejectsEmptyEmote() {
        TranslatorService.TranslationResult result = parse("EMOTE:");

        assertThat(result.type()).isEqualTo(TranslatorService.TranslationResult.Type.ERROR);
        assertThat(result.error()).isEqualTo("Missing emote");
    }

    @Test
    void translatesWhereAmIToLookWithoutLlm() {
        TranslatorService service = new TranslatorService(true, "test", (apiKey, prompt) -> {
            throw new IllegalStateException("LLM should not be called");
        });

        TranslatorService.TranslationResult result = service.translate(
                "where am i?",
                List.of(),
                List.of(),
                List.of(),
                ""
        );

        assertThat(result.type()).isEqualTo(TranslatorService.TranslationResult.Type.COMMAND);
        assertThat(result.command()).isEqualTo("look");
    }

    @Test
    void translatesWhoIsToLookWithoutLlm() {
        TranslatorService service = new TranslatorService(true, "test", (apiKey, prompt) -> {
            throw new IllegalStateException("LLM should not be called");
        });

        TranslatorService.TranslationResult result = service.translate(
                "who is elias crane?",
                List.of(),
                List.of(),
                List.of(),
                ""
        );

        assertThat(result.type()).isEqualTo(TranslatorService.TranslationResult.Type.COMMAND);
        assertThat(result.command()).isEqualTo("look elias crane");
    }

    private TranslatorService.TranslationResult parse(String raw) {
        try {
            Method m = TranslatorService.class.getDeclaredMethod("parseTranslationOutput", String.class);
            m.setAccessible(true);
            return (TranslatorService.TranslationResult) m.invoke(service, raw);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}

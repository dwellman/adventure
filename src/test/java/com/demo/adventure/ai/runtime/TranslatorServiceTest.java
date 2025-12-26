package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

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

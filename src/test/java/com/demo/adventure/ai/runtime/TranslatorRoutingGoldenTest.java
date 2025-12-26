package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranslatorRoutingGoldenTest {

    private final TranslatorService service = new TranslatorService(true, "test");

    @Test
    void parsesGoldenTranslationOutputs() throws Exception {
        Path path = Path.of("src/test/resources/ai/translator/routing-golden.txt");
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String[] parts = line.split("\\|", 2);
            assertThat(parts).hasSize(2);
            TranslatorService.TranslationResult result = parse(parts[1]);
            assertThat(result.type()).isEqualTo(parseType(parts[0]));
            switch (result.type()) {
                case COMMAND -> assertThat(result.command()).isEqualTo(parts[1]);
                case ERROR -> assertThat(result.error()).isNotBlank();
            }
        }
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

    private TranslatorService.TranslationResult.Type parseType(String raw) {
        return TranslatorService.TranslationResult.Type.valueOf(raw.trim().toUpperCase());
    }
}

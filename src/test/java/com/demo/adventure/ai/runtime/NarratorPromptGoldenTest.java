package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class NarratorPromptGoldenTest {

    @Test
    void enginePromptMatchesGolden() throws Exception {
        String prompt = NarratorPromptBuilder.buildEngine(
                "look",
                "look",
                "Garden\nExits: east",
                "Garden\nExits: east",
                "Backstory line"
        );

        Path path = Path.of("src/test/resources/ai/narrator/prompt-engine-golden.txt");
        String expected = Files.readString(path, StandardCharsets.UTF_8);
        assertThat(prompt).isEqualTo(expected);
    }

    @Test
    void snapshotPromptMatchesGolden() throws Exception {
        String prompt = NarratorPromptBuilder.buildSnapshot(
                "look",
                "look",
                "Garden\nExits: east",
                "A breeze stirs.",
                "Backstory line"
        );

        Path path = Path.of("src/test/resources/ai/narrator/prompt-snapshot-golden.txt");
        String expected = Files.readString(path, StandardCharsets.UTF_8);
        assertThat(prompt).isEqualTo(expected);
    }
}

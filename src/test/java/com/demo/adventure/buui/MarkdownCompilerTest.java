package com.demo.adventure.buui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownCompilerTest {

    @TempDir
    Path tempDir;

    @Test
    void compilesMarkdownFromFileWithTokens() throws Exception {
        Path file = tempDir.resolve("sample.md");
        Files.writeString(file, """
                # Title

                - One
                """, StandardCharsets.UTF_8);

        MarkdownDocument doc = MarkdownCompiler.compile(file, 40, 0);

        assertThat(doc.tokens()).extracting(MarkdownToken::type)
                .contains(MarkdownTokenType.HEADING, MarkdownTokenType.LIST_ITEM);
        assertThat(AnsiStyle.strip(doc.render())).contains("Title");
        assertThat(AnsiStyle.strip(doc.render())).contains("\u2022 One");
    }
}

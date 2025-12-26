package com.demo.adventure.authoring.cli;

import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchitectCliTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void printsUsageWhenArgsMissing() {
        console.reset();
        int code = new ArchitectCli().run(new String[]{});

        assertThat(code).isEqualTo(1);
        assertThat(console.output()).contains("Architect CLI");
    }

    @Test
    void parsesPlotFileAndWritesYaml() throws Exception {
        Path tempDir = Files.createTempDirectory("architect");
        Path input = tempDir.resolve("plots.md");
        Files.writeString(input, "P001 **Start** — A start plot\nP002 **End** — The end\n");
        Path output = tempDir.resolve("game.yaml");

        int code = new ArchitectCli().run(new String[]{
                "--in", input.toString(),
                "--out", output.toString(),
                "--skip-validate"
        });

        assertThat(code).isEqualTo(0);
        assertThat(Files.exists(output)).isTrue();
    }

    @Test
    void runThrowsWhenStartKeyMissing() throws Exception {
        Path tempDir = Files.createTempDirectory("architect");
        Path input = tempDir.resolve("plots.md");
        Files.writeString(input, "P001 **Start** — A start plot\n");

        assertThatThrownBy(() -> new ArchitectCli().run(new String[]{
                "--in", input.toString(),
                "--start", "P999",
                "--skip-validate"
        })).isInstanceOf(IllegalArgumentException.class);
    }
}

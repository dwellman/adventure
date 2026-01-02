package com.demo.adventure.authoring.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Files;
import java.nio.file.Path;

import com.demo.adventure.test.ConsoleCaptureExtension;
import static org.assertj.core.api.Assertions.assertThat;

class GdlCliSmokeTest {

    private record CapturedOutput(String out, String err) { }
    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void gameBuilderCliAcceptsGdlInput() throws Exception {
        Path input = gdlInput();
        CapturedOutput output = captureOutput(() -> {
            int code = new GameBuilderCli().run(new String[]{
                    input.toString(),
                    "--stdout",
                    "--skip-validate"
            });
            assertThat(code).isEqualTo(0);
        });

        assertThat(output.out()).contains("plots:");
        assertThat(output.err().trim()).isEmpty();
    }

    @Test
    void gameStructExporterAcceptsGdlInput() throws Exception {
        Path input = gdlInput();
        Files.createDirectories(Path.of("target"));
        Path outDir = Files.createTempDirectory(Path.of("target"), "gdl-export-");

        CapturedOutput output = captureOutput(() -> {
            int code = new GameStructExporter().run(new String[]{
                    "--in", input.toString(),
                    "--out", outDir.toString(),
                    "--id", "gdl-demo",
                    "--title", "GDL Demo"
            });
            assertThat(code).isEqualTo(0);
        });

        assertThat(Files.exists(outDir.resolve("game.yaml"))).isTrue();
        assertThat(Files.exists(outDir.resolve("world/map.yaml"))).isTrue();
        assertThat(output.err().trim()).isEmpty();
    }

    @Test
    void worldFingerprintDumpAcceptsGdlInput() throws Exception {
        Path input = gdlInput();
        CapturedOutput output = captureOutput(() -> WorldFingerprintDump.main(new String[]{input.toString()}));

        assertThat(output.out()).isNotBlank();
        assertThat(output.err().trim()).isEmpty();
    }

    @Test
    void worldIdDumpAcceptsGdlInput() throws Exception {
        Path input = gdlInput();
        CapturedOutput output = captureOutput(() -> WorldIdDump.main(new String[]{input.toString()}));

        assertThat(output.out()).contains("Plots:");
        assertThat(output.out()).contains("Items:");
        assertThat(output.err().trim()).isEmpty();
    }

    private static Path gdlInput() {
        Path input = Path.of("src/test/resources/games/gdl-demo/game.gdl");
        assertThat(Files.exists(input)).isTrue();
        return input;
    }

    private CapturedOutput captureOutput(ThrowingRunnable action) throws Exception {
        console.reset();
        action.run();
        return new CapturedOutput(console.output(), console.error());
    }
}

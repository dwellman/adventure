package com.demo.adventure.authoring.cli;

import com.demo.adventure.authoring.gardener.WorldFingerprint;
import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GardenerCliTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void rejectsMismatchedFingerprint() throws Exception {
        BuuiConsole.setOutputSuppressed(false);
        GameSave sample = sampleGame();
        Path tempDir = Files.createTempDirectory("gardener");
        Path input = tempDir.resolve("game.yaml");
        GameSaveYamlWriter.write(sample, input);
        GameSave loaded = GameSaveYamlLoader.load(input);
        Path patch = tempDir.resolve("patch.yaml");
        Files.writeString(patch, patchYaml(loaded.plots().get(0).plotId(), "wrong"));
        Path output = tempDir.resolve("out.yaml");

        invokeRun(input, patch, output, false, false);

        assertThat(Files.exists(output)).isFalse();
    }

    @Test
    void rejectsMissingPlotCoverage() throws Exception {
        BuuiConsole.setOutputSuppressed(false);
        GameSave sample = sampleGame();
        Path tempDir = Files.createTempDirectory("gardener");
        Path input = tempDir.resolve("game.yaml");
        GameSaveYamlWriter.write(sample, input);
        GameSave loaded = GameSaveYamlLoader.load(input);
        String fingerprint = WorldFingerprint.fingerprint(loaded);
        Path patch = tempDir.resolve("patch.yaml");
        Files.writeString(patch, patchYaml(loaded.plots().get(0).plotId(), fingerprint));
        Path output = tempDir.resolve("out.yaml");

        invokeRun(input, patch, output, false, false);

        assertThat(Files.exists(output)).isFalse();
    }

    @Test
    void validateOnlyPrintsSummary() throws Exception {
        BuuiConsole.setOutputSuppressed(false);
        GameSave sample = sampleGame();
        Path tempDir = Files.createTempDirectory("gardener");
        Path input = tempDir.resolve("game.yaml");
        GameSaveYamlWriter.write(sample, input);
        GameSave loaded = GameSaveYamlLoader.load(input);
        String fingerprint = WorldFingerprint.fingerprint(loaded);
        Path patch = tempDir.resolve("patch.yaml");
        Files.writeString(patch, patchYamlWithPlots(loaded.plots().get(0).plotId(), loaded.plots().get(1).plotId(), fingerprint));

        console.reset();
        invokeRun(input, patch, tempDir.resolve("out.yaml"), true, false);

        assertThat(console.output()).contains("Validation OK");
    }

    @Test
    void appliesPatchAndWritesOutput() throws Exception {
        BuuiConsole.setOutputSuppressed(false);
        GameSave sample = sampleGame();
        Path tempDir = Files.createTempDirectory("gardener");
        Path input = tempDir.resolve("game.yaml");
        GameSaveYamlWriter.write(sample, input);
        GameSave loaded = GameSaveYamlLoader.load(input);
        String fingerprint = WorldFingerprint.fingerprint(loaded);
        Path patch = tempDir.resolve("patch.yaml");
        Files.writeString(patch, patchYamlWithPlots(loaded.plots().get(0).plotId(), loaded.plots().get(1).plotId(), fingerprint));
        Path output = tempDir.resolve("out.yaml");

        invokeRun(input, patch, output, false, false);

        assertThat(Files.exists(output)).isTrue();
    }

    private void invokeRun(Path input, Path patch, Path output, boolean validateOnly, boolean forceGdl) throws Exception {
        Method method = GardenerCli.class.getDeclaredMethod("run", Path.class, Path.class, Path.class, boolean.class, boolean.class);
        method.setAccessible(true);
        method.invoke(new GardenerCli(), input, patch, output, validateOnly, forceGdl);
    }

    private GameSave sampleGame() {
        UUID plotA = UUID.randomUUID();
        UUID plotB = UUID.randomUUID();
        WorldRecipe.PlotSpec a = new WorldRecipe.PlotSpec(plotA, "Plot A", "TEST", 0, 0, "Desc A");
        WorldRecipe.PlotSpec b = new WorldRecipe.PlotSpec(plotB, "Plot B", "TEST", 1, 0, "Desc B");
        WorldRecipe.FixtureSpec fixture = new WorldRecipe.FixtureSpec(UUID.randomUUID(), "Fixture", "Fixture desc", plotA, true, Map.of());
        GameSave.ItemRecipe item = new GameSave.ItemRecipe(UUID.randomUUID(), "Item", "Item desc", plotA, true, false, "true", 1.0, 1.0, 0, 0, 0, 0, Map.of());
        GameSave.ActorRecipe actor = new GameSave.ActorRecipe(UUID.randomUUID(), "Actor", "Actor desc", plotA, true, List.of("Skill"), null, null, Map.of());
        return new GameSave(1L, plotA, "preamble", List.of(a, b), List.of(), List.of(fixture), List.of(item), List.of(actor));
    }

    private String patchYaml(UUID plotId, String fingerprint) {
        return "seed: 1\n" +
                "worldFingerprint: " + fingerprint + "\n" +
                "plots:\n" +
                "  " + plotId + ":\n" +
                "    displayTitle: Patched\n" +
                "    description: Patched description\n";
    }

    private String patchYamlWithPlots(UUID plotA, UUID plotB, String fingerprint) {
        return "seed: 1\n" +
                "worldFingerprint: " + fingerprint + "\n" +
                "plots:\n" +
                "  " + plotA + ":\n" +
                "    displayTitle: Patched A\n" +
                "    description: Patched A desc\n" +
                "  " + plotB + ":\n" +
                "    displayTitle: Patched B\n" +
                "    description: Patched B desc\n";
    }

}

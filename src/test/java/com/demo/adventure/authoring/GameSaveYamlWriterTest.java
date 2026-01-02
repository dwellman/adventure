package com.demo.adventure.authoring;

import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameSaveYamlWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesClueMansionYamlDeterministically() throws Exception {
        Path canonical = Path.of("src/main/resources/cookbook/gardened-mansion.yaml");
        assertThat(canonical).exists();
        GameSave canonicalSave = GameSaveYamlLoader.load(canonical);

        Path file = tempDir.resolve("clue-mansion.yaml");
        GameSaveYamlWriter.write(canonicalSave, file);
        GameSave roundTrip = GameSaveYamlLoader.load(file);

        assertThat(normalize(roundTrip)).isEqualTo(normalize(canonicalSave));
    }

    @Test
    void loadsCanonicalYamlAndMatchesRecipes() throws Exception {
        Path canonical = Path.of("src/main/resources/cookbook/gardened-mansion.yaml");
        GameSave loaded = GameSaveYamlLoader.load(canonical);

        Path file = tempDir.resolve("clue-roundtrip.yaml");
        GameSaveYamlWriter.write(loaded, file);
        GameSave roundTrip = GameSaveYamlLoader.load(file);

        assertThat(normalize(roundTrip)).isEqualTo(normalize(loaded));
    }

    private static GameSave normalize(GameSave save) {
        Comparator<WorldRecipe.PlotSpec> plotCmp = Comparator.comparing(WorldRecipe.PlotSpec::name);
        Comparator<WorldRecipe.GateSpec> gateCmp = Comparator.comparing(WorldRecipe.GateSpec::label);
        Comparator<WorldRecipe.FixtureSpec> fixtureCmp = Comparator.comparing(WorldRecipe.FixtureSpec::name);
        Comparator<GameSave.ItemRecipe> itemCmp = Comparator.comparing(GameSave.ItemRecipe::name);
        Comparator<GameSave.ActorRecipe> actorCmp = Comparator.comparing(GameSave.ActorRecipe::name);

        List<WorldRecipe.PlotSpec> plots = save.plots().stream()
                .sorted(plotCmp)
                .map(p -> new WorldRecipe.PlotSpec(
                        p.plotId(),
                        p.name(),
                        p.region(),
                        0,
                        0,
                        p.description()))
                .toList();
        List<WorldRecipe.GateSpec> gates = save.gates().stream().sorted(gateCmp).toList();
        List<WorldRecipe.FixtureSpec> fixtures = save.fixtures().stream().sorted(fixtureCmp).toList();
        List<GameSave.ItemRecipe> items = save.items().stream().sorted(itemCmp).toList();
        List<GameSave.ActorRecipe> actors = save.actors().stream().sorted(actorCmp).toList();

        return new GameSave(save.seed(), save.startPlotId(), save.preamble(), plots, gates, fixtures, items, actors);
    }
}

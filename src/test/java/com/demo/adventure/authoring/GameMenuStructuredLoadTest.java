package com.demo.adventure.authoring;

import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GameMenuStructuredLoadTest {

    @Test
    void islandLoads() throws Exception {
        assertStructuredGameLoads("src/main/resources/games/island/game.yaml");
    }

    @Test
    void mansionLoads() throws Exception {
        assertStructuredGameLoads("src/main/resources/games/mansion/game.yaml");
    }

    @Test
    void westernLoads() throws Exception {
        assertStructuredGameLoads("src/main/resources/games/western/game.yaml");
    }

    @Test
    void spy1970Loads() throws Exception {
        assertStructuredGameLoads("src/main/resources/games/spy/game.yaml");
    }

    private static void assertStructuredGameLoads(String path) throws Exception {
        // Pattern: Verification + Grounding
        // - Treat the structured loader as the source of truth and verify it yields a valid, non-empty world.
        GameSave save = StructuredGameSaveLoader.load(Path.of(path));
        assertThat(save).isNotNull();
        assertThat(save.startPlotId()).as("startPlotId for " + path).isNotNull();
        assertThat(save.plots()).isNotEmpty();
    }
}

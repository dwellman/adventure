package com.demo.adventure.engine.flow.loop;

import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.WorldState;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LoopRuntimeTest {

    @Test
    void advancesClockAndTriggersTimeout() throws Exception {
        LoopConfig config = new LoopConfig(true, 2, List.of("Notebook"));
        LoopRuntime runtime = new LoopRuntime(loadIslandSave(), config);
        WorldBuildResult world = runtime.buildWorld();
        KernelRegistry registry = world.registry();

        WorldState worldState = findWorldState(registry);
        assertThat(worldState).isNotNull();
        CellOps.setAmount(worldState, WorldState.TICK_RATE_CELL, 1L);

        LoopResetReason first = runtime.advanceTurn(registry);
        assertThat(first).isNull();
        LoopResetReason second = runtime.advanceTurn(registry);
        assertThat(second).isEqualTo(LoopResetReason.TIMEOUT);
        assertThat(worldState.getCell(WorldState.CLOCK_CELL).getAmount()).isEqualTo(runtime.state().clock());
        assertThat(worldState.getCell(WorldState.TICK_RATE_CELL).getAmount()).isEqualTo(runtime.state().tickRate());
    }

    @Test
    void persistsNotebookDescriptionAcrossReset() throws Exception {
        LoopConfig config = new LoopConfig(true, 3, List.of("Notebook"));
        LoopRuntime runtime = new LoopRuntime(loadIslandSave(), config);
        WorldBuildResult world = runtime.buildWorld();

        Item notebook = findItemByLabel(world.registry(), "Notebook");
        assertThat(notebook).isNotNull();
        notebook.setDescription("Notes from previous loop.");

        LoopResetResult reset = runtime.reset(world.registry(), LoopResetReason.TIMEOUT);
        Item after = findItemByLabel(reset.world().registry(), "Notebook");
        assertThat(after).isNotNull();
        assertThat(after.getDescription()).isEqualTo("Notes from previous loop.");
        assertThat(runtime.state().clock()).isEqualTo(0);
        assertThat(runtime.state().loopCount()).isEqualTo(2);
    }

    @Test
    void syncsTickRateFromWorldState() throws Exception {
        LoopConfig config = new LoopConfig(true, 5, List.of());
        LoopRuntime runtime = new LoopRuntime(loadIslandSave(), config);
        WorldBuildResult world = runtime.buildWorld();
        KernelRegistry registry = world.registry();

        WorldState worldState = findWorldState(registry);
        assertThat(worldState).isNotNull();
        CellOps.setAmount(worldState, WorldState.TICK_RATE_CELL, 2L);

        runtime.advanceTurn(registry);

        assertThat(runtime.state().clock()).isEqualTo(2);
        assertThat(worldState.getCell(WorldState.TICK_RATE_CELL).getAmount()).isEqualTo(2L);
    }

    private static Item findItemByLabel(KernelRegistry registry, String label) {
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> item.getLabel() != null && item.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

    private static WorldState findWorldState(KernelRegistry registry) {
        return registry.getEverything().values().stream()
                .filter(WorldState.class::isInstance)
                .map(WorldState.class::cast)
                .findFirst()
                .orElse(null);
    }

    private static GameSave loadIslandSave() throws Exception {
        return StructuredGameSaveLoader.load(Path.of("src/main/resources/games/island/game.yaml"));
    }
}

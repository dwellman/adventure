package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotKind;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InteractionStateTest {

    @Test
    void emoteCheckSetsAndClearsInteractionState() {
        boolean muted = BuuiConsole.isOutputSuppressed();
        BuuiConsole.setOutputSuppressed(true);
        try {
            GameRuntime runtime = buildRuntime();

            runtime.emote("do a dance to distract the killer");

            GameRuntime.InteractionState state = runtime.interactionState();
            assertThat(state.type()).isEqualTo(GameRuntime.InteractionType.AWAITING_DICE);
            assertThat(state.expectedToken()).isEqualTo("dice(20, 15)");

            runtime.rollDice("dice(20,15)");

            assertThat(runtime.interactionState().type()).isEqualTo(GameRuntime.InteractionType.NONE);
        } finally {
            BuuiConsole.setOutputSuppressed(muted);
        }
    }

    private GameRuntime buildRuntime() {
        KernelRegistry registry = new KernelRegistry();
        UUID plotId = UUID.randomUUID();
        Plot plot = new Plot(plotId, "Hall", "", PlotKind.LAND, null, null, KernelRegistry.MILIARIUM);
        registry.register(plot);

        NarrationService narrationService = new NarrationService(false, "", false);
        SceneNarrator narrator = new SceneNarrator(narrationService);
        GameRuntime runtime = new GameRuntime(narrator, text -> {}, false);
        runtime.configure(
                registry,
                plotId,
                UUID.randomUUID(),
                new ArrayList<>(),
                new HashMap<>(),
                null,
                null,
                Map.<String, CraftingRecipe>of(),
                Map.of()
        );
        return runtime;
    }
}

package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameRuntimeTakeFixtureItemTest {

    @Test
    void takesItemFromOpenFixtureAtPlot() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Study")
                .withDescription("A quiet study.")
                .build();
        registry.register(plot);

        UUID playerId = UUID.randomUUID();
        Actor player = new ActorBuilder()
                .withId(playerId)
                .withLabel("Detective")
                .withDescription("Investigator")
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
        registry.register(player);

        Item drawer = new ItemBuilder()
                .withLabel("Desk Drawer")
                .withDescription("A wooden drawer.")
                .withOwnerId(plot)
                .withFixture(true)
                .build();
        drawer.setKey("true");
        registry.register(drawer);

        Item key = new ItemBuilder()
                .withLabel("Basement Key")
                .withDescription("A brass key.")
                .withOwnerId(drawer)
                .build();
        registry.register(key);

        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> { }, false);
        runtime.configure(
                registry,
                plot.getId(),
                playerId,
                new ArrayList<>(),
                new HashMap<>(),
                null,
                null,
                null,
                Map.of()
        );

        Item taken = runtime.take("Basement Key");

        assertThat(taken).isNotNull();
        assertThat(taken.getOwnerId()).isEqualTo(playerId);
        assertThat(runtime.inventory())
                .extracting(Item::getLabel)
                .containsExactly("Basement Key");
    }
}

package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Rectangle2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the CLI crafting hook so future changes keep inventory in sync after crafting.
 */
class GameCliCraftTest {

    @Test
    void craftingRefreshesInventoryList() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        UUID playerId = UUID.randomUUID();

        Actor actor = new ActorBuilder()
                .withId(playerId)
                .withLabel("Player")
                .withDescription("Crafter")
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
        actor.setSkills(List.of("Firemaking"));
        registry.register(actor);
        Item stick = new ItemBuilder()
                .withLabel("Stick")
                .withDescription("Stick")
                .withOwnerId(actor)
                .build();
        Item rags = new ItemBuilder()
                .withLabel("Rags")
                .withDescription("Rags")
                .withOwnerId(actor)
                .build();
        registry.register(stick);
        registry.register(rags);

        List<Item> inventory = new ArrayList<>(List.of(stick, rags));

        Map<UUID, Map<UUID, Rectangle2D>> placements = new HashMap<>();
        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> {}, false);
        runtime.configure(registry, UUID.randomUUID(), playerId, inventory, placements, null, null, null, Map.of());
        runtime.craft("Torch");

        assertThat(inventory)
                .hasSize(1)
                .extracting(Item::getLabel)
                .containsExactly("Torch");
    }
}

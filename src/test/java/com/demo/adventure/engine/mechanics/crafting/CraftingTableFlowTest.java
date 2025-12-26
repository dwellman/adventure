package com.demo.adventure.engine.mechanics.crafting;

import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.HasResolver;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CraftingTableFlowTest {

    @Test
    void craftsTorchSoakedTorchAndLitTorchAcrossRooms() throws KeyExpressionCompileException {
        KernelRegistry registry = new KernelRegistry();
        UUID player = UUID.randomUUID();

        Plot entrance = new PlotBuilder()
                .withLabel("Cave Entrance")
                .withDescription("Entrance")
                .build();
        Plot cave = new PlotBuilder()
                .withLabel("Cave")
                .withDescription("Cave")
                .build();
        Plot darkRoom = new PlotBuilder()
                .withLabel("Dark Room")
                .withDescription("Dark")
                .build();
        registry.register(entrance);
        registry.register(cave);
        registry.register(darkRoom);
        // steps are logged via production logging; test printouts removed per request.

        Item stick = item("Stick", entrance);
        Item rags = item("Rags", entrance);
        Item kerosene = item("Kerosene", cave);
        Item flint = item("Flint", cave);
        Item riverStone = item("River Stone", cave);
        registry.register(stick);
        registry.register(rags);
        registry.register(kerosene);
        registry.register(flint);
        registry.register(riverStone);

        // Player actor with Firemaking skill for crafting checks.
        Actor actor = new ActorBuilder()
                .withId(player)
                .withLabel("Player")
                .withDescription("Crafter")
                .withOwnerId(entrance)
                .build();
        actor.setSkills(java.util.List.of("Firemaking"));
        registry.register(actor);

        CraftingTable table = new CraftingTable(registry, player);
        HasResolver hasPlayer = KeyExpressionEvaluator.registryHasResolver(registry, player);

        assertFalse(canEnterCave(hasPlayer), "Should not enter cave without a torch");

        take(registry, stick, player);
        take(registry, rags, player);
        assertTrue(table.craft("Torch"));
        assertTrue(hasPlayer.has("Torch"));
        assertFalse(hasPlayer.has("Stick"));
        assertFalse(hasPlayer.has("Rags"));

        assertTrue(canEnterCave(hasPlayer), "Torch allows cave entry");

        take(registry, kerosene, player);
        take(registry, flint, player);
        take(registry, riverStone, player);

        assertTrue(table.craft("Soaked Torch"));
        assertTrue(hasPlayer.has("Soaked Torch"));
        assertFalse(hasPlayer.has("Torch"));
        assertFalse(hasPlayer.has("Kerosene"));
        assertTrue(hasPlayer.has("Flint"), "Flint is not consumed while soaking");
        assertTrue(hasPlayer.has("River Stone"), "River stone stays available to spark");

        assertTrue(table.craft("Lit Torch"));
        assertTrue(hasPlayer.has("Lit Torch"));
        assertFalse(hasPlayer.has("Soaked Torch"));

        assertTrue(canEnterDarkRoom(hasPlayer), "Lit torch allows dark room entry");
    }

    private static boolean canEnterCave(HasResolver hasResolver) {
        return hasResolver.has("Torch") || hasResolver.has("Lit Torch");
    }

    private static boolean canEnterDarkRoom(HasResolver hasResolver) {
        return hasResolver.has("Lit Torch");
    }

    private static void take(KernelRegistry registry, Item item, UUID player) {
        registry.moveOwnership(item.getId(), player);
    }

    private static Item item(String label, Thing owner) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(label)
                .withOwnerId(owner)
                .build();
    }
}

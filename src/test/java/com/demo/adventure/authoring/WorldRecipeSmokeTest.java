package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.WorldAssembler;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRecipeSmokeTest {

    private static final UUID PLOT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PLOT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID DESK_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d5");

    @Test
    void buildsTwoPlotWorldWithBidirectionalGateAndFixture() throws GameBuilderException {
        WorldRecipe recipe = new WorldRecipe(
                1L,
                PLOT_A,
                List.of(
                        new WorldRecipe.PlotSpec(PLOT_A, "Hallway", "INTERIOR", 0, 0, "A plain hallway."),
                        new WorldRecipe.PlotSpec(PLOT_B, "Office", "INTERIOR", 1, 0, "A small office with a desk.")
                ),
                List.of(
                        new WorldRecipe.GateSpec(
                                PLOT_A,
                                Direction.E,
                                PLOT_B,
                                true,
                                "true",
                                "Hallway <-> Office",
                                "Doorway between hallway and office"
                        )
                ),
                List.of(
                        new WorldRecipe.FixtureSpec(DESK_ID, "Desk", "A sturdy desk.", PLOT_B, true, java.util.Map.of())
                )
        );

        WorldAssembler assembler = new WorldAssembler();
        WorldBuildResult result = assembler.build(recipe);
        KernelRegistry registry = result.registry();
        assertTrue(result.report().getProblems().isEmpty(), "Expected no validation problems for smoke world");

        Map<UUID, Thing> everything = registry.getEverything();
        assertEquals(4, everything.size(), "Expected two plots, one gate, and one fixture");

        Plot hallway = (Plot) everything.get(PLOT_A);
        Plot office = (Plot) everything.get(PLOT_B);
        assertNotNull(hallway);
        assertNotNull(office);
        assertEquals("INTERIOR", hallway.getRegion());
        assertEquals("INTERIOR", office.getRegion());
        assertEquals(0, hallway.getLocationX());
        assertEquals(0, hallway.getLocationY());
        assertEquals(1, office.getLocationX());
        assertEquals(0, office.getLocationY());
        assertEquals(KernelRegistry.MILIARIUM, hallway.getOwnerId());
        assertEquals(KernelRegistry.MILIARIUM, office.getOwnerId());

        Gate gate = everything.values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .findFirst()
                .orElseThrow();
        assertTrue(gate.isOpen());
        assertTrue(gate.isVisible());
        assertEquals("true", gate.getKeyString());
        assertEquals(gate.getId(), hallway.getGateId(Direction.E));
        assertEquals(gate.getId(), office.getGateId(Direction.W));
        assertEquals(gate, registry.findGates(PLOT_A, Direction.E).get(0));
        assertEquals(gate, registry.findGates(PLOT_B, Direction.W).get(0));

        Item desk = (Item) everything.get(DESK_ID);
        assertNotNull(desk);
        assertEquals(PLOT_B, desk.getOwnerId());
        assertTrue(desk.isVisible());
        assertTrue(desk.isFixture());
    }
}

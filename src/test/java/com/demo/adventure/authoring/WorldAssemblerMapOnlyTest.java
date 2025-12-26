package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.WorldAssembler;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorldAssemblerMapOnlyTest {

    private static final UUID PLOT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PLOT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void buildMapOnlyCreatesPlotsAndGates() throws GameBuilderException {
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
                        new WorldRecipe.FixtureSpec(UUID.randomUUID(), "Desk", "A sturdy desk.", PLOT_B, true, java.util.Map.of())
                )
        );

        WorldAssembler assembler = new WorldAssembler();
        KernelRegistry registry = assembler.buildMap(recipe);

        assertEquals(3, registry.getEverything().size(), "Expected two plots and one gate only");
        Plot hallway = (Plot) registry.get(PLOT_A);
        Plot office = (Plot) registry.get(PLOT_B);
        assertNotNull(hallway);
        assertNotNull(office);
        Gate gate = registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .findFirst()
                .orElse(null);
        assertNotNull(gate);
    }
}

package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldValidator;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorldOwnershipValidationTest {

    private static final UUID PLOT_ID = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final UUID DESK_ID = UUID.fromString("00000000-0000-0000-0000-0000000000bb");
    private static final UUID NOTE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000cc");

    @Test
    void fixtureOwnedByFixtureOwnedByLandIsFindable() throws GameBuilderException {
        Plot plot = new PlotBuilder()
                .withId(PLOT_ID)
                .withLabel("Office")
                .withDescription("Office plot")
                .withPlotRole("OFFICE")
                .withRegion("INTERIOR")
                .withLocationX(0)
                .withLocationY(0)
                .build();
        Item desk = new ItemBuilder()
                .withId(DESK_ID)
                .withLabel("Desk")
                .withDescription("Desk")
                .withOwnerId(plot)
                .build();
        desk.setFixture(true);
        Item note = new ItemBuilder()
                .withId(NOTE_ID)
                .withLabel("Note")
                .withDescription("Note")
                .withOwnerId(desk)
                .build();
        note.setFixture(true);

        KernelRegistry registry = new KernelRegistry();
        registry.register(plot);
        registry.register(desk);
        registry.register(note);

        WorldRecipe recipe = new WorldRecipe(
                1L,
                PLOT_ID,
                List.of(new WorldRecipe.PlotSpec(PLOT_ID, "Office", "INTERIOR", 0, 0, "Office plot")),
                List.of(),
                List.of()
        );

        WorldValidator validator = new WorldValidator();
        WorldBuildReport report = validator.validateWorld(recipe, registry);
        assertThat(report.getProblems()).isEmpty();
    }

    @Test
    void detectsOwnershipCycle() throws GameBuilderException {
        Plot plot = new PlotBuilder()
                .withId(PLOT_ID)
                .withLabel("Office")
                .withDescription("Office plot")
                .withPlotRole("OFFICE")
                .withRegion("INTERIOR")
                .withLocationX(0)
                .withLocationY(0)
                .build();
        Item desk = new ItemBuilder()
                .withId(DESK_ID)
                .withLabel("Desk")
                .withDescription("Desk")
                .withOwnerId(plot)
                .build();
        desk.setFixture(true);
        Item note = new ItemBuilder()
                .withId(NOTE_ID)
                .withLabel("Note")
                .withDescription("Note")
                .withOwnerId(desk)
                .build();
        note.setFixture(true);
        // cycle: desk owned by note
        desk.setOwnerId(note.getId());

        KernelRegistry registry = new KernelRegistry();
        registry.register(plot);
        registry.register(desk);
        registry.register(note);

        WorldRecipe recipe = new WorldRecipe(
                1L,
                PLOT_ID,
                List.of(new WorldRecipe.PlotSpec(PLOT_ID, "Office", "INTERIOR", 0, 0, "Office plot")),
                List.of(),
                List.of()
        );

        WorldValidator validator = new WorldValidator();
        WorldBuildReport report = validator.validateWorld(recipe, registry);
        assertThat(report.getProblems())
                .extracting(WorldBuildProblem::code)
                .contains("E_OWNERSHIP_CYCLE");
    }
}

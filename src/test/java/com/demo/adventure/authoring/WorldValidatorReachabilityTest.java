package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.WorldAssembler;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldValidator;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorldValidatorReachabilityTest {

    private static final UUID PLOT_START = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PLOT_CONNECTED = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PLOT_STRANDED = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void synthesizesGatesForAdjacentPlots() throws GameBuilderException {
        WorldRecipe recipe = new WorldRecipe(
                1L,
                PLOT_START,
                List.of(
                        new WorldRecipe.PlotSpec(PLOT_START, "Start", "INTERIOR", 0, 0, "Start plot."),
                        new WorldRecipe.PlotSpec(PLOT_CONNECTED, "Connected", "INTERIOR", 1, 0, "Connected plot."),
                        new WorldRecipe.PlotSpec(PLOT_STRANDED, "Stranded", "INTERIOR", 2, 0, "Stranded plot.")
                ),
                List.of(
                        new WorldRecipe.GateSpec(
                                PLOT_START,
                                Direction.E,
                                PLOT_CONNECTED,
                                true,
                                "true",
                                "Start <-> Connected",
                                "Path to connected"
                        )
                ),
                List.of()
        );

        WorldAssembler assembler = new WorldAssembler();
        KernelRegistry registry = assembler.buildMap(recipe);

        WorldValidator validator = new WorldValidator();
        WorldBuildReport report = validator.validateWorld(recipe, registry);

        assertThat(report.getProblems())
                .extracting(WorldBuildProblem::code)
                .doesNotContain("E_UNREACHABLE_PLOT");

        assertThat(registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .anyMatch(g -> g.connects(PLOT_CONNECTED) && g.connects(PLOT_STRANDED)))
                .isTrue();
    }
}

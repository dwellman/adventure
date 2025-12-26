package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.authoring.save.build.WorldAssembler;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldAssemblerCollectAllIntegrationTest {

    private static final UUID PLOT_START = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID PLOT_CONNECTED = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID PLOT_STRANDED = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID FIXTURE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    @Test
    void buildReportsRecipeAndWorldProblemsTogether() {
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
                                "Valid gate"
                        ),
                        new WorldRecipe.GateSpec(
                                PLOT_CONNECTED,
                                Direction.E,
                                UUID.fromString("00000000-0000-0000-0000-00000000beef"), // unknown plot
                                true,
                                "true",
                                "Broken Gate",
                                "Invalid gate"
                        )
                ),
                List.of(
                        new WorldRecipe.FixtureSpec(FIXTURE_ID, "Desk", "Desk 1", PLOT_CONNECTED, true, java.util.Map.of()),
                        new WorldRecipe.FixtureSpec(FIXTURE_ID, "Desk Dup", "Desk 2", PLOT_CONNECTED, true, java.util.Map.of())
                )
        );

        WorldAssembler assembler = new WorldAssembler();

        assertThatThrownBy(() -> assembler.build(recipe))
                .isInstanceOf(GameBuilderException.class)
                .satisfies(ex -> {
                    GameBuilderException gbe = (GameBuilderException) ex;
                    WorldBuildReport report = gbe.getReport();
                    assertThat(report.getProblems())
                            .extracting(WorldBuildProblem::code)
                            .contains(
                                    "E_GATE_TO_PLOT_NOT_FOUND",
                                    "E_FIXTURE_ID_DUPLICATE"
                            )
                            .doesNotContain("E_UNREACHABLE_PLOT");
                });
    }
}

package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldValidator;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorldGateKeyNormalizationTest {

    private static final UUID PLOT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PLOT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Test
    void blankKeyStringIsReportedAsMissing() throws GameBuilderException {
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
                                "   ",
                                "Hallway <-> Office",
                                "Doorway between hallway and office"
                        )
                ),
                List.of()
        );

        WorldValidator validator = new WorldValidator();
        WorldBuildReport report = validator.validateRecipe(recipe);
        assertThat(report.getProblems())
                .extracting(WorldBuildProblem::code)
                .contains("E_GATE_KEYSTRING_NULL");
    }
}

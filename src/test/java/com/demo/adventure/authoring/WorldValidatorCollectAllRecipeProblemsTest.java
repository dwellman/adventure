package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldValidator;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorldValidatorCollectAllRecipeProblemsTest {

    private static final UUID PLOT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID UNKNOWN = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
    private static final UUID FIXTURE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000f1");

    @Test
    void collectsMultipleRecipeProblems() throws GameBuilderException {
        WorldRecipe recipe = new WorldRecipe(
                1L,
                PLOT_A,
                List.of(
                        new WorldRecipe.PlotSpec(PLOT_A, "Hallway", "INTERIOR", 0, 0, "A plain hallway.")
                ),
                List.of(
                        new WorldRecipe.GateSpec(
                                UNKNOWN, // from plot missing
                                null, // direction missing
                                PLOT_A,
                                true,
                                null, // keyString null
                                "Broken Gate",
                                "Invalid gate"
                        )
                ),
                List.of(
                        new WorldRecipe.FixtureSpec(FIXTURE_ID, "Desk", "A sturdy desk.", PLOT_A, true, java.util.Map.of()),
                        new WorldRecipe.FixtureSpec(FIXTURE_ID, "Desk Duplicate", "Duplicate desk.", PLOT_A, true, java.util.Map.of())
                )
        );

        WorldValidator validator = new WorldValidator();
        WorldBuildReport report = validator.validateRecipe(recipe);

        assertThat(report.getProblems())
                .extracting(WorldBuildProblem::code)
                .contains(
                        "E_GATE_FROM_PLOT_NOT_FOUND",
                        "E_GATE_DIRECTION_MISSING",
                        "E_GATE_KEYSTRING_NULL",
                        "E_FIXTURE_ID_DUPLICATE"
                );
    }
}

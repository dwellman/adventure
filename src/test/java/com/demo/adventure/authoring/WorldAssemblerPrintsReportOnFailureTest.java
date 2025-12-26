package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.authoring.save.build.WorldAssembler;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.demo.adventure.test.ConsoleCaptureExtension;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorldAssemblerPrintsReportOnFailureTest {

    private static final UUID PLOT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PLOT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID FIXTURE_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d5");

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void printsReportOnFailure() {
        WorldRecipe recipe = new WorldRecipe(
                1L,
                PLOT_A,
                List.of(
                        new WorldRecipe.PlotSpec(PLOT_A, "Hallway", "INTERIOR", 0, 0, "A plain hallway.")
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
                        new WorldRecipe.FixtureSpec(FIXTURE_ID, "Desk", "A sturdy desk.", PLOT_B, true, java.util.Map.of()),
                        new WorldRecipe.FixtureSpec(FIXTURE_ID, "Desk Duplicate", "Duplicate desk.", PLOT_B, true, java.util.Map.of())
                )
        );

        console.reset();
        WorldAssembler assembler = new WorldAssembler();
        assertThatThrownBy(() -> assembler.build(recipe))
                .isInstanceOf(GameBuilderException.class);

        String output = console.output();
        assertThat(output).contains("World Build Report");
        assertThat(output).contains("E_GATE_TO_PLOT_NOT_FOUND");
        assertThat(output).contains("E_FIXTURE_ID_DUPLICATE");
    }
}

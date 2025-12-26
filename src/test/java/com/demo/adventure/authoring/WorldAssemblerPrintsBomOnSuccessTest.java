package com.demo.adventure.authoring;

import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.authoring.save.build.WorldAssembler;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.demo.adventure.test.ConsoleCaptureExtension;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WorldAssemblerPrintsBomOnSuccessTest {

    private static final UUID PLOT_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID PLOT_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final UUID DESK_ID = UUID.fromString("00000000-0000-0000-0000-0000000000d5");

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void printsBomOnSuccess() throws Exception {
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

        String previousProperty = System.getProperty("adventure.bom");
        System.setProperty("adventure.bom", "true");
        try {
            console.reset();
            WorldAssembler assembler = new WorldAssembler();
            assembler.build(recipe);
        } finally {
            if (previousProperty == null) {
                System.clearProperty("adventure.bom");
            } else {
                System.setProperty("adventure.bom", previousProperty);
            }
        }

        String output = console.output();
        assertThat(output).contains("World Bill of Materials");
        assertThat(output).contains("Map");
        assertThat(output).contains("Fixtures");
        assertThat(output).contains("2× Land plots");
        assertThat(output).contains("1× Gates");
        assertThat(output).contains("1× Desk");
    }
}

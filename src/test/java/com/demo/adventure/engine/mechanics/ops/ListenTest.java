package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ListenTest {

    @Test
    void returnsEmptyForNullTarget() {
        assertThat(Listen.listen(null)).isEmpty();
    }

    @Test
    void includesLabelAndDescriptionWhenPresent() {
        Plot plot = new PlotBuilder()
                .withLabel("Shore")
                .withDescription("A windy shore.")
                .withPlotRole("SHORE")
                .build();
        Item shell = new ItemBuilder()
                .withLabel("Shell")
                .withDescription("It hums with a faint echo.")
                .withOwnerId(plot)
                .build();

        String output = Listen.listen(shell);

        assertThat(output).contains("Shell");
        assertThat(output).contains("It hums with a faint echo.");
    }
}

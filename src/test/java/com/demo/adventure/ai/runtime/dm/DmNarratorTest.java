package com.demo.adventure.ai.runtime.dm;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.engine.mechanics.ops.Look;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DmNarratorTest {

    private static final UUID PLOT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FIXTURE_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Test
    void fallsBackToBaseWhenNoAgent() {
        Plot plot = new PlotBuilder()
                .withId(PLOT_ID)
                .withLabel("Meadow")
                .withDescription("A quiet meadow.")
                .withPlotRole("MEADOW")
                .build();
        String base = new DmNarrator().describe(plot, List.of(), List.of());
        assertThat(base).contains("Meadow").contains("A quiet meadow.");
    }

    @Test
    void usesAgentRewrite() {
        Plot plot = new PlotBuilder()
                .withId(PLOT_ID)
                .withLabel("Cave")
                .withDescription("A dark cave.")
                .withPlotRole("CAVE")
                .build();
        DmAgent agent = ctx -> "AI: " + ctx.baseText();
        String rewritten = new DmNarrator(agent).describe(plot, List.of(), List.of());
        assertThat(rewritten).startsWith("AI: ").contains("Cave");
    }

    @Test
    void suppliesFixtureAndInventorySummaries() throws Exception {
        Plot plot = new PlotBuilder()
                .withId(PLOT_ID)
                .withLabel("Cavern")
                .withDescription("Stone walls.")
                .withPlotRole("CAVERN")
                .build();
        Item fixture = new ItemBuilder()
                .withId(FIXTURE_ID)
                .withLabel("Lantern")
                .withDescription("Still burning.")
                .withOwnerId(plot)
                .build();
        fixture.setFixture(true);

        Item coin = new ItemBuilder()
                .withLabel("Coin")
                .withDescription("Old coin.")
                .withOwnerId(plot)
                .build();
        Look.ContainerEntry entry = new Look.ContainerEntry(coin, null);

        DmAgentContext[] captured = new DmAgentContext[1];
        DmAgent agent = ctx -> {
            captured[0] = ctx;
            return ctx.baseText();
        };

        String line = new DmNarrator(agent).describe(plot, List.of(fixture), List.of(entry));
        assertThat(line).isNotBlank();
        assertThat(captured[0]).isNotNull();
        assertThat(captured[0].fixtureSummaries()).anyMatch(s -> s.contains("Lantern"));
        assertThat(captured[0].inventorySummaries()).anyMatch(s -> s.contains("Coin"));
    }

    @Test
    void fallsBackOnAgentFailure() {
        Plot plot = new PlotBuilder()
                .withId(PLOT_ID)
                .withLabel("Cave")
                .withDescription("A dark cave.")
                .withPlotRole("CAVE")
                .build();
        DmAgent agent = ctx -> { throw new RuntimeException("fail"); };
        String line = new DmNarrator(agent).describe(plot, List.of(), List.of());
        assertThat(line).contains("Cave").contains("A dark cave.");
    }
}

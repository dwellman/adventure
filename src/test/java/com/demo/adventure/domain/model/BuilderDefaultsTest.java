package com.demo.adventure.domain.model;

import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BuilderDefaultsTest {

    @Test
    void plotBuilderDefaultsLandAnchoredToMiliarium() {
        UUID id = UUID.randomUUID();
        Plot plot = new PlotBuilder()
                .withId(id)
                .withLabel("Plot")
                .withDescription("Desc")
                .build();

        assertThat(plot.getPlotKind()).isEqualTo(PlotKind.LAND);
        assertThat(plot.getOwnerId()).isEqualTo(KernelRegistry.MILIARIUM);
        assertThat(plot.getLocationX()).isZero();
        assertThat(plot.getLocationY()).isZero();
    }

    @Test
    void gateBuilderDefaultsOpenVisibleKey() {
        Plot plotA = new PlotBuilder()
                .withLabel("Plot A")
                .withDescription("Plot A")
                .build();
        Plot plotB = new PlotBuilder()
                .withLabel("Plot B")
                .withDescription("Plot B")
                .build();
        Gate gate = new GateBuilder()
                .withLabel("Gate")
                .withDescription("Desc")
                .withPlotA(plotA)
                .withPlotB(plotB)
                .withDirection(Direction.N)
                .build();

        assertThat(gate.isOpen()).isTrue();
        assertThat(gate.isVisible()).isTrue();
        assertThat(gate.getKeyString()).isEqualTo("true");
    }

    @Test
    void itemBuilderDefaultsVisibilityAndFixture() {
        UUID owner = UUID.randomUUID();
        Item item = new ItemBuilder()
                .withLabel("Item")
                .withDescription("Desc")
                .withOwnerId(owner)
                .build();

        assertThat(item.isVisible()).isTrue();
        assertThat(item.isOpen()).isTrue();
        assertThat(item.isFixture()).isFalse();
        assertThat(item.getKey()).isEqualTo("true");
    }

    @Test
    void actorBuilderDefaultsVisibility() {
        UUID owner = UUID.randomUUID();
        Actor actor = new ActorBuilder()
                .withLabel("Actor")
                .withDescription("Desc")
                .withOwnerId(owner)
                .build();

        assertThat(actor.isVisible()).isTrue();
        assertThat(actor.isOpen()).isTrue();
        assertThat(actor.getKey()).isEqualTo("true");
    }
}

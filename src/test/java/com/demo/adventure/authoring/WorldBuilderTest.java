package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.samples.TwoPlotWorldFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

class WorldBuilderTest {

    @Test
    void buildsTwoPlotWorldAndConnectsNorth() throws GameBuilderException {
        KernelRegistry registry = new KernelRegistry();

        TwoPlotWorldFactory.build(registry);

        Map<UUID, ?> everything = registry.getEverything();
        List<Plot> plots = everything.values().stream()
                .filter(Plot.class::isInstance)
                .map(Plot.class::cast)
                .collect(toList());
        List<Gate> gates = everything.values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .collect(toList());

        assertThat(plots).hasSize(2);
        assertThat(gates).hasSize(1);

        Plot center = plots.stream().filter(p -> "CENTER".equals(p.getPlotRole())).findFirst().orElse(null);
        Plot north = plots.stream().filter(p -> "NORTH".equals(p.getPlotRole())).findFirst().orElse(null);
        assertThat(center).isNotNull();
        assertThat(north).isNotNull();

        Gate gate = gates.get(0);
        assertThat(gate.connects(center.getId())).isTrue();
        assertThat(gate.connects(north.getId())).isTrue();

        assertThat(center.getGateId(Direction.N)).isEqualTo(gate.getId());
        assertThat(north.getGateId(Direction.S)).isEqualTo(gate.getId());
    }
}

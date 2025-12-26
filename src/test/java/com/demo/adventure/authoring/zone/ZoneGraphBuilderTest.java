package com.demo.adventure.authoring.zone;

import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneGraphBuilderTest {

    @Test
    void generatesConnectedZoneWithLoopAndLandmarks() {
        ZoneSpec spec = new ZoneSpec(
                "demo-zone",
                "TEST",
                16,
                MappingDifficulty.MEDIUM,
                PacingProfile.BALANCED,
                TopologyBias.BRANCHY,
                List.of(
                        new AnchorSpec("entry", "Entry", AnchorRole.ENTRY, "Start here."),
                        new AnchorSpec("vista", "Vista", AnchorRole.VISTA, ""),
                        new AnchorSpec("exit", "Exit", AnchorRole.EXIT, "Finish here.")
                )
        );

        ZoneGraphBuilder builder = new ZoneGraphBuilder();
        ZoneBuildResult result = builder.generate(spec, 99L);

        WorldRecipe recipe = result.recipe();
        assertThat(recipe.plots()).isNotEmpty();
        assertThat(recipe.plots().size()).isLessThanOrEqualTo(spec.targetPlotCount());

        // All plots should have a description after landmark pass.
        assertThat(recipe.plots())
                .allMatch(p -> p.description() != null && !p.description().isBlank());

        // Every plot appears in adjacency and the graph is connected from the start plot.
        assertThat(isConnected(recipe.startPlotId(), recipe.gates(), recipe.plots()))
                .isTrue();

        // Gates carry directions.
        assertThat(recipe.gates()).allMatch(g -> g.direction() != null);

        // Loop metric should be computed and non-negative.
        assertThat(result.metrics().loopCount()).isGreaterThanOrEqualTo(0);

        // Anchors should resolve to plot ids.
        assertThat(result.anchorPlotIds()).containsKeys("entry", "vista", "exit");
        assertThat(result.anchorRolePlotIds()).containsKeys(AnchorRole.ENTRY, AnchorRole.EXIT);
    }

    private boolean isConnected(UUID start, List<WorldRecipe.GateSpec> gates, List<WorldRecipe.PlotSpec> plots) {
        if (plots.isEmpty()) {
            return true;
        }
        Set<UUID> seen = new java.util.HashSet<>();
        java.util.Deque<UUID> queue = new java.util.ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty()) {
            UUID u = queue.removeFirst();
            for (WorldRecipe.GateSpec g : gates) {
                if (g.fromPlotId().equals(u)) {
                    if (seen.add(g.toPlotId())) {
                        queue.addLast(g.toPlotId());
                    }
                }
                if (g.toPlotId().equals(u)) {
                    if (seen.add(g.fromPlotId())) {
                        queue.addLast(g.fromPlotId());
                    }
                }
            }
        }
        return seen.size() == plots.size();
    }
}

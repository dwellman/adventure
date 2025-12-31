package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.GateBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import org.junit.jupiter.api.Test;

import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameCliExitFormattingTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void exitListOmitsGateHints() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot origin = new PlotBuilder()
                .withLabel("Lagoon Shore")
                .withDescription("desc")
                .build();
        Plot destination = new PlotBuilder()
                .withLabel("Offshore Sandbar")
                .withDescription("desc")
                .build();
        Plot west = new PlotBuilder()
                .withLabel("West Path")
                .withDescription("desc")
                .build();
        registry.register(origin);
        registry.register(destination);
        registry.register(west);

        Gate blockedSouth = new GateBuilder()
                .withLabel("Blocked South")
                .withDescription("A patched raft could ferry you to the sandbar.")
                .withPlotA(origin)
                .withPlotB(destination)
                .withDirection(Direction.S)
                .withVisible(true)
                .withKeyString("false")
                .build();
        Gate openWest = new GateBuilder()
                .withLabel("Open West")
                .withDescription("")
                .withPlotA(origin)
                .withPlotB(west)
                .withDirection(Direction.W)
                .withVisible(true)
                .withKeyString("true")
                .build();
        registry.register(blockedSouth);
        registry.register(openWest);

        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> {}, false);
        runtime.configure(registry, origin.getId(), UUID.randomUUID(), new ArrayList<>(), new HashMap<>(), null, null, null, Map.of());

        console.reset();
        runtime.describe();
        String output = console.output();
        assertThat(output).contains("Exits: SOUTH \u2022 WEST");
        assertThat(output).doesNotContain("sandbar");
    }
}

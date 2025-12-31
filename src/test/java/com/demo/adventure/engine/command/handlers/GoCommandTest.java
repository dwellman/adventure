package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.GateBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandOutput;
import com.demo.adventure.engine.command.CommandPhrase;
import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.engine.runtime.SceneNarrator;
import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GoCommandTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    private String previousNoColor;

    @BeforeEach
    void disableAnsi() {
        previousNoColor = System.getProperty("NO_COLOR");
        System.setProperty("NO_COLOR", "1");
    }

    @AfterEach
    void restoreAnsi() {
        if (previousNoColor == null) {
            System.clearProperty("NO_COLOR");
        } else {
            System.setProperty("NO_COLOR", previousNoColor);
        }
    }

    @Test
    void blockedMoveUsesGateDescriptionOnly() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot origin = new PlotBuilder()
                .withLabel("Train Platform")
                .withDescription("desc")
                .build();
        Plot destination = new PlotBuilder()
                .withLabel("Rail Yard")
                .withDescription("desc")
                .build();
        registry.register(origin);
        registry.register(destination);

        Gate gate = new GateBuilder()
                .withLabel("Freight Door")
                .withDescription("A padlocked freight door with a brass tag")
                .withPlotA(origin)
                .withPlotB(destination)
                .withDirection(Direction.N)
                .withVisible(true)
                .withKeyString("false")
                .build();
        registry.register(gate);

        UUID playerId = UUID.randomUUID();
        registry.register(new ActorBuilder()
                .withId(playerId)
                .withLabel("Player")
                .withDescription("desc")
                .withOwnerId(origin)
                .build());

        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> {}, false);
        runtime.configure(registry, origin.getId(), playerId, new ArrayList<>(), new HashMap<>(), null, null, Map.of(), Map.of());

        CommandContext context = new CommandContext(new CommandOutput() {
            @Override
            public void emit(String text) {
            }

            @Override
            public void printHelp() {
            }
        }, runtime);

        GoCommand handler = new GoCommand();
        Command command = Command.from(CommandAction.GO, new CommandPhrase("north", "north", null, null));

        console.reset();
        handler.handle(context, command);

        String output = console.output();
        assertThat(output).contains("A padlocked freight door with a brass tag.");
        assertThat(output).doesNotContain("You can't go that way.");
    }

    @Test
    void lookDirectionPrefixesGateDescription() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot origin = new PlotBuilder()
                .withLabel("Train Platform")
                .withDescription("desc")
                .build();
        Plot destination = new PlotBuilder()
                .withLabel("Rail Yard")
                .withDescription("desc")
                .build();
        registry.register(origin);
        registry.register(destination);

        Gate gate = new GateBuilder()
                .withLabel("Freight Door")
                .withDescription("A padlocked freight door with a brass tag")
                .withPlotA(origin)
                .withPlotB(destination)
                .withDirection(Direction.N)
                .withVisible(true)
                .withKeyString("true")
                .build();
        registry.register(gate);

        UUID playerId = UUID.randomUUID();
        registry.register(new ActorBuilder()
                .withId(playerId)
                .withLabel("Player")
                .withDescription("desc")
                .withOwnerId(origin)
                .build());

        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> {}, false);
        runtime.configure(registry, origin.getId(), playerId, new ArrayList<>(), new HashMap<>(), null, null, Map.of(), Map.of());

        console.reset();
        runtime.lookDirectionOrThing("north");

        String output = console.output();
        assertThat(output).contains("To the north: A padlocked freight door with a brass tag");
    }

    @Test
    void lookDirectionStripsTrailingDestinationTag() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot origin = new PlotBuilder()
                .withLabel("Hall")
                .withDescription("desc")
                .build();
        Plot destination = new PlotBuilder()
                .withLabel("Corridor Hall-Lounge")
                .withDescription("desc")
                .build();
        registry.register(origin);
        registry.register(destination);

        Gate gate = new GateBuilder()
                .withLabel("Hall Gate")
                .withDescription("A passage leads east to Corridor Hall-Lounge. to: Corridor Hall-Lounge")
                .withPlotA(origin)
                .withPlotB(destination)
                .withDirection(Direction.E)
                .withVisible(true)
                .withKeyString("true")
                .build();
        registry.register(gate);

        UUID playerId = UUID.randomUUID();
        registry.register(new ActorBuilder()
                .withId(playerId)
                .withLabel("Player")
                .withDescription("desc")
                .withOwnerId(origin)
                .build());

        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> {}, false);
        runtime.configure(registry, origin.getId(), playerId, new ArrayList<>(), new HashMap<>(), null, null, Map.of(), Map.of());

        console.reset();
        runtime.lookDirectionOrThing("east");

        String output = console.output();
        assertThat(output).contains("To the east: A passage leads east to Corridor Hall-Lounge.");
        assertThat(output).doesNotContain("to: Corridor Hall-Lounge");
    }
}

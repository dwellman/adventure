package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.mechanics.combat.UnknownReferenceReceipt;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
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

class GameCliCombatErrorTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void attackUnknownTargetEmitsReceiptAndMessage() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Arena")
                .withDescription("Arena")
                .build();
        UUID plotId = plot.getId();
        UUID playerId = UUID.randomUUID();
        Actor player = new ActorBuilder()
                .withId(playerId)
                .withLabel("Player")
                .withDescription("Player")
                .withOwnerId(plot)
                .build();
        registry.register(plot);
        registry.register(player);

        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> {}, false);
        runtime.configure(registry, plotId, playerId, new ArrayList<>(), new HashMap<>(), null, null, null, Map.of());
        narrator.setLastCommand("attack blorf");

        console.reset();
        runtime.attack("blorf");
        String output = console.output();
        assertThat(output).contains("I don't know what that is.");
        assertThat(registry.getReceipts()).hasSize(1);
        Object receipt = registry.getReceipts().get(0);
        assertThat(receipt).isInstanceOf(UnknownReferenceReceipt.class);
        UnknownReferenceReceipt unknown = (UnknownReferenceReceipt) receipt;
        assertThat(unknown.commandText()).isEqualTo("attack blorf");
        assertThat(unknown.token()).isEqualTo("blorf");
    }

    @Test
    void fleeWithoutEncounterReportsNotInCombat() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Arena")
                .withDescription("Arena")
                .build();
        UUID plotId = plot.getId();
        UUID playerId = UUID.randomUUID();
        Actor player = new ActorBuilder()
                .withId(playerId)
                .withLabel("Player")
                .withDescription("Player")
                .withOwnerId(plot)
                .build();
        registry.register(plot);
        registry.register(player);

        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> {}, false);
        runtime.configure(registry, plotId, playerId, new ArrayList<>(), new HashMap<>(), null, null, null, Map.of());

        console.reset();
        runtime.flee();
        String output = console.output();
        assertThat(output).contains("You are not in combat.");
        assertThat(registry.getReceipts()).isEmpty();
    }

}

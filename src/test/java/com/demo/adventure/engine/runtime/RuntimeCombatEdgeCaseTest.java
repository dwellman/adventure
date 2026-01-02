package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.engine.mechanics.combat.CombatEncounter;
import com.demo.adventure.engine.mechanics.combat.CombatEngine;
import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuntimeCombatEdgeCaseTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void attackNotPlayersTurnReports() throws Exception {
        Harness harness = new Harness();
        harness.startEncounter(harness.npc.getId());

        console.reset();
        harness.runtime.attack(harness.npc.getLabel());

        assertThat(console.output()).contains("It is not your turn.");
    }

    @Test
    void fleeNotPlayersTurnReports() throws Exception {
        Harness harness = new Harness();
        harness.startEncounter(harness.npc.getId());

        console.reset();
        harness.runtime.flee();

        assertThat(console.output()).contains("It is not your turn.");
    }

    @Test
    void attackSelfReports() throws Exception {
        Harness harness = new Harness();

        console.reset();
        harness.runtime.attack(harness.player.getLabel());

        assertThat(console.output()).contains("You can't attack yourself.");
    }

    @Test
    void attackAlreadyDefeatedTargetReports() throws Exception {
        Harness harness = new Harness();
        CombatEncounter encounter = harness.startEncounter(harness.player.getId());
        encounter.markDefeated(harness.npc.getId());

        console.reset();
        harness.runtime.attack(harness.npc.getLabel());

        assertThat(console.output()).contains("They are not here.");
    }

    private static class Harness {
        private final KernelRegistry registry = new KernelRegistry();
        private final Plot plot;
        private final UUID playerId = UUID.randomUUID();
        private final UUID npcId = UUID.randomUUID();
        private final Actor player;
        private final Actor npc;
        private final GameRuntime runtime;

        private Harness() {
            plot = new PlotBuilder()
                    .withLabel("Arena")
                    .withDescription("Arena")
                    .build();
            player = new ActorBuilder()
                    .withId(playerId)
                    .withLabel("Player")
                    .withDescription("Player")
                    .withOwnerId(plot)
                    .build();
            npc = new ActorBuilder()
                    .withId(npcId)
                    .withLabel("Rival")
                    .withDescription("Rival")
                    .withOwnerId(plot)
                    .build();
            registry.register(plot);
            registry.register(player);
            registry.register(npc);

            SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
            runtime = new GameRuntime(narrator, text -> { }, false);
            runtime.configure(registry, plot.getId(), playerId, new ArrayList<>(), new HashMap<>(), null, null, null, Map.of());
        }

        private CombatEncounter startEncounter(UUID instigatorId) {
            List<Actor> participants = List.of(player, npc);
            CombatEncounter encounter = CombatEngine.startEncounter(registry, plot.getId(), participants, instigatorId);
            runtime.setEncounter(encounter);
            return encounter;
        }
    }
}

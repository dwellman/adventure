package com.demo.adventure.engine.flow.trigger;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.engine.flow.loop.LoopResetReason;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TriggerEngineTest {

    @Test
    void fireOnTakeAppliesActions() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Beach")
                .withDescription("A windswept beach.")
                .build();
        registry.register(plot);

        Actor player = new ActorBuilder()
                .withLabel("Player")
                .withDescription("Player")
                .withOwnerId(plot)
                .build();
        player.setCell("ENERGY", new Cell(10, 0));
        registry.register(player);

        Item gem = new ItemBuilder()
                .withLabel("Gem")
                .withDescription("Gem")
                .withOwnerId(plot)
                .build();
        registry.register(gem);

        Item door = new ItemBuilder()
                .withLabel("Door")
                .withDescription("Door")
                .withOwnerId(plot)
                .withVisible(false)
                .build();
        registry.register(door);

        TriggerDefinition trigger = new TriggerDefinition(
                "gem-pickup",
                TriggerType.ON_TAKE,
                "Gem",
                "",
                "",
                List.of(
                        action(TriggerActionType.MESSAGE, null, null, "The gem warms in your palm.", null, null, null, null, null, null, null),
                        action(TriggerActionType.REVEAL, "Door", null, null, null, null, null, null, null, null, null),
                        action(TriggerActionType.SET_CELL, "@PLAYER", null, null, null, null, "ENERGY", 3L, null, null, null),
                        action(TriggerActionType.RESET_LOOP, null, null, "A flash knocks you back.", null, null, null, null, null, "MANUAL", null)
                )
        );

        TriggerEngine engine = new TriggerEngine(List.of(trigger));
        TriggerOutcome outcome = engine.fire(
                new TriggerEvent(TriggerType.ON_TAKE, gem.getLabel(), "", gem.getId(), null),
                new TriggerContext(registry, plot.getId(), player.getId(), null)
        );

        assertThat(door.isVisibleFlag()).isTrue();
        assertThat(player.getCell("ENERGY").getAmount()).isEqualTo(3L);
        assertThat(outcome.messages()).containsExactly("The gem warms in your palm.");
        assertThat(outcome.resetReason()).isEqualTo(LoopResetReason.MANUAL);
        assertThat(outcome.resetMessage()).isEqualTo("A flash knocks you back.");
    }

    @Test
    void endGameActionSetsFlag() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Beach")
                .withDescription("Beach")
                .build();
        registry.register(plot);

        Actor player = new ActorBuilder()
                .withLabel("Player")
                .withDescription("Player")
                .withOwnerId(plot)
                .build();
        registry.register(player);

        TriggerDefinition trigger = new TriggerDefinition(
                "escape",
                TriggerType.ON_TURN,
                "Beach",
                "",
                "",
                List.of(
                        action(TriggerActionType.END_GAME, null, null, "You escape.", null, null, null, null, null, null, null)
                )
        );
        TriggerEngine engine = new TriggerEngine(List.of(trigger));

        TriggerOutcome outcome = engine.fire(
                new TriggerEvent(TriggerType.ON_TURN, plot.getLabel(), "", plot.getId(), null),
                new TriggerContext(registry, plot.getId(), player.getId(), null)
        );

        assertThat(outcome.endGame()).isTrue();
        assertThat(outcome.messages()).containsExactly("You escape.");
    }

    private static TriggerAction action(
            TriggerActionType type,
            String target,
            String owner,
            String text,
            String key,
            String visibilityKey,
            String cell,
            Long amount,
            String description,
            String reason,
            Boolean visible
    ) {
        return new TriggerAction(type, target, owner, text, key, visibilityKey, cell, amount, description, reason, visible);
    }
}

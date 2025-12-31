package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.save.ActorRecipeBuilder;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.ItemRecipeBuilder;
import com.demo.adventure.domain.save.WorldRecipe;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.loop.LoopResetReason;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerAction;
import com.demo.adventure.engine.flow.trigger.TriggerActionType;
import com.demo.adventure.engine.flow.trigger.TriggerDefinition;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;
import com.demo.adventure.engine.flow.trigger.TriggerOutcome;
import com.demo.adventure.engine.flow.trigger.TriggerType;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameRuntimeCoverageTest {

    @Test
    void describeUpdatesSceneState() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        harness.runtime.describe();

        assertThat(harness.runtime.lastSceneState()).contains("Start");
    }

    @Test
    void primeSceneSeedsSnapshot() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        harness.runtime.primeScene();

        assertThat(harness.runtime.lastSceneState()).contains("Start");
        assertThat(harness.runtime.lastSceneState()).contains("Exits:");
    }

    @Test
    void gateDescriptionsUseFromPlot() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        com.demo.adventure.domain.model.Gate gate = harness.findGate("Start -> Hall");

        assertThat(gate.getDescriptionFrom(harness.plotAId())).isEqualTo("A blocked door");
        assertThat(gate.getDescriptionFrom(harness.plotBId())).isEqualTo("An open door");
    }

    @Test
    void takeDropAndInventoryFlow() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        Item taken = harness.runtime.take("Keycard");
        assertThat(taken).isNotNull();
        assertThat(harness.runtime.inventoryLabels()).contains("Keycard");

        harness.runtime.showInventory();
        harness.runtime.drop("Keycard");

        assertThat(harness.runtime.inventoryLabels()).doesNotContain("Keycard");
    }

    @Test
    void takeFailsWhenInventoryFull() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();
        Map<UUID, Map<UUID, Rectangle2D>> placements = harness.runtimePlacements();
        UUID bucket = harness.backpackId;
        placements.computeIfAbsent(bucket, k -> new HashMap<>())
                .put(UUID.randomUUID(), new Rectangle2D(0, 0, 1, 1));

        Item item = harness.runtime.take("Boulder");

        assertThat(item).isNull();
    }

    @Test
    void openHandlesGateAndItem() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        harness.runtime.open("east");
        com.demo.adventure.domain.model.Gate gate = harness.findGate("Start -> Hall");
        assertThat(gate.getKey()).isEqualTo("false");

        harness.runtime.open("Chest");
        Item chest = harness.findItem("Chest");
        assertThat(chest.getKey()).isEqualTo("true");

        harness.runtime.open("Unknown");
    }

    @Test
    void useHandlesTransfersAndMissingTargets() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        UseResult invalid = harness.runtime.use("", null, null);
        assertThat(invalid.valid()).isFalse();

        UseResult missing = harness.runtime.use("Battery", "on", "Missing");
        assertThat(missing.valid()).isFalse();

        UseResult transfer = harness.runtime.use("Battery", "on", "Lamp");
        assertThat(transfer.valid()).isTrue();
        Item lamp = harness.findItem("Lamp");
        Cell cell = lamp.getCell("CHARGE");
        assertThat(cell).isNotNull();
        assertThat(cell.getAmount()).isGreaterThan(0);
    }

    @Test
    void moveRespectsClosedGate() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        assertThat(harness.runtime.move(Direction.E)).isNull();
        harness.openGate("Start -> Hall");
        assertThat(harness.runtime.move(Direction.E)).isEqualTo(harness.plotBId);
    }

    @Test
    void resolveTriggerOutcomeCoversEndGameAndReset() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        CommandOutcome endGame = harness.runtime.resolveTriggerOutcome(
                new TriggerOutcome(List.of("Done"), null, "", true)
        );
        assertThat(endGame.endGame()).isTrue();

        CommandOutcome reset = harness.runtime.resolveTriggerOutcome(
                new TriggerOutcome(List.of("Reset"), LoopResetReason.MANUAL, "", false)
        );
        assertThat(reset.skipTurnAdvance()).isTrue();
    }

    @Test
    void exploreRevealsHiddenItem() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();
        KeyExpressionEvaluator.DiceRoller prior = KeyExpressionEvaluator.getDefaultDiceRoller();
        KeyExpressionEvaluator.setDefaultDiceRoller(sides -> sides);
        try {
            Item hidden = harness.findItem("Hidden Note");
            assertThat(hidden.isVisible()).isFalse();

            harness.runtime.explore();

            assertThat(hidden.isVisible()).isTrue();
        } finally {
            KeyExpressionEvaluator.setDefaultDiceRoller(prior);
        }
    }

    @Test
    void putHandlesMissingTarget() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        harness.runtime.put("", null);
        harness.runtime.put("Keycard", "Chest");
    }

    @Test
    void parseDirectionHandlesInvalidInput() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        assertThat(harness.runtime.parseDirection("north")).isEqualTo(Direction.N);
        assertThat(harness.runtime.parseDirection("nope")).isNull();
    }

    @Test
    void fireTriggerEmitsOutcome() throws Exception {
        RuntimeHarness harness = new RuntimeHarness();

        Item keycard = harness.findItem("Keycard");
        TriggerOutcome outcome = harness.runtime.fireTrigger(TriggerType.ON_USE, keycard, null);

        assertThat(outcome.messages()).isNotEmpty();
    }

    private static class RuntimeHarness {
        private final UUID plotAId = UUID.randomUUID();
        private final UUID plotBId = UUID.randomUUID();
        private final UUID playerId = UUID.randomUUID();
        private final UUID enemyId = UUID.randomUUID();
        private final UUID chestId = UUID.randomUUID();
        private final UUID backpackId = UUID.randomUUID();
        private final List<String> output = new ArrayList<>();
        private final KernelRegistry registry;
        private final GameRuntime runtime;
        private final Map<UUID, Map<UUID, Rectangle2D>> placements = new HashMap<>();

        private RuntimeHarness() throws Exception {
            List<WorldRecipe.PlotSpec> plots = List.of(
                    new WorldRecipe.PlotSpec(plotAId, "Start", "TEST", 0, 0, "Start plot"),
                    new WorldRecipe.PlotSpec(plotBId, "Hall", "TEST", 1, 0, "Hall plot")
            );
            List<WorldRecipe.GateSpec> gates = List.of(
                    new WorldRecipe.GateSpec(plotAId, Direction.E, plotBId, true, "false", "Start -> Hall", "A blocked door"),
                    new WorldRecipe.GateSpec(plotBId, Direction.W, plotAId, true, "true", "Hall -> Start", "An open door")
            );
            List<WorldRecipe.FixtureSpec> fixtures = List.of(
                    new WorldRecipe.FixtureSpec(chestId, "Chest", "A wooden chest", plotAId, true, Map.of())
            );
            List<GameSave.ItemRecipe> items = List.of(
                    new ItemRecipeBuilder()
                            .withId(UUID.randomUUID())
                            .withName("Keycard")
                            .withDescription("A plastic keycard")
                            .withOwnerId(plotAId)
                            .build(),
                    new ItemRecipeBuilder()
                            .withId(UUID.randomUUID())
                            .withName("Boulder")
                            .withDescription("A heavy boulder")
                            .withOwnerId(plotAId)
                            .withFootprint(5, 5)
                            .build(),
                    new ItemRecipeBuilder()
                            .withId(backpackId)
                            .withName("Backpack")
                            .withDescription("A canvas backpack")
                            .withOwnerId(playerId)
                            .withCapacity(2, 2)
                            .build(),
                    new ItemRecipeBuilder()
                            .withId(UUID.randomUUID())
                            .withName("Gem")
                            .withDescription("A shiny gem")
                            .withOwnerId(chestId)
                            .build(),
                    new ItemRecipeBuilder()
                            .withId(UUID.randomUUID())
                            .withName("Battery")
                            .withDescription("A spare battery")
                            .withOwnerId(plotAId)
                            .withCells(Map.of("CHARGE", new CellSpec(5, 2)))
                            .build(),
                    new ItemRecipeBuilder()
                            .withId(UUID.randomUUID())
                            .withName("Lamp")
                            .withDescription("A brass lamp")
                            .withOwnerId(plotAId)
                            .withCells(Map.of("CHARGE", new CellSpec(5, 0)))
                            .build(),
                    new ItemRecipeBuilder()
                            .withId(UUID.randomUUID())
                            .withName("Hidden Note")
                            .withDescription("A hidden note")
                            .withOwnerId(plotAId)
                            .withVisible(false)
                            .build()
            );
            List<GameSave.ActorRecipe> actors = List.of(
                    new ActorRecipeBuilder()
                            .withId(playerId)
                            .withName("Player")
                            .withDescription("The hero")
                            .withOwnerId(plotAId)
                            .build(),
                    new ActorRecipeBuilder()
                            .withId(enemyId)
                            .withName("Bandit")
                            .withDescription("A lurking bandit")
                            .withOwnerId(plotAId)
                            .build()
            );

            GameSave save = new GameSave(1L, plotAId, "preamble", plots, gates, fixtures, items, actors);
            LoopRuntime loopRuntime = new LoopRuntime(save, LoopConfig.disabled());
            registry = loopRuntime.buildWorld().registry();

            TriggerEngine triggerEngine = new TriggerEngine(List.of(
                    new TriggerDefinition(
                            "use-keycard",
                            TriggerType.ON_USE,
                            "Keycard",
                            null,
                            "true",
                            List.of(new TriggerAction(TriggerActionType.MESSAGE, null, null, "It clicks.", null, null, null, null, null, null, null))
                    )
            ));

            runtime = new GameRuntime(new SceneNarrator(new NarrationService(false, "", false)), output::add, false);
            List<Item> inventory = new ArrayList<>(runtime.startingInventory(registry, playerId));
            runtime.seedInventoryPlacements(inventory, placements);
            runtime.configure(
                    registry,
                    plotAId,
                    playerId,
                    inventory,
                    placements,
                    loopRuntime,
                    triggerEngine,
                    Map.of(),
                    Map.of()
            );
        }

        private Map<UUID, Map<UUID, Rectangle2D>> runtimePlacements() {
            return placements;
        }

        private Item findItem(String label) {
            return registry.getEverything().values().stream()
                    .filter(Item.class::isInstance)
                    .map(Item.class::cast)
                    .filter(i -> label.equalsIgnoreCase(i.getLabel()))
                    .findFirst()
                    .orElseThrow();
        }

        private void openGate(String label) {
            registry.getEverything().values().stream()
                    .filter(v -> v instanceof com.demo.adventure.domain.model.Gate)
                    .map(v -> (com.demo.adventure.domain.model.Gate) v)
                    .filter(g -> label.equalsIgnoreCase(g.getLabel()))
                    .forEach(g -> g.setKey("true"));
        }

        private UUID plotAId() {
            return plotAId;
        }

        private UUID plotBId() {
            return plotBId;
        }

        private com.demo.adventure.domain.model.Gate findGate(String label) {
            return registry.getEverything().values().stream()
                    .filter(v -> v instanceof com.demo.adventure.domain.model.Gate)
                    .map(v -> (com.demo.adventure.domain.model.Gate) v)
                    .filter(g -> label.equalsIgnoreCase(g.getLabel()))
                    .findFirst()
                    .orElseThrow();
        }
    }
}

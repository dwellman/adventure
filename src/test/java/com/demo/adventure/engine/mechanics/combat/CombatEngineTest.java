package com.demo.adventure.engine.mechanics.combat;

import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.model.Thing;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CombatEngineTest {

    private static final UUID PLOT_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-000000000011");
    private static final UUID GOBLIN_ID = UUID.fromString("00000000-0000-0000-0000-000000000012");
    private static final UUID SWORD_ID = UUID.fromString("00000000-0000-0000-0000-000000000013");
    private static final UUID ARMOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000014");
    private static final Plot ARENA = new PlotBuilder()
            .withId(PLOT_ID)
            .withLabel("Arena")
            .withDescription("Arena")
            .build();

    @Test
    void attackDefeatsTargetAndRecordsReceipts() {
        KernelRegistry registry = new KernelRegistry();
        Actor player = actor(PLAYER_ID, "Player", 5, 5);
        Actor goblin = actor(GOBLIN_ID, "Goblin", 1, 1);
        registry.register(ARENA);
        registry.register(player);
        registry.register(goblin);

        CombatEncounter encounter = CombatEngine.startEncounter(registry, PLOT_ID, List.of(player, goblin), player.getId());
        withDiceRoll(20, () -> CombatEngine.attack(registry, encounter, player, goblin));
        String outcome = CombatEngine.checkEnd(registry, encounter, player.getId());

        assertThat(outcome).isEqualTo("VICTORY");
        assertThat(encounter.isDefeated(goblin.getId())).isTrue();

        List<Object> receipts = registry.getReceipts();
        assertThat(receipts).hasSize(7);
        assertThat(receipts.get(0)).isInstanceOf(CombatStartedReceipt.class);
        assertThat(receipts.get(1)).isInstanceOf(TurnStartedReceipt.class);
        assertThat(receipts.get(2)).isInstanceOf(DiceCheckedReceipt.class);
        assertThat(receipts.get(3)).isInstanceOf(CellMutationReceipt.class);
        assertThat(receipts.get(4)).isInstanceOf(AttackResolvedReceipt.class);
        assertThat(receipts.get(5)).isInstanceOf(ActorDefeatedReceipt.class);
        assertThat(receipts.get(6)).isInstanceOf(CombatEndedReceipt.class);
    }

    @Test
    void attackUsesWeaponAndArmorMitigation() {
        KernelRegistry registry = new KernelRegistry();
        Actor player = actor(PLAYER_ID, "Player", 10, 10);
        Actor goblin = actor(GOBLIN_ID, "Goblin", 10, 10);
        Item sword = weapon(SWORD_ID, "Sword", 4);
        Item armor = armor(ARMOR_ID, "Armor", 2);
        player.setEquippedMainHandItemId(sword.getId());
        goblin.setEquippedBodyItemId(armor.getId());
        registry.register(ARENA);
        registry.register(player);
        registry.register(goblin);
        registry.register(sword);
        registry.register(armor);

        CombatEncounter encounter = CombatEngine.startEncounter(registry, PLOT_ID, List.of(player, goblin), player.getId());
        CombatEngine.AttackOutcome outcome = withDiceRoll(20, () -> CombatEngine.attack(registry, encounter, player, goblin));

        assertThat(outcome.hit()).isTrue();
        assertThat(outcome.damageApplied()).isEqualTo(2);
        assertThat(outcome.targetAmount()).isEqualTo(8);
    }

    @Test
    void fleeSuccessEndsCombat() {
        KernelRegistry registry = new KernelRegistry();
        Actor player = actor(PLAYER_ID, "Player", 5, 5);
        Actor goblin = actor(GOBLIN_ID, "Goblin", 5, 5);
        registry.register(ARENA);
        registry.register(player);
        registry.register(goblin);

        CombatEncounter encounter = CombatEngine.startEncounter(registry, PLOT_ID, List.of(player, goblin), player.getId());
        CombatEngine.FleeOutcome outcome = withDiceRoll(20, () -> CombatEngine.flee(registry, encounter, player));

        assertThat(outcome.escaped()).isTrue();
        assertThat(encounter.isFled(player.getId())).isTrue();
        assertThat(CombatEngine.checkEnd(registry, encounter, player.getId())).isEqualTo("PLAYER_FLED");
    }

    @Test
    void attackMissRecordsReceiptsWithoutCellMutation() {
        KernelRegistry registry = new KernelRegistry();
        Actor player = actor(PLAYER_ID, "Player", 5, 5);
        Actor goblin = actor(GOBLIN_ID, "Goblin", 5, 5);
        registry.register(ARENA);
        registry.register(player);
        registry.register(goblin);

        CombatEncounter encounter = CombatEngine.startEncounter(registry, PLOT_ID, List.of(player, goblin), player.getId());
        CombatEngine.AttackOutcome outcome = withDiceRoll(1, () -> CombatEngine.attack(registry, encounter, player, goblin));

        assertThat(outcome.hit()).isFalse();
        assertThat(registry.getCellMutationReceipts()).isEmpty();
        assertThat(registry.getReceipts())
                .extracting(Object::getClass)
                .containsExactly(
                        CombatStartedReceipt.class,
                        TurnStartedReceipt.class,
                        DiceCheckedReceipt.class,
                        AttackResolvedReceipt.class
                );
    }

    @Test
    void attackKeepsHealthInBounds() {
        long[][] cases = new long[][]{
                {5L, 5L, 10L},
                {5L, 10L, 3L},
                {2L, 10L, 1L}
        };
        for (long[] testCase : cases) {
            long amount = testCase[0];
            long capacity = testCase[1];
            long damage = testCase[2];

            KernelRegistry registry = new KernelRegistry();
            Actor player = actor(PLAYER_ID, "Player", 5, 5);
            Actor goblin = actor(GOBLIN_ID, "Goblin", amount, capacity);
            Item sword = weapon(SWORD_ID, "Sword", damage);
            player.setEquippedMainHandItemId(sword.getId());
            registry.register(ARENA);
            registry.register(player);
            registry.register(goblin);
            registry.register(sword);

            CombatEncounter encounter = CombatEngine.startEncounter(registry, PLOT_ID, List.of(player, goblin), player.getId());
            CombatEngine.AttackOutcome outcome = withDiceRoll(20, () -> CombatEngine.attack(registry, encounter, player, goblin));

            assertThat(outcome.targetAmount()).isBetween(0L, capacity);
            assertThat(outcome.targetCapacity()).isEqualTo(capacity);
            assertThat(outcome.targetVolume()).isBetween(0.0, 1.0);
        }
    }

    @Test
    void receiptStreamMatchesMultiStepFlow() {
        KernelRegistry registry = new KernelRegistry();
        Actor player = actor(PLAYER_ID, "Player", 5, 5);
        Actor goblin = actor(GOBLIN_ID, "Goblin", 5, 5);
        registry.register(ARENA);
        registry.register(player);
        registry.register(goblin);

        CombatEncounter encounter = CombatEngine.startEncounter(registry, PLOT_ID, List.of(player, goblin), player.getId());
        withDiceRoll(20, () -> CombatEngine.attack(registry, encounter, player, goblin));
        CombatEngine.advanceTurn(registry, encounter);
        withDiceRoll(1, () -> CombatEngine.attack(registry, encounter, goblin, player));

        assertThat(registry.getReceipts())
                .extracting(Object::getClass)
                .containsExactly(
                        CombatStartedReceipt.class,
                        TurnStartedReceipt.class,
                        DiceCheckedReceipt.class,
                        CellMutationReceipt.class,
                        AttackResolvedReceipt.class,
                        TurnStartedReceipt.class,
                        DiceCheckedReceipt.class,
                        AttackResolvedReceipt.class
                );
    }

    private static Actor actor(UUID id, String label, long amount, long capacity) {
        Actor actor = new ActorBuilder()
                .withId(id)
                .withLabel(label)
                .withDescription(label)
                .withOwnerId(ARENA)
                .build();
        actor.setCells(Map.of(Thing.normalizeCellKey("HEALTH"), new CellSpec(capacity, amount).toCell()));
        return actor;
    }

    private static Item weapon(UUID id, String label, long damage) {
        Item item = new ItemBuilder()
                .withId(id)
                .withLabel(label)
                .withDescription(label)
                .withOwnerId(ARENA)
                .withWeaponDamage(damage)
                .build();
        return item;
    }

    private static Item armor(UUID id, String label, long mitigation) {
        Item item = new ItemBuilder()
                .withId(id)
                .withLabel(label)
                .withDescription(label)
                .withOwnerId(ARENA)
                .withArmorMitigation(mitigation)
                .build();
        return item;
    }

    private static <T> T withDiceRoll(int roll, java.util.concurrent.Callable<T> action) {
        KeyExpressionEvaluator.DiceRoller previous = KeyExpressionEvaluator.getDefaultDiceRoller();
        KeyExpressionEvaluator.setDefaultDiceRoller(sides -> roll);
        try {
            return action.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        } finally {
            KeyExpressionEvaluator.setDefaultDiceRoller(previous);
        }
    }
}

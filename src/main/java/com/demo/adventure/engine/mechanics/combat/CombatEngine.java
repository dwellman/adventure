package com.demo.adventure.engine.mechanics.combat;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.engine.mechanics.cells.CellMutationReason;
import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Thing;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public final class CombatEngine {

    private CombatEngine() {
    }

    public record AttackOutcome(
            boolean hit,
            long damageApplied,
            long targetAmount,
            long targetCapacity,
            double targetVolume,
            boolean targetDefeated
    ) {
    }

    public record FleeOutcome(boolean escaped) {
    }

    public static CombatEncounter startEncounter(
            KernelRegistry registry,
            UUID locationId,
            List<Actor> participants,
            UUID instigatorId
    ) {
        List<Actor> unique = uniqueActors(participants);
        List<Actor> ordered = new ArrayList<>(unique);
        ordered.sort(Comparator.comparing(CombatEngine::safeLabel)
                .thenComparing(Actor::getId, Comparator.nullsLast(UUID::compareTo)));
        List<UUID> initiative = ordered.stream()
                .map(Actor::getId)
                .toList();
        int startIndex = Math.max(0, initiative.indexOf(instigatorId));
        UUID encounterId = encounterId(locationId, initiative);
        CombatEncounter encounter = new CombatEncounter(encounterId, locationId, initiative, initiative, startIndex);
        record(registry, new CombatStartedReceipt(encounterId, locationId, encounter.getParticipants(), encounter.getInitiativeOrder()));
        recordTurnStart(registry, encounter);
        return encounter;
    }

    public static AttackOutcome attack(
            KernelRegistry registry,
            CombatEncounter encounter,
            Actor attacker,
            Actor target
    ) {
        DiceCheckResult hitCheck = evaluateDiceCheck(registry, "HIT", CombatRules.DICE_SIDES, CombatRules.HIT_MIN_ROLL, CombatRules.hitExpression());
        boolean hit = hitCheck.success();
        long damageApplied = 0L;
        long targetAmount = 0L;
        long targetCapacity = 0L;
        double targetVolume = 0.0;
        boolean defeated = false;

        if (hit) {
            long baseDamage = Math.max(0L, weaponDamage(registry, attacker));
            long mitigation = Math.max(0L, armorMitigation(registry, target));
            long requestedDamage = Math.max(0L, baseDamage - mitigation);
            CellMutationReceipt mutation = CellOps.consume(target, "HEALTH", requestedDamage);
            registry.recordCellMutation(mutation);
            damageApplied = mutation.appliedDelta() == null ? 0L : mutation.appliedDelta();
            targetAmount = mutation.afterAmount();
            targetVolume = mutation.afterVolume();
            Cell health = target.getCell(Thing.normalizeCellKey("HEALTH"));
            targetCapacity = health == null ? 0L : health.getCapacity();
            if (mutation.reason() != CellMutationReason.MISSING_CELL && targetAmount == 0L) {
                defeated = true;
            }
        }

        record(registry, new AttackResolvedReceipt(attacker.getId(), target.getId(), hit, damageApplied));
        if (defeated) {
            encounter.markDefeated(target.getId());
            record(registry, new ActorDefeatedReceipt(target.getId()));
        }
        return new AttackOutcome(hit, damageApplied, targetAmount, targetCapacity, targetVolume, defeated);
    }

    public static FleeOutcome flee(KernelRegistry registry, CombatEncounter encounter, Actor actor) {
        DiceCheckResult fleeCheck = evaluateDiceCheck(registry, "FLEE", CombatRules.DICE_SIDES, CombatRules.FLEE_MIN_ROLL, CombatRules.fleeExpression());
        if (fleeCheck.success()) {
            encounter.markFled(actor.getId());
            return new FleeOutcome(true);
        }
        return new FleeOutcome(false);
    }

    public static void recordTurnStart(KernelRegistry registry, CombatEncounter encounter) {
        if (registry == null || encounter == null) {
            return;
        }
        UUID actorId = encounter.currentActorId();
        if (actorId == null) {
            return;
        }
        record(registry, new TurnStartedReceipt(encounter.getId(), actorId, encounter.getTurnIndex()));
    }

    public static UUID advanceTurn(KernelRegistry registry, CombatEncounter encounter) {
        if (encounter == null || encounter.getState() != CombatState.ACTIVE) {
            return null;
        }
        UUID next = encounter.advanceTurn();
        if (next != null) {
            recordTurnStart(registry, encounter);
        }
        return next;
    }

    public static String checkEnd(KernelRegistry registry, CombatEncounter encounter, UUID playerId) {
        if (encounter == null || encounter.getState() != CombatState.ACTIVE) {
            return null;
        }
        boolean playerActive = encounter.isActiveParticipant(playerId);
        boolean opponentsRemain = encounter.activeParticipants().stream()
                .anyMatch(id -> playerId == null || !playerId.equals(id));
        if (!playerActive) {
            encounter.setState(CombatState.ENDED);
            String outcome = encounter.isFled(playerId) ? "PLAYER_FLED" : "PLAYER_DEFEATED";
            record(registry, new CombatEndedReceipt(encounter.getId(), outcome));
            return outcome;
        }
        if (!opponentsRemain) {
            encounter.setState(CombatState.ENDED);
            String outcome = "VICTORY";
            record(registry, new CombatEndedReceipt(encounter.getId(), outcome));
            return outcome;
        }
        return null;
    }

    private static DiceCheckResult evaluateDiceCheck(
            KernelRegistry registry,
            String contextTag,
            int sides,
            int minRoll,
            String expression
    ) {
        KeyExpressionEvaluator.DiceRoller previous = KeyExpressionEvaluator.getDefaultDiceRoller();
        List<Integer> rolls = new ArrayList<>();
        KeyExpressionEvaluator.setDefaultDiceRoller(s -> {
            int roll = previous.roll(s);
            rolls.add(roll);
            return roll;
        });
        boolean success;
        try {
            success = KeyExpressionEvaluator.evaluate(expression);
        } finally {
            KeyExpressionEvaluator.setDefaultDiceRoller(previous);
        }
        int result = rolls.isEmpty() ? 0 : rolls.get(rolls.size() - 1);
        record(registry, new DiceCheckedReceipt(contextTag, sides, minRoll, result));
        return new DiceCheckResult(result, success);
    }

    private static long weaponDamage(KernelRegistry registry, Actor actor) {
        if (actor == null) {
            return CombatRules.UNARMED_DAMAGE;
        }
        UUID weaponId = actor.getEquippedMainHandItemId();
        if (weaponId != null && registry != null) {
            Thing thing = registry.get(weaponId);
            if (thing instanceof Item item && item.getWeaponDamage() > 0) {
                return item.getWeaponDamage();
            }
        }
        return CombatRules.UNARMED_DAMAGE;
    }

    private static long armorMitigation(KernelRegistry registry, Actor actor) {
        if (actor == null) {
            return 0L;
        }
        UUID armorId = actor.getEquippedBodyItemId();
        if (armorId != null && registry != null) {
            Thing thing = registry.get(armorId);
            if (thing instanceof Item item) {
                return Math.max(0L, item.getArmorMitigation());
            }
        }
        return 0L;
    }

    private static String safeLabel(Actor actor) {
        String label = actor == null || actor.getLabel() == null ? "" : actor.getLabel().trim();
        return label.toLowerCase(Locale.ROOT);
    }

    private static List<Actor> uniqueActors(List<Actor> actors) {
        if (actors == null) {
            return List.of();
        }
        List<Actor> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();
        for (Actor actor : actors) {
            if (actor == null) {
                continue;
            }
            UUID id = actor.getId();
            if (id != null && seen.add(id)) {
                result.add(actor);
            }
        }
        return result;
    }

    private static UUID encounterId(UUID locationId, List<UUID> initiative) {
        StringBuilder sb = new StringBuilder("encounter:");
        sb.append(locationId == null ? "unknown" : locationId.toString());
        for (UUID id : initiative) {
            sb.append(':').append(id);
        }
        return UUID.nameUUIDFromBytes(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private static void record(KernelRegistry registry, CombatReceipt receipt) {
        if (registry != null && receipt != null) {
            registry.recordReceipt(receipt);
        }
    }

    private record DiceCheckResult(int roll, boolean success) {
    }
}

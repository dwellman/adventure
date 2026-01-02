package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.mechanics.combat.CombatEncounter;
import com.demo.adventure.engine.mechanics.combat.CombatEngine;
import com.demo.adventure.engine.mechanics.combat.CombatState;
import com.demo.adventure.engine.mechanics.combat.UnknownReferenceReceipt;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class RuntimeCombat {
    private final GameRuntime runtime;

    RuntimeCombat(GameRuntime runtime) {
        this.runtime = runtime;
    }

    boolean inCombat() {
        CombatEncounter encounter = runtime.encounter();
        return encounter != null && encounter.getState() == CombatState.ACTIVE;
    }

    void attack(String target) {
        if (target == null || target.isBlank()) {
            runtime.narrate("Attack what?");
            return;
        }
        KernelRegistry registry = runtime.registry();
        UUID playerId = runtime.playerId();
        UUID currentPlot = runtime.currentPlotId();
        Actor attacker = registry == null ? null : registry.get(playerId) instanceof Actor actor ? actor : null;
        if (attacker == null) {
            runtime.narrate("That cannot be done right now.");
            return;
        }
        Actor targetActor = runtime.findVisibleActorByLabel(registry, currentPlot, target);
        if (targetActor == null) {
            if (registry != null) {
                registry.recordReceipt(new UnknownReferenceReceipt(runtime.lastCommand(), target));
            }
            runtime.narrate("I don't know what that is.");
            return;
        }
        CombatEncounter encounter = runtime.encounter();
        if (encounter == null || encounter.getState() != CombatState.ACTIVE || !currentPlot.equals(encounter.getLocationId())) {
            List<Actor> participants = new ArrayList<>(runtime.visibleActorsAtPlot(registry, currentPlot));
            if (participants.stream().noneMatch(a -> attacker.getId().equals(a.getId()))) {
                participants.add(attacker);
            }
            if (participants.stream().noneMatch(a -> targetActor.getId().equals(a.getId()))) {
                participants.add(targetActor);
            }
            encounter = CombatEngine.startEncounter(registry, currentPlot, participants, attacker.getId());
            runtime.setEncounter(encounter);
            runtime.narrate("Combat begins.");
        }
        if (!attacker.getId().equals(encounter.currentActorId())) {
            runtime.narrate("It is not your turn.");
            return;
        }
        if (attacker.getId().equals(targetActor.getId())) {
            runtime.narrate("You can't attack yourself.");
            return;
        }
        if (!encounter.isActiveParticipant(targetActor.getId())) {
            runtime.narrate("They are not here.");
            return;
        }
        if (encounter.isDefeated(targetActor.getId())) {
            runtime.narrate("They are already defeated.");
            return;
        }
        CombatEngine.AttackOutcome outcome = CombatEngine.attack(registry, encounter, attacker, targetActor);
        narrateAttackOutcome(attacker, targetActor, outcome, playerId);
        if (outcome.targetDefeated()) {
            runtime.narrate(targetActor.getLabel() + " is defeated.");
        }
        String end = CombatEngine.checkEnd(registry, encounter, playerId);
        if (end != null) {
            narrateCombatEnd(end);
            runtime.setEncounter(null);
            return;
        }
        CombatEngine.advanceTurn(registry, encounter);
        resolveNpcTurns(registry, currentPlot, playerId);
    }

    void flee() {
        CombatEncounter encounter = runtime.encounter();
        UUID currentPlot = runtime.currentPlotId();
        if (encounter == null || encounter.getState() != CombatState.ACTIVE || !currentPlot.equals(encounter.getLocationId())) {
            runtime.narrate("You are not in combat.");
            return;
        }
        KernelRegistry registry = runtime.registry();
        UUID playerId = runtime.playerId();
        Actor actor = registry == null ? null : registry.get(playerId) instanceof Actor a ? a : null;
        if (actor == null) {
            runtime.narrate("That cannot be done right now.");
            return;
        }
        if (!actor.getId().equals(encounter.currentActorId())) {
            runtime.narrate("It is not your turn.");
            return;
        }
        CombatEngine.FleeOutcome outcome = CombatEngine.flee(registry, encounter, actor);
        if (outcome.escaped()) {
            runtime.narrate("You flee.");
        } else {
            runtime.narrate("You fail to flee.");
        }
        String end = CombatEngine.checkEnd(registry, encounter, playerId);
        if (end != null) {
            narrateCombatEnd(end);
            runtime.setEncounter(null);
            return;
        }
        CombatEngine.advanceTurn(registry, encounter);
        resolveNpcTurns(registry, currentPlot, playerId);
    }

    void resolveNpcTurns(KernelRegistry registry, UUID plotId, UUID playerId) {
        while (true) {
            CombatEncounter encounter = runtime.encounter();
            if (encounter == null || encounter.getState() != CombatState.ACTIVE) {
                return;
            }
            UUID actorId = encounter.currentActorId();
            if (actorId == null || actorId.equals(playerId)) {
                return;
            }
            if (runtime.smartActorRuntime() != null && runtime.smartActorRuntime().handlesActor(actorId)) {
                try {
                    runtime.smartActorRuntime().advanceCombatTurn(runtime, actorId);
                } catch (GameBuilderException ex) {
                    return;
                }
                if (runtime.encounter() == null || runtime.encounter().getState() != CombatState.ACTIVE) {
                    return;
                }
                continue;
            }
            Actor npc = registry == null ? null : registry.get(actorId) instanceof Actor actor ? actor : null;
            Actor player = registry == null ? null : registry.get(playerId) instanceof Actor actor ? actor : null;
            if (npc == null || player == null) {
                encounter.markDefeated(actorId);
                CombatEngine.advanceTurn(registry, encounter);
                continue;
            }
            CombatEngine.AttackOutcome outcome = CombatEngine.attack(registry, encounter, npc, player);
            narrateAttackOutcome(npc, player, outcome, playerId);
            if (outcome.targetDefeated()) {
                runtime.narrate(player.getLabel() + " is defeated.");
            }
            String end = CombatEngine.checkEnd(registry, encounter, playerId);
            if (end != null) {
                narrateCombatEnd(end);
                runtime.setEncounter(null);
                return;
            }
            CombatEngine.advanceTurn(registry, encounter);
        }
    }

    CommandOutcome resolveSmartActorCombatAction(UUID actorId, Command command) {
        CombatEncounter encounter = runtime.encounter();
        if (encounter == null || encounter.getState() != CombatState.ACTIVE) {
            return CommandOutcome.none();
        }
        KernelRegistry registry = runtime.registry();
        if (registry == null || actorId == null) {
            return CommandOutcome.none();
        }
        if (!actorId.equals(encounter.currentActorId())) {
            return CommandOutcome.none();
        }
        Actor actor = registry.get(actorId) instanceof Actor found ? found : null;
        if (actor == null) {
            encounter.markDefeated(actorId);
            CombatEngine.advanceTurn(registry, encounter);
            return CommandOutcome.none();
        }
        CommandAction action = command == null ? CommandAction.UNKNOWN : command.action();
        return switch (action) {
            case ATTACK -> resolveSmartActorAttack(actor, command.target());
            case FLEE -> resolveSmartActorFlee(actor);
            default -> resolveSmartActorPass(actor);
        };
    }

    private CommandOutcome resolveSmartActorAttack(Actor actor, String targetLabel) {
        KernelRegistry registry = runtime.registry();
        CombatEncounter encounter = runtime.encounter();
        if (actor == null || registry == null || encounter == null) {
            return CommandOutcome.none();
        }
        Actor targetActor = runtime.findVisibleActorByLabel(registry, runtime.currentPlotId(), targetLabel);
        if (targetActor == null) {
            return resolveSmartActorPass(actor);
        }
        if (actor.getId().equals(targetActor.getId())) {
            return resolveSmartActorPass(actor);
        }
        if (!encounter.isActiveParticipant(targetActor.getId())) {
            return resolveSmartActorPass(actor);
        }
        if (encounter.isDefeated(targetActor.getId())) {
            runtime.narrate(targetActor.getLabel() + " is already defeated.");
            CombatEngine.advanceTurn(registry, encounter);
            return CommandOutcome.none();
        }
        CombatEngine.AttackOutcome outcome = CombatEngine.attack(registry, encounter, actor, targetActor);
        narrateAttackOutcome(actor, targetActor, outcome, runtime.playerId());
        if (outcome.targetDefeated()) {
            runtime.narrate(targetActor.getLabel() + " is defeated.");
        }
        String end = CombatEngine.checkEnd(registry, encounter, runtime.playerId());
        if (end != null) {
            narrateCombatEnd(end);
            runtime.setEncounter(null);
            return CommandOutcome.none();
        }
        CombatEngine.advanceTurn(registry, encounter);
        return CommandOutcome.none();
    }

    private CommandOutcome resolveSmartActorFlee(Actor actor) {
        KernelRegistry registry = runtime.registry();
        CombatEncounter encounter = runtime.encounter();
        if (actor == null || registry == null || encounter == null) {
            return CommandOutcome.none();
        }
        CombatEngine.FleeOutcome outcome = CombatEngine.flee(registry, encounter, actor);
        String label = actor.getLabel() == null ? "Someone" : actor.getLabel();
        if (outcome.escaped()) {
            runtime.narrate(label + " flees.");
        } else {
            runtime.narrate(label + " fails to flee.");
        }
        String end = CombatEngine.checkEnd(registry, encounter, runtime.playerId());
        if (end != null) {
            narrateCombatEnd(end);
            runtime.setEncounter(null);
            return CommandOutcome.none();
        }
        CombatEngine.advanceTurn(registry, encounter);
        return CommandOutcome.none();
    }

    private CommandOutcome resolveSmartActorPass(Actor actor) {
        String label = actor == null || actor.getLabel() == null ? "Someone" : actor.getLabel();
        runtime.narrate(label + " hesitates.");
        CombatEncounter encounter = runtime.encounter();
        if (encounter != null && encounter.getState() == CombatState.ACTIVE) {
            CombatEngine.advanceTurn(runtime.registry(), encounter);
        }
        return CommandOutcome.none();
    }

    private void narrateAttackOutcome(Actor attacker, Actor target, CombatEngine.AttackOutcome outcome, UUID playerId) {
        String attackerLabel = attacker == null ? "Someone" : attacker.getLabel();
        String targetLabel = target == null ? "someone" : target.getLabel();
        if (!outcome.hit()) {
            if (attacker != null && attacker.getId().equals(playerId)) {
                runtime.narrate("You miss " + targetLabel + ".");
            } else if (target != null && target.getId().equals(playerId)) {
                runtime.narrate(attackerLabel + " misses you.");
            } else {
                runtime.narrate(attackerLabel + " misses " + targetLabel + ".");
            }
            return;
        }
        String health = formatHealth(outcome);
        if (attacker != null && attacker.getId().equals(playerId)) {
            runtime.narrate("You hit " + targetLabel + " for " + outcome.damageApplied() + ". " + targetLabel + " health: " + health + ".");
        } else if (target != null && target.getId().equals(playerId)) {
            runtime.narrate(attackerLabel + " hits you for " + outcome.damageApplied() + ". Your health: " + health + ".");
        } else {
            runtime.narrate(attackerLabel + " hits " + targetLabel + " for " + outcome.damageApplied() + ". " + targetLabel + " health: " + health + ".");
        }
    }

    private String formatHealth(CombatEngine.AttackOutcome outcome) {
        long capacity = outcome.targetCapacity();
        if (capacity <= 0) {
            return String.valueOf(outcome.targetAmount());
        }
        int pct = (int) Math.round(outcome.targetVolume() * 100.0);
        return outcome.targetAmount() + "/" + capacity + " (" + pct + "%)";
    }

    private void narrateCombatEnd(String outcome) {
        if (outcome == null) {
            runtime.narrate("Combat ends.");
            return;
        }
        switch (outcome) {
            case "PLAYER_FLED" -> runtime.narrate("You escape the fight.");
            case "PLAYER_DEFEATED" -> runtime.narrate("You are defeated.");
            case "VICTORY" -> runtime.narrate("You are victorious.");
            default -> runtime.narrate("Combat ends.");
        }
    }
}

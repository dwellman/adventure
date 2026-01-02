package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.flow.loop.LoopResetReason;
import com.demo.adventure.engine.flow.trigger.TriggerContext;
import com.demo.adventure.engine.flow.trigger.TriggerEvent;
import com.demo.adventure.engine.flow.trigger.TriggerOutcome;
import com.demo.adventure.engine.flow.trigger.TriggerType;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.support.exceptions.GameBuilderException;

final class RuntimeTriggers {
    private final GameRuntime runtime;

    RuntimeTriggers(GameRuntime runtime) {
        this.runtime = runtime;
    }

    TriggerOutcome fireTrigger(TriggerType type, Thing target, Thing object) {
        if (runtime.triggerEngine() == null || type == null || runtime.registry() == null) {
            return TriggerOutcome.empty();
        }
        String targetLabel = target == null ? "" : target.getLabel();
        String objectLabel = object == null ? "" : object.getLabel();
        TriggerEvent event = new TriggerEvent(
                type,
                targetLabel,
                objectLabel,
                target == null ? null : target.getId(),
                object == null ? null : object.getId()
        );
        TriggerContext context = new TriggerContext(
                runtime.registry(),
                runtime.currentPlotId(),
                runtime.playerId(),
                runtime.findWorldStateId(runtime.registry())
        );
        return runtime.triggerEngine().fire(event, context);
    }

    CommandOutcome resolveTriggerOutcome(TriggerOutcome outcome) throws GameBuilderException {
        TriggerResolution resolution = runtime.applyTriggerOutcome(outcome);
        if (resolution.endGame()) {
            return CommandOutcome.endGameOutcome();
        }
        ResetContext reset = resolution.reset();
        if (reset != null) {
            runtime.updateState(reset);
            return CommandOutcome.skipTurnAdvanceOutcome();
        }
        runtime.refreshInventory();
        return CommandOutcome.none();
    }

    void applyLoopResetIfNeeded(LoopResetReason reason, String message) throws GameBuilderException {
        if (reason == null) {
            return;
        }
        ResetContext reset = runtime.applyLoopReset(reason, message);
        if (reset != null) {
            runtime.updateState(reset);
        }
    }

    CommandOutcome advanceTurn() throws GameBuilderException {
        var currentPlot = runtime.currentPlot();
        TriggerOutcome turnOutcome = fireTrigger(TriggerType.ON_TURN, currentPlot, null);
        CommandOutcome turnResolution = resolveTriggerOutcome(turnOutcome);
        if (turnResolution.endGame() || turnResolution.skipTurnAdvance()) {
            return turnResolution;
        }

        LoopResetReason resetReason = runtime.loopRuntime() == null ? null : runtime.loopRuntime().advanceTurn(runtime.registry());
        if (resetReason != null) {
            ResetContext reset = runtime.applyLoopReset(resetReason, "");
            if (reset != null) {
                runtime.updateState(reset);
                return CommandOutcome.skipTurnAdvanceOutcome();
            }
        }
        if (runtime.smartActorRuntime() != null) {
            CommandOutcome smartOutcome = runtime.smartActorRuntime().advanceTurn(runtime);
            if (smartOutcome.endGame() || smartOutcome.skipTurnAdvance()) {
                return smartOutcome;
            }
        }
        return CommandOutcome.none();
    }

    void updateState(ResetContext reset) {
        runtime.updateState(reset);
    }

    void refreshInventory() {
        runtime.refreshInventory();
    }
}

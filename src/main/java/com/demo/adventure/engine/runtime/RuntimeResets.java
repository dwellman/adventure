package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.engine.flow.loop.LoopResetReason;
import com.demo.adventure.engine.flow.loop.LoopResetResult;
import com.demo.adventure.engine.flow.trigger.TriggerOutcome;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class RuntimeResets {
    private final GameRuntime runtime;

    RuntimeResets(GameRuntime runtime) {
        this.runtime = runtime;
    }

    TriggerResolution applyTriggerOutcome(TriggerOutcome outcome) throws GameBuilderException {
        if (outcome == null) {
            return TriggerResolution.none();
        }
        for (String message : outcome.messages()) {
            if (message != null && !message.isBlank()) {
                runtime.narrate(message);
            }
        }
        if (outcome.endGame()) {
            return new TriggerResolution(null, true);
        }
        if (outcome.hasReset()) {
            ResetContext reset = applyLoopReset(outcome.resetReason(), outcome.resetMessage());
            return new TriggerResolution(reset, false);
        }
        return TriggerResolution.none();
    }

    ResetContext applyLoopReset(LoopResetReason reason, String overrideMessage) throws GameBuilderException {
        if (runtime.loopRuntime() == null || reason == null) {
            return null;
        }
        LoopResetResult reset = runtime.loopRuntime().reset(runtime.registry(), reason);
        KernelRegistry nextRegistry = reset.world().registry();
        UUID nextPlot = reset.world().startPlotId();
        UUID nextPlayer = runtime.findPlayerActor(nextRegistry, nextPlot);
        List<Item> nextInventory = new ArrayList<>(runtime.startingInventory(nextRegistry, nextPlayer));
        runtime.inventoryPlacements().clear();
        runtime.seedInventoryPlacements(nextInventory, runtime.inventoryPlacements());
        runtime.setEncounter(null);
        runtime.resetNarratorScene();
        String message = (overrideMessage != null && !overrideMessage.isBlank()) ? overrideMessage : reset.message();
        if (message != null && !message.isBlank()) {
            runtime.narrate(message);
        }
        runtime.updateState(new ResetContext(nextRegistry, nextPlot, nextPlayer, nextInventory));
        runtime.describe();
        return new ResetContext(nextRegistry, nextPlot, nextPlayer, nextInventory);
    }
}

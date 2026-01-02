package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class RuntimeActorContext {
    private final GameRuntime runtime;

    RuntimeActorContext(GameRuntime runtime) {
        this.runtime = runtime;
    }

    <T> T runAsActor(UUID actorId, boolean suppressOutput, boolean updateOwner, GameRuntime.ActorAction<T> action)
            throws GameBuilderException {
        KernelRegistry registry = runtime.registry();
        if (registry == null || actorId == null || action == null) {
            return null;
        }
        Actor actor = registry.get(actorId) instanceof Actor found ? found : null;
        if (actor == null) {
            return null;
        }
        UUID savedPlayerId = runtime.playerId();
        UUID savedPlot = runtime.currentPlotId();
        List<Item> savedInventory = runtime.inventory();
        Map<UUID, Map<UUID, Rectangle2D>> savedPlacements = runtime.inventoryPlacements();
        boolean savedSuppress = runtime.isOutputSuppressed();
        KernelRegistry savedRegistry = registry;

        UUID actorPlot = actor.getOwnerId();
        List<Item> actorInventory = runtime.startingInventory(registry, actorId);
        Map<UUID, Map<UUID, Rectangle2D>> actorPlacements = new HashMap<>();

        runtime.updateState(new ResetContext(registry, actorPlot, actorId, actorInventory));
        runtime.replaceInventoryPlacements(actorPlacements);
        runtime.setOutputSuppressed(suppressOutput);
        runtime.seedInventoryPlacements(actorInventory, actorPlacements);

        T result = action.run();

        UUID actorPlotAfter = runtime.currentPlotId();
        boolean registryChanged = runtime.registry() != savedRegistry;
        if (!registryChanged && updateOwner && actorPlotAfter != null) {
            registry.moveOwnership(actorId, actorPlotAfter);
        }

        runtime.setOutputSuppressed(savedSuppress);
        if (!registryChanged) {
            runtime.updateState(new ResetContext(savedRegistry, savedPlot, savedPlayerId, savedInventory));
            runtime.replaceInventoryPlacements(savedPlacements);
        }
        return result;
    }
}

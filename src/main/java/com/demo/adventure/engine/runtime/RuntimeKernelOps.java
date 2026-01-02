package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.model.ThingKind;
import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;

import java.util.UUID;

final class RuntimeKernelOps {
    private final GameRuntime runtime;

    RuntimeKernelOps(GameRuntime runtime) {
        this.runtime = runtime;
    }

    void consumeCell(KernelRegistry registry, UUID thingId, String cellName, long delta) {
        if (registry == null || thingId == null || cellName == null || cellName.isBlank()) {
            return;
        }
        Thing thing = registry.get(thingId);
        if (thing == null) {
            return;
        }
        CellMutationReceipt receipt = CellOps.consume(thing, cellName, delta);
        registry.recordCellMutation(receipt);
    }

    UUID findWorldStateId(KernelRegistry registry) {
        if (registry == null) {
            return null;
        }
        return registry.getEverything().values().stream()
                .filter(t -> t != null && t.getKind() == ThingKind.WORLD)
                .map(Thing::getId)
                .findFirst()
                .orElse(null);
    }

    boolean isGateOpen(Gate gate, KernelRegistry registry, UUID playerId, UUID plotId) {
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, plotId, playerId);
        return gate.isOpen(
                KeyExpressionEvaluator.registryHasResolver(registry, playerId),
                KeyExpressionEvaluator.registrySearchResolver(registry, playerId),
                attributeResolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
    }

    boolean isThingOpen(Thing thing, KernelRegistry registry, UUID playerId, UUID plotId) {
        if (thing == null) {
            return false;
        }
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, plotId, playerId);
        return thing.isOpen(
                KeyExpressionEvaluator.registryHasResolver(registry, playerId),
                KeyExpressionEvaluator.registrySearchResolver(registry, playerId),
                attributeResolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
    }
}

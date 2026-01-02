package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.smart.SmartActorWorldSnapshot;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

final class SmartActorSnapshotBuilder {
    private static final int RECEIPT_LIMIT = 6;

    SmartActorWorldSnapshot build(GameRuntime runtime, UUID actorId) throws GameBuilderException {
        return build(runtime, actorId, "");
    }

    SmartActorWorldSnapshot build(GameRuntime runtime, UUID actorId, String playerUtterance) throws GameBuilderException {
        return runtime.runAsActor(actorId, true, false, () -> {
            Actor actor = runtime.registry().get(actorId) instanceof Actor found ? found : null;
            Plot plot = runtime.currentPlot();
            String actorLabel = actor == null ? "" : Objects.toString(actor.getLabel(), "");
            String actorDescription = actor == null ? "" : Objects.toString(actor.getDescription(), "");
            String plotLabel = plot == null ? "" : Objects.toString(plot.getLabel(), "");
            String plotDescription = plot == null ? "" : Objects.toString(plot.getDescription(), "");
            List<String> fixtures = runtime.visibleFixtureLabels();
            List<String> items = runtime.visibleItemLabels();
            List<String> actors = runtime.visibleActorLabels(actorId);
            List<String> inventory = runtime.inventoryLabels();
            List<String> exits = exitsFor(runtime.registry(), runtime.currentPlotId());
            String lastScene = runtime.lastSceneState();
            List<String> receipts = recentReceipts(runtime.registry());
            return new SmartActorWorldSnapshot(
                    actorLabel,
                    actorDescription,
                    plotLabel,
                    plotDescription,
                    fixtures,
                    items,
                    actors,
                    inventory,
                    exits,
                    lastScene,
                    playerUtterance,
                    receipts
            );
        });
    }

    List<String> exitsFor(KernelRegistry registry, UUID plotId) {
        if (registry == null || plotId == null) {
            return List.of();
        }
        List<String> exits = new ArrayList<>();
        for (Object value : registry.getEverything().values()) {
            if (!(value instanceof Gate gate)) {
                continue;
            }
            if (!gate.isVisible() || !gate.connects(plotId)) {
                continue;
            }
            Direction direction = gate.directionFrom(plotId);
            if (direction != null) {
                exits.add(direction.toLongName());
            }
        }
        return exits.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    List<String> recentReceipts(KernelRegistry registry) {
        if (registry == null) {
            return List.of();
        }
        List<Object> receipts = registry.getReceipts();
        if (receipts.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, receipts.size() - RECEIPT_LIMIT);
        return receipts.subList(start, receipts.size()).stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }
}

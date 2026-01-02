package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.ops.Open;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class RuntimeCommandActions {
    private final GameRuntime runtime;

    RuntimeCommandActions(GameRuntime runtime) {
        this.runtime = runtime;
    }

    void explore() {
        KernelRegistry registry = runtime.registry();
        UUID playerId = runtime.playerId();
        UUID currentPlot = runtime.currentPlotId();
        runtime.consumeCell(registry, playerId, "STAMINA", 1);
        boolean success = KeyExpressionEvaluator.evaluate(
                "DICE(6) >= 4",
                KeyExpressionEvaluator.registryHasResolver(registry, currentPlot),
                null,
                null,
                KeyExpressionEvaluator.registryAttributeResolver(registry, currentPlot),
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
        // Hidden items and actors at this plot are discoverable via SEARCH (used to reveal things like hidden hatchet or Scratch).
        List<Item> hiddenItems = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> !item.isVisible())
                .filter(item -> currentPlot.equals(item.getOwnerId()))
                .toList();

        List<Actor> hiddenActors = registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(actor -> !actor.isVisible())
                .filter(actor -> currentPlot.equals(actor.getOwnerId()))
                .toList();

        if (success && (!hiddenItems.isEmpty() || !hiddenActors.isEmpty())) {
            if (!hiddenItems.isEmpty()) {
                Item reveal = hiddenItems.get(0);
                reveal.setVisible(true);
                runtime.narrate("You rummage around and uncover: " + reveal.getLabel());
            }
            if (!hiddenActors.isEmpty()) {
                hiddenActors.forEach(a -> a.setVisible(true));
                if (hiddenItems.isEmpty()) {
                    runtime.narrate("Something stirs nearby...");
                }
            }
        } else if (success) {
            String breadcrumb = runtime.firstExitDirection();
            if (breadcrumb.isBlank()) {
                runtime.narrate("You scour the area but nothing new turns up.");
            } else {
                runtime.narrate("You scour the area; tracks point " + breadcrumb + ".");
            }
        } else {
            runtime.narrate("You poke around and stir up dust, but find nothing useful.");
        }
    }

    void open(String target) {
        if (target == null || target.isBlank()) {
            runtime.narrate("Open what?");
            return;
        }
        KernelRegistry registry = runtime.registry();
        UUID currentPlot = runtime.currentPlotId();
        UUID playerId = runtime.playerId();
        String trimmed = target.trim();
        Direction dir = runtime.parseDirection(trimmed);
        if (dir != null) {
            Gate gate = runtime.exits().stream()
                    .filter(g -> dir.equals(g.directionFrom(currentPlot)))
                    .findFirst()
                    .orElse(null);
            if (gate == null) {
                runtime.narrate("No door that way.");
                return;
            }
            if (runtime.isGateOpen(gate, registry, playerId, currentPlot)) {
                runtime.narrate("It's already open.");
            } else {
                runtime.narrate("It won't open.");
            }
            return;
        }
        String targetLower = trimmed.toLowerCase(Locale.ROOT);
        Gate gateByLabel = runtime.exits().stream()
                .filter(g -> g.getLabel() != null && g.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (gateByLabel != null) {
            if (runtime.isGateOpen(gateByLabel, registry, playerId, currentPlot)) {
                runtime.narrate("It's already open.");
            } else {
                runtime.narrate("It won't open.");
            }
            return;
        }

        Item item = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isVisible)
                .filter(i -> currentPlot.equals(i.getOwnerId()) || playerId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (item != null) {
            if (runtime.isThingOpen(item, registry, playerId, currentPlot)) {
                runtime.narrate("It's already open.");
            } else {
                String result = Open.open(item);
                runtime.narrate(result.isBlank() ? "You open it." : result);
            }
            return;
        }

        runtime.narrate("You don't see that here.");
    }

    void put(String target, String object) {
        if (target == null || target.isBlank()) {
            runtime.narrate("Put what?");
            return;
        }
        if (object != null && !object.isBlank()) {
            runtime.narrate("You can't put items into containers yet.");
            return;
        }
        runtime.drop(target);
    }
}

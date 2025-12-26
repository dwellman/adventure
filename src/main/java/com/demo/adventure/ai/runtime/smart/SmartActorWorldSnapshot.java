package com.demo.adventure.ai.runtime.smart;

import java.util.List;

public record SmartActorWorldSnapshot(
        String actorLabel,
        String actorDescription,
        String plotLabel,
        String plotDescription,
        List<String> visibleFixtures,
        List<String> visibleItems,
        List<String> visibleActors,
        List<String> inventory,
        List<String> exits,
        String lastScene,
        List<String> receipts
) {
    public SmartActorWorldSnapshot {
        actorLabel = actorLabel == null ? "" : actorLabel.trim();
        actorDescription = actorDescription == null ? "" : actorDescription.trim();
        plotLabel = plotLabel == null ? "" : plotLabel.trim();
        plotDescription = plotDescription == null ? "" : plotDescription.trim();
        visibleFixtures = visibleFixtures == null ? List.of() : List.copyOf(visibleFixtures);
        visibleItems = visibleItems == null ? List.of() : List.copyOf(visibleItems);
        visibleActors = visibleActors == null ? List.of() : List.copyOf(visibleActors);
        inventory = inventory == null ? List.of() : List.copyOf(inventory);
        exits = exits == null ? List.of() : List.copyOf(exits);
        lastScene = lastScene == null ? "" : lastScene.trim();
        receipts = receipts == null ? List.of() : List.copyOf(receipts);
    }
}

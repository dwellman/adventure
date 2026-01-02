package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.ContainerPacker;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Rectangle2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class RuntimeInventory {
    private final GameRuntime runtime;

    RuntimeInventory(GameRuntime runtime) {
        this.runtime = runtime;
    }

    List<String> inventoryLabels() {
        List<Item> inventory = runtime.inventory();
        if (inventory == null || inventory.isEmpty()) {
            return List.of();
        }
        return inventory.stream()
                .map(Item::getLabel)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    Item take(String name) {
        if (name.isBlank()) {
            runtime.narrate("Take what?");
            return null;
        }
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        UUID playerId = runtime.playerId();
        List<Item> inventory = runtime.inventory();
        Map<UUID, Map<UUID, Rectangle2D>> placements = runtime.inventoryPlacements();

        Item item = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> !i.isFixture())
                .filter(Item::isVisible)
                .filter(i -> plotId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (item == null) {
            item = runtime.itemsInOpenFixturesAtPlot().stream()
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }
        if (item == null) {
            runtime.narrate("No such item here.");
            return null;
        }
        Item container = primaryContainer(inventory);
        if (!fitsInventory(item, container, placements, playerId)) {
            String destLabel = container == null ? "pack" : container.getLabel();
            runtime.narrate("You can't fit the " + item.getLabel() + " in your " + destLabel + ".");
            return null;
        }
        registry.moveOwnership(item.getId(), playerId);
        inventory.add(item);
        runtime.narrate("You take the " + item.getLabel() + ".");
        return item;
    }

    void drop(String name) {
        if (name.isBlank()) {
            runtime.narrate("Drop what?");
            return;
        }
        List<Item> inventory = runtime.inventory();
        Map<UUID, Map<UUID, Rectangle2D>> placements = runtime.inventoryPlacements();
        Item item = inventory.stream()
                .filter(i -> i.getLabel().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (item == null) {
            runtime.narrate("You aren't carrying that.");
            return;
        }
        inventory.remove(item);
        placements.values().forEach(m -> m.remove(item.getId()));
        runtime.registry().moveOwnership(item.getId(), runtime.currentPlotId());
        item.setVisible(true);
        runtime.narrate("You drop the " + item.getLabel() + ".");
    }

    void showInventory() {
        List<Item> inventory = runtime.inventory();
        if (inventory.isEmpty()) {
            runtime.emit("You are carrying nothing.");
            return;
        }
        runtime.emit("You are carrying:");
        // Build a lookup of ownerId -> contained items so container contents show even if not listed as inventory items.
        Map<UUID, List<Item>> contained = runtime.registry().getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> i.getOwnerId() != null)
                .collect(java.util.stream.Collectors.groupingBy(Item::getOwnerId));

        Map<UUID, Map<UUID, Rectangle2D>> placements = runtime.inventoryPlacements();
        for (Item item : inventory) {
            List<Item> contents = contained.getOrDefault(item.getId(), List.of());
            String inner = contents.stream()
                    .map(Item::getLabel)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            String capacityNote = "";
            if (item.getCapacityWidth() > 0 && item.getCapacityHeight() > 0) {
                double used = capacityUsed(item, placements);
                capacityNote = " [space: " + fmtPct(used) + "]";
            }
            runtime.emit("- " + item.getLabel() + capacityNote + (inner.isEmpty() ? "" : " (contains: " + inner + ")"));
        }
    }

    void refreshInventory() {
        KernelRegistry registry = runtime.registry();
        UUID playerId = runtime.playerId();
        List<Item> inventory = runtime.inventory();
        Map<UUID, Map<UUID, Rectangle2D>> placements = runtime.inventoryPlacements();
        if (registry == null || playerId == null) {
            inventory.clear();
            placements.clear();
            return;
        }
        Map<UUID, Item> ownedItems = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> !i.isFixture())
                .filter(i -> playerId.equals(i.getOwnerId()))
                .collect(Collectors.toMap(Item::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        if (ownedItems.isEmpty()) {
            inventory.clear();
            placements.clear();
            return;
        }
        List<Item> refreshed = new ArrayList<>();
        for (Item item : inventory) {
            if (item == null) {
                continue;
            }
            Item owned = ownedItems.remove(item.getId());
            if (owned != null) {
                refreshed.add(owned);
            }
        }
        refreshed.addAll(ownedItems.values());
        inventory.clear();
        inventory.addAll(refreshed);
        placements.clear();
        seedInventoryPlacements(inventory, placements, playerId);
    }

    void seedInventoryPlacements(List<Item> inventory, Map<UUID, Map<UUID, Rectangle2D>> placements, UUID playerId) {
        Item container = primaryContainer(inventory);
        for (Item item : inventory) {
            if (container != null && item.getId().equals(container.getId())) {
                continue;
            }
            fitsInventory(item, container, placements, playerId);
        }
    }

    List<Item> startingInventory(KernelRegistry registry, UUID playerId) {
        if (registry == null || playerId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> playerId.equals(i.getOwnerId()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private Item primaryContainer(List<Item> inventory) {
        return inventory.stream()
                .filter(i -> i.getCapacityWidth() > 0 && i.getCapacityHeight() > 0)
                .max(java.util.Comparator.comparingDouble(i -> i.getCapacityWidth() * i.getCapacityHeight()))
                .orElse(null);
    }

    private boolean fitsInventory(Item item, Item container, Map<UUID, Map<UUID, Rectangle2D>> placements, UUID playerId) {
        double capacityWidth = container == null ? 0.3 : container.getCapacityWidth();
        double capacityHeight = container == null ? 0.3 : container.getCapacityHeight();
        UUID bucket = container == null ? (playerId == null ? GameRuntime.PLAYER_ID : playerId) : container.getId();

        Map<UUID, Rectangle2D> bucketPlacements = placements.computeIfAbsent(bucket, k -> new HashMap<>());
        List<Rectangle2D> occupied = new ArrayList<>(bucketPlacements.values());

        double width = normalizeFootprint(item.getFootprintWidth(), capacityWidth);
        double height = normalizeFootprint(item.getFootprintHeight(), capacityHeight);

        var placement = ContainerPacker.place(width, height, occupied);
        if (placement.isEmpty()) {
            return false;
        }
        bucketPlacements.put(item.getId(), placement.get().asRectangle());
        return true;
    }

    private double normalizeFootprint(double value, double capacity) {
        double v = Math.max(0.01, value);
        double cap = capacity <= 0 ? 1.0 : capacity;
        return Math.min(1.0, v / cap);
    }

    private String fmtPct(double used) {
        int pct = (int) Math.round(used * 100.0);
        return pct + "%";
    }

    private double capacityUsed(Item container, Map<UUID, Map<UUID, Rectangle2D>> placements) {
        UUID bucket = container.getId();
        Map<UUID, Rectangle2D> entries = placements.get(bucket);
        if (entries == null || entries.isEmpty()) {
            return 0.0;
        }
        double used = entries.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Rectangle2D::area)
                .sum();
        return Math.min(1.0, used);
    }
}

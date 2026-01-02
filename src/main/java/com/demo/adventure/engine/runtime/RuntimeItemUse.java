package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.engine.mechanics.cells.CellTransferResult;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Thing;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class RuntimeItemUse {
    private final GameRuntime runtime;

    RuntimeItemUse(GameRuntime runtime) {
        this.runtime = runtime;
    }

    UseResult use(String target, String preposition, String object) {
        if (target == null || target.isBlank()) {
            runtime.narrate("Use what?");
            return UseResult.invalid();
        }
        KernelRegistry registry = runtime.registry();
        UUID currentPlot = runtime.currentPlotId();
        UUID playerId = runtime.playerId();
        String trimmed = target.trim();
        Thing source = findThingByLabel(registry, currentPlot, playerId, trimmed);
        if (source == null) {
            runtime.narrate("You don't see that here.");
            return UseResult.invalid();
        }
        if (object != null && !object.isBlank()) {
            String destLabel = object.trim();
            Thing dest = findThingByLabel(registry, currentPlot, playerId, destLabel);
            if (dest == null) {
                runtime.narrate("You don't see that here.");
                return UseResult.invalid();
            }
            int transferred = transferAllCells(registry, source, dest);
            if (transferred == 0) {
                String prep = preposition == null ? "on" : preposition;
                runtime.narrate("You try to use " + trimmed + " " + prep + " " + destLabel + ", but nothing happens.");
            } else {
                runtime.narrate("You use " + trimmed + " on " + destLabel + ".");
            }
            return new UseResult(source, dest, true);
        }
        if (source.getCells().isEmpty()) {
            runtime.narrate("You try to use " + trimmed + ", but nothing happens.");
            return new UseResult(source, null, true);
        }
        if (source.getCells().size() > 1) {
            runtime.narrate("Use " + trimmed + " on what?");
            return UseResult.invalid();
        }
        var entry = source.getCells().entrySet().iterator().next();
        CellMutationReceipt receipt = CellOps.consume(source, entry.getKey(), 1);
        registry.recordCellMutation(receipt);
        runtime.narrate("You use " + trimmed + ".");
        return new UseResult(source, null, true);
    }

    private Thing findThingByLabel(KernelRegistry registry, UUID plotId, UUID playerId, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        String targetLower = label.trim().toLowerCase(Locale.ROOT);
        Thing player = playerId == null ? null : registry.get(playerId);
        if (player != null && player.getLabel() != null && player.getLabel().equalsIgnoreCase(targetLower)) {
            return player;
        }

        Thing inventoryItem = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> playerId != null && playerId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (inventoryItem != null) {
            return inventoryItem;
        }

        Thing plotItem = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> !i.isFixture())
                .filter(Item::isVisible)
                .filter(i -> plotId != null && plotId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (plotItem != null) {
            return plotItem;
        }

        Thing fixture = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(Item::isVisible)
                .filter(i -> plotId != null && plotId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (fixture != null) {
            return fixture;
        }

        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(a -> plotId != null && plotId.equals(a.getOwnerId()))
                .filter(a -> a.getLabel() != null && a.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
    }

    private int transferAllCells(KernelRegistry registry, Thing source, Thing dest) {
        if (registry == null || source == null || dest == null) {
            return 0;
        }
        if (source.getCells() == null || source.getCells().isEmpty()) {
            return 0;
        }
        int transferred = 0;
        for (Map.Entry<String, Cell> entry : source.getCells().entrySet()) {
            String cellName = entry.getKey();
            Cell cell = entry.getValue();
            if (cell == null || cell.getAmount() <= 0) {
                continue;
            }
            if (dest.getCell(cellName) == null) {
                dest.setCell(cellName, new Cell(cell.getCapacity(), 0));
            }
            CellTransferResult result = CellOps.transfer(source, dest, cellName, cell.getAmount());
            if (result != null) {
                registry.recordCellMutation(result.fromReceipt());
                registry.recordCellMutation(result.toReceipt());
                registry.recordCellTransfer(result.transferReceipt());
                if (result.transferReceipt().appliedDelta() > 0) {
                    transferred++;
                }
            }
        }
        return transferred;
    }
}

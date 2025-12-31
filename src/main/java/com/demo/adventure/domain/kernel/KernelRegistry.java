package com.demo.adventure.domain.kernel;

import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.engine.mechanics.cells.CellReferenceReceipt;
import com.demo.adventure.engine.mechanics.cells.CellTransferReceipt;
import com.demo.adventure.engine.mechanics.crafting.CraftingLog;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Thing;

import java.util.*;
import java.util.logging.Logger;

public final class KernelRegistry {

    public static final UUID MILIARIUM = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final Map<UUID, Thing> everything = new HashMap<>();
    private final Map<UUID, Set<UUID>> ownershipIndex = new HashMap<>();
    private final List<CellMutationReceipt> cellMutationReceipts = new ArrayList<>();
    private final List<CellReferenceReceipt> cellReferenceReceipts = new ArrayList<>();
    private final List<CellTransferReceipt> cellTransferReceipts = new ArrayList<>();
    private final List<Object> receipts = new ArrayList<>();
    private final Logger log = CraftingLog.get();

    public void register(Thing thing) {
        if (thing == null) {
            return;
        }
        everything.put(thing.getId(), thing);
        moveOwnership(thing.getId(), thing.getOwnerId());
        log.info(() -> "Register: " + label(thing) + " owner=" + thing.getOwnerId());
    }

    public Thing get(UUID id) {
        return everything.get(id);
    }

    public void moveOwnership(UUID thingId, UUID newOwnerId) {
        Thing thing = everything.get(thingId);
        if (thing == null) {
            return;
        }
        UUID source = thing.getOwnerId();
        if (Objects.equals(source, newOwnerId)) {
            return;
        }
        if (source != null) {
            ownershipIndex.computeIfPresent(source, (k, v) -> {
                v.remove(thingId);
                return v.isEmpty() ? null : v;
            });
        }
        if (newOwnerId != null) {
            ownershipIndex.computeIfAbsent(newOwnerId, k -> new HashSet<>()).add(thingId);
        }
        thing.setOwnerId(newOwnerId);
        log.info(() -> "Move: " + label(thing) + " source=" + source + " target=" + newOwnerId);
    }

    public List<Gate> findGates(UUID sourcePlotId, Direction direction) {
        List<Gate> gates = new ArrayList<>();
        if (sourcePlotId == null) {
            return gates;
        }
        for (Thing t : everything.values()) {
            if (t instanceof Gate gate) {
                Direction directionFromSource = gate.directionFrom(sourcePlotId);
                if (directionFromSource == null) {
                    continue;
                }
                if (direction == null
                        || direction == Direction.PORTAL
                        || directionFromSource == Direction.PORTAL
                        || direction == directionFromSource) {
                    gates.add(gate);
                }
            }
        }
        return gates;
    }

    public Map<UUID, Thing> getEverything() {
        return Collections.unmodifiableMap(everything);
    }

    public Map<UUID, Set<UUID>> getPlotInventoryIndex() {
        return Collections.unmodifiableMap(ownershipIndex);
    }

    public void recordCellMutation(CellMutationReceipt receipt) {
        if (receipt != null) {
            cellMutationReceipts.add(receipt);
            receipts.add(receipt);
        }
    }

    public void recordCellTransfer(CellTransferReceipt receipt) {
        if (receipt != null) {
            cellTransferReceipts.add(receipt);
            receipts.add(receipt);
        }
    }

    public void recordCellReference(CellReferenceReceipt receipt) {
        if (receipt != null) {
            cellReferenceReceipts.add(receipt);
            receipts.add(receipt);
        }
    }

    public void recordReceipt(Object receipt) {
        if (receipt != null) {
            receipts.add(receipt);
        }
    }

    public List<CellMutationReceipt> getCellMutationReceipts() {
        return Collections.unmodifiableList(cellMutationReceipts);
    }

    public List<CellTransferReceipt> getCellTransferReceipts() {
        return Collections.unmodifiableList(cellTransferReceipts);
    }

    public List<CellReferenceReceipt> getCellReferenceReceipts() {
        return Collections.unmodifiableList(cellReferenceReceipts);
    }

    public List<Object> getReceipts() {
        return Collections.unmodifiableList(receipts);
    }

    private static String label(Thing thing) {
        return thing == null ? "null" : thing.getLabel();
    }
}

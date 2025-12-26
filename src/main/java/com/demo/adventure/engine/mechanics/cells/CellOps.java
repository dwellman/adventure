package com.demo.adventure.engine.mechanics.cells;

import com.demo.adventure.domain.model.Thing;

import java.util.UUID;

public final class CellOps {
    private CellOps() {
    }

    public static CellReadResult read(Thing thing, String cellName) {
        String key = normalizeCellName(cellName);
        if (thing == null || key.isBlank()) {
            return CellReadResult.missing(key);
        }
        Cell cell = thing.getCell(key);
        return CellReadResult.of(key, cell);
    }

    public static CellMutationReceipt setAmount(Thing thing, String cellName, long value) {
        String key = normalizeCellName(cellName);
        if (thing == null || key.isBlank()) {
            return missingReceipt(thing, key, null);
        }
        Cell cell = thing.getCell(key);
        if (cell == null) {
            return missingReceipt(thing, key, null);
        }
        long beforeAmount = cell.getAmount();
        double beforeVolume = cell.getVolume();
        long clamped = clamp(value, 0, cell.getCapacity());
        CellMutationReason reason = reasonForSet(value, clamped, beforeAmount, cell.getCapacity());
        cell.setAmount(clamped);
        long afterAmount = cell.getAmount();
        double afterVolume = cell.getVolume();
        return new CellMutationReceipt(
                thing.getId(),
                key,
                "amount",
                beforeAmount,
                afterAmount,
                beforeVolume,
                afterVolume,
                null,
                null,
                reason
        );
    }

    public static CellMutationReceipt consume(Thing thing, String cellName, long delta) {
        String key = normalizeCellName(cellName);
        if (thing == null || key.isBlank()) {
            return missingReceipt(thing, key, delta);
        }
        Cell cell = thing.getCell(key);
        if (cell == null) {
            return missingReceipt(thing, key, delta);
        }
        long beforeAmount = cell.getAmount();
        double beforeVolume = cell.getVolume();
        long applied = computeConsumeApplied(delta, beforeAmount);
        long afterAmount = beforeAmount - applied;
        cell.setAmount(afterAmount);
        double afterVolume = cell.getVolume();
        CellMutationReason reason = reasonForConsume(delta, applied);
        return new CellMutationReceipt(
                thing.getId(),
                key,
                "amount",
                beforeAmount,
                afterAmount,
                beforeVolume,
                afterVolume,
                delta,
                applied,
                reason
        );
    }

    public static CellMutationReceipt replenish(Thing thing, String cellName, long delta) {
        String key = normalizeCellName(cellName);
        if (thing == null || key.isBlank()) {
            return missingReceipt(thing, key, delta);
        }
        Cell cell = thing.getCell(key);
        if (cell == null) {
            return missingReceipt(thing, key, delta);
        }
        long beforeAmount = cell.getAmount();
        double beforeVolume = cell.getVolume();
        long applied = computeReplenishApplied(delta, beforeAmount, cell.getCapacity());
        long afterAmount = beforeAmount + applied;
        cell.setAmount(afterAmount);
        double afterVolume = cell.getVolume();
        CellMutationReason reason = reasonForReplenish(delta, applied);
        return new CellMutationReceipt(
                thing.getId(),
                key,
                "amount",
                beforeAmount,
                afterAmount,
                beforeVolume,
                afterVolume,
                delta,
                applied,
                reason
        );
    }

    public static CellTransferResult transfer(Thing fromThing, Thing toThing, String cellName, long delta) {
        String key = normalizeCellName(cellName);
        UUID fromId = fromThing == null ? null : fromThing.getId();
        UUID toId = toThing == null ? null : toThing.getId();
        if (fromThing == null || toThing == null || key.isBlank()) {
            CellMutationReceipt fromReceipt = missingReceipt(fromThing, key, delta);
            CellMutationReceipt toReceipt = missingReceipt(toThing, key, delta);
            return new CellTransferResult(
                    fromReceipt,
                    toReceipt,
                    new CellTransferReceipt(fromId, toId, key, delta, 0L)
            );
        }
        Cell fromCell = fromThing.getCell(key);
        Cell toCell = toThing.getCell(key);
        if (fromCell == null || toCell == null) {
            CellMutationReceipt fromReceipt = missingReceipt(fromThing, key, delta);
            CellMutationReceipt toReceipt = missingReceipt(toThing, key, delta);
            return new CellTransferResult(
                    fromReceipt,
                    toReceipt,
                    new CellTransferReceipt(fromId, toId, key, delta, 0L)
            );
        }

        long fromBefore = fromCell.getAmount();
        long toBefore = toCell.getAmount();
        double fromBeforeVolume = fromCell.getVolume();
        double toBeforeVolume = toCell.getVolume();

        long maxFrom = fromBefore;
        long maxTo = Math.max(0L, toCell.getCapacity() - toBefore);
        long applied = computeTransferApplied(delta, maxFrom, maxTo);
        fromCell.setAmount(fromBefore - applied);
        toCell.setAmount(toBefore + applied);

        long fromAfter = fromCell.getAmount();
        long toAfter = toCell.getAmount();
        double fromAfterVolume = fromCell.getVolume();
        double toAfterVolume = toCell.getVolume();

        CellMutationReceipt fromReceipt = new CellMutationReceipt(
                fromThing.getId(),
                key,
                "amount",
                fromBefore,
                fromAfter,
                fromBeforeVolume,
                fromAfterVolume,
                delta,
                applied,
                reasonForTransferFrom(delta, applied, maxFrom)
        );
        CellMutationReceipt toReceipt = new CellMutationReceipt(
                toThing.getId(),
                key,
                "amount",
                toBefore,
                toAfter,
                toBeforeVolume,
                toAfterVolume,
                delta,
                applied,
                reasonForTransferTo(delta, applied, maxTo)
        );
        CellTransferReceipt transferReceipt = new CellTransferReceipt(fromThing.getId(), toThing.getId(), key, delta, applied);
        return new CellTransferResult(fromReceipt, toReceipt, transferReceipt);
    }

    private static CellMutationReceipt missingReceipt(Thing thing, String cellName, Long requestedDelta) {
        UUID id = thing == null ? null : thing.getId();
        return new CellMutationReceipt(
                id,
                cellName,
                "amount",
                0L,
                0L,
                0.0,
                0.0,
                requestedDelta,
                0L,
                CellMutationReason.MISSING_CELL
        );
    }

    private static CellMutationReason reasonForSet(long value, long clamped, long beforeAmount, long capacity) {
        if (value < 0) {
            return CellMutationReason.CLAMPED_TO_ZERO;
        }
        if (value > capacity) {
            return CellMutationReason.CLAMPED_TO_CAPACITY;
        }
        if (clamped == beforeAmount) {
            return CellMutationReason.NO_OP;
        }
        return CellMutationReason.APPLIED;
    }

    private static CellMutationReason reasonForConsume(long delta, long applied) {
        if (delta <= 0) {
            return CellMutationReason.NO_OP;
        }
        if (applied < delta) {
            return CellMutationReason.CLAMPED_TO_ZERO;
        }
        return CellMutationReason.APPLIED;
    }

    private static CellMutationReason reasonForReplenish(long delta, long applied) {
        if (delta <= 0) {
            return CellMutationReason.NO_OP;
        }
        if (applied < delta) {
            return CellMutationReason.CLAMPED_TO_CAPACITY;
        }
        return CellMutationReason.APPLIED;
    }

    private static CellMutationReason reasonForTransferFrom(long delta, long applied, long available) {
        if (delta <= 0) {
            return CellMutationReason.NO_OP;
        }
        if (applied < delta && applied == available) {
            return CellMutationReason.CLAMPED_TO_ZERO;
        }
        if (applied == 0) {
            return CellMutationReason.NO_OP;
        }
        return CellMutationReason.APPLIED;
    }

    private static CellMutationReason reasonForTransferTo(long delta, long applied, long capacityRemaining) {
        if (delta <= 0) {
            return CellMutationReason.NO_OP;
        }
        if (applied < delta && applied == capacityRemaining) {
            return CellMutationReason.CLAMPED_TO_CAPACITY;
        }
        if (applied == 0) {
            return CellMutationReason.NO_OP;
        }
        return CellMutationReason.APPLIED;
    }

    private static long computeConsumeApplied(long delta, long amount) {
        if (delta <= 0) {
            return 0L;
        }
        return Math.min(delta, Math.max(0L, amount));
    }

    private static long computeReplenishApplied(long delta, long amount, long capacity) {
        if (delta <= 0) {
            return 0L;
        }
        long room = Math.max(0L, capacity - amount);
        return Math.min(delta, room);
    }

    private static long computeTransferApplied(long delta, long maxFrom, long maxTo) {
        if (delta <= 0) {
            return 0L;
        }
        return Math.min(delta, Math.min(maxFrom, maxTo));
    }

    private static String normalizeCellName(String cellName) {
        if (cellName == null) {
            return "";
        }
        return Thing.normalizeCellKey(cellName);
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}

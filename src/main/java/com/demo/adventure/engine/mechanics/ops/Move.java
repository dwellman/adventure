package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.kernel.ContainerPacker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class Move {
    private Move() {
    }

    public static String move(
            Thing thingToMove,
            Thing from,
            Thing to,
            Map<Thing, List<Look.ContainerEntry>> entriesByOwner
    ) {
        if (thingToMove == null || from == null || to == null || entriesByOwner == null) {
            return "You can't see that.";
        }
        if (!from.isOpen()) {
            return "The " + label(from) + " is closed.";
        }
        if (!thingToMove.isVisible()) {
            return "You can't see that.";
        }

        List<Look.ContainerEntry> fromEntries = entriesByOwner.get(from);
        if (fromEntries == null) {
            return "You can't see that.";
        }

        List<Look.ContainerEntry> mutableFrom = new ArrayList<>(fromEntries);
        Look.ContainerEntry match = findMatch(mutableFrom, thingToMove);
        if (match == null) {
            return "You can't see that.";
        }

        mutableFrom.remove(match);
        entriesByOwner.put(from, mutableFrom);

        List<Look.ContainerEntry> toEntries = entriesByOwner.get(to);
        List<Look.ContainerEntry> mutableTo = toEntries == null ? new ArrayList<>() : new ArrayList<>(toEntries);
        Rectangle2D placement = computePlacement(to, thingToMove, mutableTo);
        if (placement == null && to instanceof Item dest && dest.getCapacityWidth() > 0 && dest.getCapacityHeight() > 0) {
            // put back the removed entry before returning failure
            mutableFrom.add(match);
            entriesByOwner.put(from, mutableFrom);
            return "It doesn't fit in the " + label(to) + ".";
        }
        mutableTo.add(new Look.ContainerEntry(thingToMove, placement));
        entriesByOwner.put(to, mutableTo);

        String label = thingToMove.getLabel() == null ? "" : thingToMove.getLabel();
        return "You move " + label + ".";
    }

    public static String moveToHand(
            Thing thingToMove,
            Thing from,
            Thing actor,
            java.util.function.Function<Thing, List<Thing>> fixtureChildrenProvider,
            Map<Thing, List<Look.ContainerEntry>> entriesByOwner
    ) {
        if (thingToMove == null || from == null || actor == null || fixtureChildrenProvider == null || entriesByOwner == null) {
            return "You can't see that.";
        }
        List<Thing> fixtures = fixtureChildrenProvider.apply(actor);
        if (fixtures == null) {
            return "You can't see that.";
        }
        Thing hand = null;
        for (Thing candidate : fixtures) {
            if (candidate != null && candidate.isVisible()) {
                hand = candidate;
                break;
            }
        }
        if (hand == null) {
            return "You can't see that.";
        }
        return move(thingToMove, from, hand, entriesByOwner);
    }

    private static Look.ContainerEntry findMatch(List<Look.ContainerEntry> entries, Thing thingToMove) {
        for (Look.ContainerEntry entry : entries) {
            if (entry == null || entry.thing() == null) {
                continue;
            }
            if (Objects.equals(entry.thing().getId(), thingToMove.getId())) {
                return entry;
            }
            if (entry.thing() == thingToMove) {
                return entry;
            }
        }
        return null;
    }

    private static Rectangle2D computePlacement(Thing container, Thing item, List<Look.ContainerEntry> existing) {
        if (!(container instanceof Item dest)) {
            return null;
        }
        if (!(item instanceof Item src)) {
            return null;
        }
        if (dest.getCapacityWidth() <= 0 || dest.getCapacityHeight() <= 0) {
            return null;
        }

        double width = Math.min(1.0, Math.max(0.01, src.getFootprintWidth() / dest.getCapacityWidth()));
        double height = Math.min(1.0, Math.max(0.01, src.getFootprintHeight() / dest.getCapacityHeight()));

        List<Rectangle2D> occupied = existing.stream()
                .map(Look.ContainerEntry::placement)
                .filter(Objects::nonNull)
                .toList();
        var placement = ContainerPacker.place(width, height, occupied);
        return placement.map(ContainerPacker.Placement::asRectangle).orElse(null);
    }

    private static String label(Thing thing) {
        if (thing == null) {
            return "";
        }
        String value = thing.getLabel();
        return value == null ? "" : value;
    }
}

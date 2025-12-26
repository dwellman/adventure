package com.demo.adventure.domain.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class Gate extends Thing {
    private Direction direction;
    private UUID plotAId;
    private UUID plotBId;
    private final Map<UUID, Description> descriptionsByFromPlot = new HashMap<>();

    public Gate(
        UUID id,
        String label,
        String description,
        UUID plotAId,
        UUID plotBId,
        Direction direction,
        boolean visible,
        String keyString
    ) {
        super(id, ThingKind.GATE, label, description, requireOwner(plotAId, plotBId));
        this.direction = direction;
        this.plotAId = plotAId;
        this.plotBId = plotBId;
        if (plotAId != null) {
            descriptionsByFromPlot.put(plotAId, new Description(description));
        }
        if (plotBId != null) {
            descriptionsByFromPlot.put(plotBId, new Description(description));
        }
        setVisible(visible);
        setKey(keyString == null ? "true" : keyString);
    }

    private static UUID requireOwner(UUID plotAId, UUID plotBId) {
        if (plotAId != null) {
            return plotAId;
        }
        if (plotBId != null) {
            return plotBId;
        }
        throw new IllegalArgumentException("Gate plotAId or plotBId is required");
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public UUID getPlotAId() {
        return plotAId;
    }

    public void setPlotAId(UUID plotAId) {
        this.plotAId = plotAId;
    }

    public UUID getPlotBId() {
        return plotBId;
    }

    public void setPlotBId(UUID plotBId) {
        this.plotBId = plotBId;
    }

    private Description descriptionFor(UUID fromPlotId) {
        return descriptionsByFromPlot.computeIfAbsent(fromPlotId, k -> new Description(getDescription()));
    }

    public String getDescriptionFrom(UUID fromPlotId) {
        return descriptionFor(fromPlotId).getText();
    }

    public List<DescriptionVersion> getDescriptionHistoryFrom(UUID fromPlotId) {
        return descriptionFor(fromPlotId).getHistory();
    }

    public void recordDescriptionFrom(UUID fromPlotId, String descriptionText, int worldClock) {
        descriptionFor(fromPlotId).record(descriptionText, worldClock);
    }

    public String getKeyString() {
        return getKey();
    }

    public void setKeyString(String keyString) {
        setKey(keyString);
    }

    public boolean connects(UUID plotId) {
        return Objects.equals(plotAId, plotId) || Objects.equals(plotBId, plotId);
    }

    public UUID otherSide(UUID plotId) {
        if (Objects.equals(plotId, plotAId)) {
            return plotBId;
        }
        if (Objects.equals(plotId, plotBId)) {
            return plotAId;
        }
        return null;
    }

    public Direction directionFrom(UUID sourcePlotId) {
        if (Objects.equals(plotAId, sourcePlotId)) {
            return direction;
        }
        if (Objects.equals(plotBId, sourcePlotId)) {
            return Direction.oppositeOf(direction);
        }
        return null;
    }
}

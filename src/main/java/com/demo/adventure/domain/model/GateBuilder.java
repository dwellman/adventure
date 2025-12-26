package com.demo.adventure.domain.model;

import java.util.UUID;

public final class GateBuilder {
    private UUID id;
    private String label;
    private String description;
    private UUID plotAId;
    private UUID plotBId;
    private Direction direction;
    private boolean visible = true;
    private String keyString = "true";

    public GateBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public GateBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    public GateBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public GateBuilder withPlotAId(UUID plotAId) {
        this.plotAId = plotAId;
        return this;
    }

    public GateBuilder withPlotA(Plot plotA) {
        this.plotAId = plotA == null ? null : plotA.getId();
        return this;
    }

    public GateBuilder withPlotBId(UUID plotBId) {
        this.plotBId = plotBId;
        return this;
    }

    public GateBuilder withPlotB(Plot plotB) {
        this.plotBId = plotB == null ? null : plotB.getId();
        return this;
    }

    public GateBuilder withDirection(Direction direction) {
        this.direction = direction;
        return this;
    }

    public GateBuilder withVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public GateBuilder withKeyString(String keyString) {
        this.keyString = keyString;
        return this;
    }

    public Gate build() {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Gate label is required");
        }
        if (plotAId == null || plotBId == null) {
            throw new IllegalArgumentException("Gate plotAId and plotBId are required");
        }
        if (direction == null) {
            throw new IllegalArgumentException("Gate direction is required");
        }
        UUID idToUse = id == null ? UUID.randomUUID() : id;
        String keyToUse = keyString == null ? "true" : keyString;
        return new Gate(idToUse, label, description, plotAId, plotBId, direction, visible, keyToUse);
    }
}

package com.demo.adventure.domain.model;

import com.demo.adventure.domain.kernel.KernelRegistry;

import java.util.UUID;

public final class PlotBuilder {
    private UUID id;
    private String label;
    private String description;
    private PlotKind plotKind = PlotKind.LAND;
    private String plotRole;
    private UUID hostThingId;
    private UUID ownerId;
    private String region;
    private int locationX = 0;
    private int locationY = 0;

    public PlotBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public PlotBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    public PlotBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public PlotBuilder withPlotKind(PlotKind plotKind) {
        this.plotKind = plotKind;
        return this;
    }

    public PlotBuilder withPlotRole(String plotRole) {
        this.plotRole = plotRole;
        return this;
    }

    public PlotBuilder withHostThingId(UUID hostThingId) {
        this.hostThingId = hostThingId;
        return this;
    }

    public PlotBuilder withHostThing(Thing host) {
        this.hostThingId = host == null ? null : host.getId();
        return this;
    }

    public PlotBuilder withOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public PlotBuilder withOwnerId(Thing owner) {
        this.ownerId = owner == null ? null : owner.getId();
        return this;
    }

    public PlotBuilder withRegion(String region) {
        this.region = region;
        return this;
    }

    public PlotBuilder withLocationX(int locationX) {
        this.locationX = locationX;
        return this;
    }

    public PlotBuilder withLocationY(int locationY) {
        this.locationY = locationY;
        return this;
    }

    public Plot build() {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Plot label is required");
        }
        if (description == null) {
            throw new IllegalArgumentException("Plot description is required");
        }

        UUID idToUse = id == null ? UUID.randomUUID() : id;
        PlotKind kindToUse = plotKind == null ? PlotKind.LAND : plotKind;
        UUID ownerToUse = ownerId == null ? KernelRegistry.MILIARIUM : ownerId;

        return new Plot(
                idToUse,
                label,
                description,
                kindToUse,
                plotRole,
                hostThingId,
                ownerToUse,
                region,
                locationX,
                locationY
        );
    }
}

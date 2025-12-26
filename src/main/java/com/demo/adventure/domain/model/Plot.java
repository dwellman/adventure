package com.demo.adventure.domain.model;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class Plot extends Thing {
    private PlotKind plotKind;
    private String plotRole;
    private UUID hostThingId;
    private String region;
    private int locationX;
    private int locationY;
    // One gate slot per direction; null entry means no path.
    private final EnumMap<Direction, UUID> gateSlots = new EnumMap<>(Direction.class);

    public Plot(
            UUID id, String label, String description, PlotKind plotKind, String plotRole,
            UUID hostThingId, UUID ownerId
    ) {
        this(id, label, description, plotKind, plotRole, hostThingId, ownerId, null, 0, 0);
    }

    public Plot(
            UUID id,
            String label,
            String description,
            PlotKind plotKind,
            String plotRole,
            UUID hostThingId,
            UUID ownerId,
            String region,
            int locationX,
            int locationY
    ) {
        super(id, ThingKind.PLOT, label, description, ownerId);
        this.plotKind = plotKind == null ? PlotKind.LAND : plotKind;
        this.plotRole = plotRole;
        this.hostThingId = hostThingId;
        this.region = region;
        this.locationX = locationX;
        this.locationY = locationY;
    }

    public PlotKind getPlotKind() {
        return plotKind;
    }

    public void setPlotKind(PlotKind plotKind) {
        this.plotKind = plotKind;
    }

    public String getPlotRole() {
        return plotRole;
    }

    public void setPlotRole(String plotRole) {
        this.plotRole = plotRole;
    }

    public UUID getHostThingId() {
        return hostThingId;
    }

    public void setHostThingId(UUID hostThingId) {
        this.hostThingId = hostThingId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public int getLocationX() {
        return locationX;
    }

    public void setLocationX(int locationX) {
        this.locationX = locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    public void setLocationY(int locationY) {
        this.locationY = locationY;
    }

    public UUID getGateId(Direction direction) {
        if (direction == null) {
            return null;
        }
        return gateSlots.get(direction);
    }

    public void setGateId(Direction direction, UUID gateId) {
        if (direction == null) {
            return;
        }
        if (gateId == null) {
            gateSlots.remove(direction);
        } else {
            gateSlots.put(direction, gateId);
        }
    }

    public Map<Direction, UUID> getGateSlots() {
        return Collections.unmodifiableMap(gateSlots);
    }
}

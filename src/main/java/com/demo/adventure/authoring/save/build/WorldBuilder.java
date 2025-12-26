package com.demo.adventure.authoring.save.build;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.GateBuilder;

import java.util.UUID;

/**
 * Minimal builder to keep map cohesion: adds plots and connects them with gates.
 */
public final class WorldBuilder {
    private final KernelRegistry registry;

    public WorldBuilder(KernelRegistry registry) {
        this.registry = registry;
    }

    /**
     * Register a new plot in the registry.
     *
     * @param plot plot to add
     * @return added plot
     * @throws GameBuilderException when input is invalid or already registered
     */
    public Plot addPlot(Plot plot) throws GameBuilderException {
        if (plot == null) {
            throw new GameBuilderException("Plot cannot be null");
        }
        if (registry.get(plot.getId()) != null) {
            throw new GameBuilderException("Plot already exists: " + plot.getId());
        }
        registry.register(plot);
        return plot;
    }

    /**
     * Connects two plots with a bidirectional gate. Direction is interpreted from plotA toward plotB.
     */
    public Gate connectPlots(
            UUID plotAId,
            Direction directionFromA,
            UUID plotBId,
            String gateLabel,
            String gateDescription
    ) throws GameBuilderException {
        return connectPlots(plotAId, directionFromA, plotBId, gateLabel, gateDescription, true, null);
    }

    /**
     * Connect two plots with a bidirectional gate.
     *
     * @param plotAId         source plot id
     * @param directionFromA  direction from source to target
     * @param plotBId         target plot id
     * @param gateLabel       label for the gate
     * @param gateDescription description for the gate
     * @param visible         whether the gate is visible
     * @param keyString       key expression controlling open/closed state
     * @return created gate
     * @throws GameBuilderException when plots are missing or directions conflict
     */
    public Gate connectPlots(
            UUID plotAId,
            Direction directionFromA,
            UUID plotBId,
            String gateLabel,
            String gateDescription,
            boolean visible,
            String keyString
    ) throws GameBuilderException {
        Plot plotA = requirePlot(plotAId, "plotAId");
        Plot plotB = requirePlot(plotBId, "plotBId");
        if (directionFromA == null) {
            throw new GameBuilderException("Direction is required to connect plots.");
        }

        Direction directionFromB = Direction.oppositeOf(directionFromA);

        if (plotA.getGateId(directionFromA) != null) {
            throw new GameBuilderException("Plot " + plotAId + " already has a gate for direction " + directionFromA);
        }
        if (directionFromB != null && plotB.getGateId(directionFromB) != null) {
            throw new GameBuilderException("Plot " + plotBId + " already has a gate for direction " + directionFromB);
        }

        Gate gate = new GateBuilder()
                .withLabel(gateLabel)
                .withDescription(gateDescription)
                .withPlotA(plotA)
                .withPlotB(plotB)
                .withDirection(directionFromA)
                .withVisible(visible)
                .withKeyString(keyString)
                .build();
        registry.register(gate);

        plotA.setGateId(directionFromA, gate.getId());
        if (directionFromB != null) {
            plotB.setGateId(directionFromB, gate.getId());
        }

        return gate;
    }

    private Plot requirePlot(UUID plotId, String fieldName) throws GameBuilderException {
        if (plotId == null) {
            throw new GameBuilderException(fieldName + " is required.");
        }
        Thing thing = registry.get(plotId);
        if (!(thing instanceof Plot plot)) {
            throw new GameBuilderException("Plot not found: " + plotId);
        }
        return plot;
    }
}

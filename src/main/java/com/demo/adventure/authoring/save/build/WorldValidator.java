package com.demo.adventure.authoring.save.build;

import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotKind;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Validates world recipes and built registries for consistency.
 */
public final class WorldValidator {

    /**
     * Validate a recipe before build.
     *
     * @param recipe world recipe
     * @return report of issues (empty when valid)
     */
    public WorldBuildReport validateRecipe(WorldRecipe recipe) {
        WorldBuildReport report = new WorldBuildReport();
        if (recipe == null) {
            report.add(new WorldBuildProblem("E_RECIPE_MISSING", "Recipe is required", "WORLD", null));
            return report;
        }

        Set<UUID> plotIds = new HashSet<>();
        for (WorldRecipe.PlotSpec plot : recipe.plots()) {
            if (plot == null) {
                continue;
            }
            UUID plotId = plot.plotId();
            if (plotId == null) {
                report.add(new WorldBuildProblem("E_PLOT_ID_MISSING", "Plot id is required", "PLOT", null));
                continue;
            }
            if (!plotIds.add(plotId)) {
                report.add(new WorldBuildProblem("E_PLOT_ID_DUPLICATE", "Duplicate plot id: " + plotId, "PLOT", plotId));
            }
        }

        if (recipe.startPlotId() == null) {
            report.add(new WorldBuildProblem("E_START_PLOT_MISSING", "startPlotId is required", "WORLD", null));
        } else if (!plotIds.contains(recipe.startPlotId())) {
            report.add(new WorldBuildProblem(
                    "E_START_PLOT_NOT_FOUND",
                    "startPlotId does not match any plot",
                    "WORLD",
                    recipe.startPlotId()
            ));
        }

        Map<UUID, Set<Direction>> gateDirections = new HashMap<>();
        for (WorldRecipe.GateSpec gate : recipe.gates()) {
            if (gate == null) {
                continue;
            }
            UUID fromId = gate.fromPlotId();
            UUID toId = gate.toPlotId();
            if (fromId == null || !plotIds.contains(fromId)) {
                report.add(new WorldBuildProblem(
                        "E_GATE_FROM_PLOT_NOT_FOUND",
                        "Gate fromPlotId missing or unknown",
                        "GATE",
                        fromId
                ));
            }
            if (toId == null || !plotIds.contains(toId)) {
                report.add(new WorldBuildProblem(
                        "E_GATE_TO_PLOT_NOT_FOUND",
                        "Gate toPlotId missing or unknown",
                        "GATE",
                        toId
                ));
            }
            if (gate.direction() == null) {
                report.add(new WorldBuildProblem(
                        "E_GATE_DIRECTION_MISSING",
                        "Gate direction is required",
                        "GATE",
                        fromId
                ));
            }
            if (gate.keyString() == null || gate.keyString().trim().isEmpty()) {
                report.add(new WorldBuildProblem(
                        "E_GATE_KEYSTRING_NULL",
                        "Gate keyString must be non-null",
                        "GATE",
                        fromId
                ));
            }
            if (gate.direction() != null && fromId != null) {
                Set<Direction> used = gateDirections.computeIfAbsent(fromId, k -> new HashSet<>());
                if (!used.add(gate.direction())) {
                    report.add(new WorldBuildProblem(
                            "E_GATE_DIRECTION_CONFLICT",
                            "Multiple gates for direction " + gate.direction() + " from plot " + fromId,
                            "GATE",
                            fromId
                    ));
                }
            }
        }

        Set<UUID> fixtureIds = new HashSet<>();
        for (WorldRecipe.FixtureSpec fixture : recipe.fixtures()) {
            if (fixture == null) {
                continue;
            }
            UUID fixtureId = fixture.id();
            if (fixtureId == null) {
                report.add(new WorldBuildProblem("E_FIXTURE_ID_MISSING", "Fixture id is required", "FIXTURE", null));
            } else if (!fixtureIds.add(fixtureId)) {
                report.add(new WorldBuildProblem(
                        "E_FIXTURE_ID_DUPLICATE",
                        "Duplicate fixture id: " + fixtureId,
                        "FIXTURE",
                        fixtureId
                ));
            }
            UUID ownerId = fixture.ownerId();
            if (ownerId == null) {
                report.add(new WorldBuildProblem(
                        "E_OWNER_MISSING",
                        "Fixture owner is required",
                        "FIXTURE",
                        fixtureId
                ));
            } else if (!plotIds.contains(ownerId) && !fixtureIds.contains(ownerId)) {
                report.add(new WorldBuildProblem(
                        "E_OWNER_NOT_FOUND",
                        "Fixture owner not found in recipe",
                        "FIXTURE",
                        fixtureId
                ));
            }
        }

        return report;
    }

    /**
     * Validate the built world after assembly.
     *
     * @param recipe    recipe used for build
     * @param registry  resulting registry
     * @return report of issues (empty when valid)
     */
    public WorldBuildReport validateWorld(WorldRecipe recipe, KernelRegistry registry) {
        WorldBuildReport report = new WorldBuildReport();
        if (recipe == null || registry == null) {
            return report;
        }

        Map<UUID, Thing> everything = registry.getEverything();
        Map<UUID, Plot> plots = new HashMap<>();
        for (Thing thing : everything.values()) {
            if (thing instanceof Plot plot) {
                plots.put(plot.getId(), plot);
            }
        }

        UUID startPlotId = recipe.startPlotId();
        if (startPlotId == null) {
            report.add(new WorldBuildProblem("E_START_PLOT_MISSING", "startPlotId is required", "WORLD", null));
            return report;
        }
        if (!plots.containsKey(startPlotId)) {
            report.add(new WorldBuildProblem(
                    "E_START_PLOT_NOT_FOUND",
                    "startPlotId does not exist in built world",
                    "WORLD",
                    startPlotId
            ));
            return report;
        }

        Set<UUID> reachable = computeReachablePlots(startPlotId, plots, everything);
        for (UUID plotId : plots.keySet()) {
            if (!reachable.contains(plotId)) {
                report.add(new WorldBuildProblem(
                        "E_UNREACHABLE_PLOT",
                        "Plot is not reachable from start",
                        "PLOT",
                        plotId
                ));
            }
            Plot plot = plots.get(plotId);
            if (plot != null && plot.getPlotKind() == PlotKind.LAND
                    && !Objects.equals(plot.getOwnerId(), KernelRegistry.MILIARIUM)) {
                report.add(new WorldBuildProblem(
                        "E_LAND_NOT_ANCHORED",
                        "Land plot is not anchored to the Miliarium",
                        "PLOT",
                        plotId
                ));
            }
        }

        for (Thing thing : everything.values()) {
            if (thing instanceof Item item && item.isFixture()) {
                validateFixtureOwnership(item, everything, reachable, report);
            }
        }

        return report;
    }

    private static Set<UUID> computeReachablePlots(UUID startPlotId, Map<UUID, Plot> plots, Map<UUID, Thing> everything) {
        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new ArrayDeque<>();
        visited.add(startPlotId);
        queue.add(startPlotId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            for (Thing thing : everything.values()) {
                if (thing instanceof Gate gate && gate.connects(current)) {
                    UUID other = gate.otherSide(current);
                    if (other != null && plots.containsKey(other) && visited.add(other)) {
                        queue.add(other);
                    }
                }
            }
        }
        return visited;
    }

    private static void validateFixtureOwnership(
            Item item,
            Map<UUID, Thing> everything,
            Set<UUID> reachablePlots,
            WorldBuildReport report
    ) {
        Set<UUID> visited = new HashSet<>();
        Thing current = item;
        while (current != null && visited.add(current.getId())) {
            UUID ownerId = current.getOwnerId();
            if (ownerId == null) {
                report.add(new WorldBuildProblem(
                        "E_OWNER_MISSING",
                        "Fixture owner is missing",
                        "FIXTURE",
                        item.getId()
                ));
                return;
            }
            Thing owner = everything.get(ownerId);
            if (owner == null) {
                report.add(new WorldBuildProblem(
                        "E_OWNER_NOT_FOUND",
                        "Fixture owner not found in world",
                        "FIXTURE",
                        item.getId()
                ));
                return;
            }
            if (owner instanceof Plot plot) {
                if (plot.getPlotKind() != PlotKind.LAND) {
                    report.add(new WorldBuildProblem(
                            "E_OWNERSHIP_NOT_TERMINATED_AT_LAND",
                            "Fixture ownership chain does not end at a land plot",
                            "FIXTURE",
                            item.getId()
                    ));
                    return;
                }
                if (!reachablePlots.contains(plot.getId())) {
                    report.add(new WorldBuildProblem(
                            "E_FIXTURE_UNREACHABLE",
                            "Fixture cannot be reached from start plot",
                            "FIXTURE",
                            item.getId()
                    ));
                }
                return;
            }
            if (visited.contains(owner.getId())) {
                report.add(new WorldBuildProblem(
                        "E_OWNERSHIP_CYCLE",
                        "Ownership cycle detected",
                        "FIXTURE",
                        item.getId()
                ));
                return;
            }
            current = owner;
        }
        report.add(new WorldBuildProblem(
                "E_OWNERSHIP_NOT_TERMINATED_AT_LAND",
                "Fixture ownership chain does not end at a land plot",
                "FIXTURE",
                item.getId()
        ));
    }
}

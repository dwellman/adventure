package com.demo.adventure.authoring.save.build;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

    /**
     * Deterministic builder that consumes a WorldRecipe (Phase 0 input) and
     * materializes plots, gates, and fixtures (Phase 1/2) into a KernelRegistry.
     */
public final class WorldAssembler {

    private static final String BOM_PROPERTY = "adventure.bom";

    /**
     * Build plots, gates, and fixtures from a recipe and validate the result.
     *
     * @param recipe world recipe input
     * @return world build result with registry and validation report
     * @throws GameBuilderException when blocking problems are found
     */
    public WorldBuildResult build(WorldRecipe recipe) throws GameBuilderException {
        WorldValidator validator = new WorldValidator();
        WorldBuildReport recipeReport = validator.validateRecipe(recipe);

        KernelRegistry registry = new KernelRegistry();
        WorldBuildReport buildProblems = new WorldBuildReport();
        try {
            registry = buildMap(recipe);
        } catch (Exception ex) {
            buildProblems.add(new WorldBuildProblem(
                    "E_BUILD_EXCEPTION",
                    "Exception during map build: " + ex.getMessage(),
                    "WORLD",
                    null
            ));
        }
        try {
            placeFixtures(recipe, registry);
        } catch (Exception ex) {
            buildProblems.add(new WorldBuildProblem(
                    "E_BUILD_EXCEPTION",
                    "Exception during fixture placement: " + ex.getMessage(),
                    "WORLD",
                    null
            ));
        }

        WorldBuildReport worldReport = validator.validateWorld(recipe, registry);

        WorldBuildReport combined = combineReports(recipeReport, buildProblems, worldReport);
        if (combined.hasBlockingProblems()) {
            System.out.println(WorldBuildReportFormatter.format(combined));
            throw new GameBuilderException(
                    "World build failed with " + combined.getProblems().size() + " problems",
                    combined
            );
        }

        if (Boolean.parseBoolean(System.getProperty(BOM_PROPERTY, "false"))) {
            WorldBillOfMaterials bom = WorldBillOfMaterialsGenerator.fromRegistry(registry);
            System.out.println(WorldBillOfMaterialsFormatter.format(bom));
        }
        return new WorldBuildResult(recipe.startPlotId(), recipe.seed(), registry, combined);
    }

    /**
     * Build only the map layer (plots + gates) from a recipe.
     *
     * @param recipe world recipe input
     * @return registry containing plots and gates
     */
    public KernelRegistry buildMap(WorldRecipe recipe) {
        if (recipe == null) {
            return new KernelRegistry();
        }
        KernelRegistry registry = new KernelRegistry();
        WorldBuilder builder = new WorldBuilder(registry);

        Set<UUID> plotIds = new HashSet<>();
        for (WorldRecipe.PlotSpec plotSpec : recipe.plots()) {
            if (plotSpec == null) {
                continue;
            }
            if (plotSpec.plotId() == null) {
                continue;
            }
            if (!plotIds.add(plotSpec.plotId())) {
                continue;
            }
            PlotBuilder plotBuilder = new PlotBuilder()
                    .withId(plotSpec.plotId())
                    .withLabel(plotSpec.name())
                    .withDescription(plotSpec.description())
                    .withPlotRole(normalizeRole(plotSpec.name()))
                    .withRegion(plotSpec.region())
                    .withLocationX(plotSpec.locationX())
                    .withLocationY(plotSpec.locationY());
            try {
                builder.addPlot(plotBuilder.build());
            } catch (Exception ex) {
                // skip invalid plot
            }
        }

        for (WorldRecipe.GateSpec gateSpec : recipe.gates()) {
            if (gateSpec == null) {
                continue;
            }
            UUID fromId = gateSpec.fromPlotId();
            UUID toId = gateSpec.toPlotId();
            Direction direction = gateSpec.direction();
            if (fromId == null || toId == null || direction == null) {
                continue;
            }
            Plot fromPlot = (Plot) registry.get(fromId);
            Plot toPlot = (Plot) registry.get(toId);
            if (fromPlot == null || toPlot == null) {
                continue;
            }
            if (fromPlot.getGateId(direction) != null) {
                continue;
            }
            Direction opposite = Direction.oppositeOf(direction);
            if (opposite != null && toPlot.getGateId(opposite) != null) {
                continue;
            }
                try {
                    // Gates are plain graph edges: keyString gates traversal; regions do not block cross-zone links.
                    builder.connectPlots(
                            gateSpec.fromPlotId(),
                            gateSpec.direction(),
                            gateSpec.toPlotId(),
                            gateSpec.label(),
                            gateSpec.description(),
                            gateSpec.visible(),
                            normalizeKeyString(gateSpec.keyString())
                    );
                } catch (GameBuilderException ex) {
                    // skip invalid gate
            }
        }

        synthesizeMissingGates(registry, builder);
        return registry;
    }

    /**
     * Place fixture items from the recipe into the provided registry.
     *
     * @param recipe   world recipe input
     * @param registry target registry
     */
    public void placeFixtures(WorldRecipe recipe, KernelRegistry registry) {
        if (recipe == null || registry == null) {
            return;
        }

        for (WorldRecipe.FixtureSpec fixture : recipe.fixtures()) {
            if (fixture == null) {
                continue;
            }
            if (fixture.id() == null) {
                continue;
            }
            try {
                Item item = new ItemBuilder()
                        .withId(fixture.id())
                        .withLabel(fixture.name())
                        .withDescription(fixture.description())
                        .withOwnerId(fixture.ownerId())
                        .withVisible(fixture.visible())
                        .withFixture(true)
                        .build();
                applyCells(item, fixture.cells());
                registry.register(item);
            } catch (Exception ex) {
                // skip invalid fixture build
            }
        }
    }

    private static String normalizeRole(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return name.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("\\s+", "_");
    }

    private static String normalizeKeyString(String keyString) {
        if (keyString == null) {
            return null;
        }
        String trimmed = keyString.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    private static WorldBuildReport combineReports(WorldBuildReport... reports) {
        WorldBuildReport combined = new WorldBuildReport();
        if (reports != null) {
            for (WorldBuildReport report : reports) {
                if (report != null) {
                    combined.addAll(report.getProblems());
                }
            }
        }
        return combined;
    }

    /**
     * Auto-synthesize gates between cardinally adjacent plots when no gate was declared in YAML.
     * This keeps the “everything connects through a gate” rule without forcing authors to list every edge.
     */
    private void synthesizeMissingGates(KernelRegistry registry, WorldBuilder builder) {
        if (registry == null || builder == null) {
            return;
        }

        Map<String, Plot> plotsByCoord = new HashMap<>();
        for (Thing thing : registry.getEverything().values()) {
            if (thing instanceof Plot plot) {
                String key = plot.getLocationX() + ":" + plot.getLocationY();
                plotsByCoord.put(key, plot);
            }
        }

        int[][] deltas = {{0, 1}, {1, 0}, {0, -1}, {-1, 0}};
        Direction[] directions = {Direction.N, Direction.E, Direction.S, Direction.W};

        for (Plot plot : plotsByCoord.values()) {
            if (plot == null) {
                continue;
            }
            for (int i = 0; i < directions.length; i++) {
                Direction dir = directions[i];
                Plot neighbor = plotsByCoord.get((plot.getLocationX() + deltas[i][0]) + ":" + (plot.getLocationY() + deltas[i][1]));
                if (neighbor == null) {
                    continue;
                }
                if (plot.getGateId(dir) != null) {
                    continue;
                }
                Direction opposite = Direction.oppositeOf(dir);
                if (opposite != null && neighbor.getGateId(opposite) != null) {
                    continue;
                }
                if (hasGateBetween(registry, plot.getId(), neighbor.getId())) {
                    continue;
                }
                try {
                    // Optimistic movement: synthesize visible, open gates by default.
                    // If an explicit gate is desired, declare it in YAML; this only fills gaps.
                    builder.connectPlots(
                            plot.getId(),
                            dir,
                            neighbor.getId(),
                            (plot.getLabel() == null ? "Gate" : plot.getLabel()) + " -> " + (neighbor.getLabel() == null ? "Gate" : neighbor.getLabel()),
                            "Automatically generated connection.",
                            true,
                            "true"
                    );
                } catch (GameBuilderException ex) {
                    // If a collision occurs, skip and continue.
                }
            }
        }
    }

    private boolean hasGateBetween(KernelRegistry registry, UUID a, UUID b) {
        if (registry == null || a == null || b == null) {
            return false;
        }
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .anyMatch(g -> g.connects(a) && g.connects(b));
    }

    private static void applyCells(Item item, Map<String, CellSpec> specs) {
        if (item == null || specs == null || specs.isEmpty()) {
            return;
        }
        Map<String, Cell> cells = new HashMap<>();
        for (Map.Entry<String, CellSpec> entry : specs.entrySet()) {
            CellSpec spec = entry.getValue();
            if (spec != null) {
                cells.put(entry.getKey(), spec.toCell());
            }
        }
        item.setCells(cells);
    }
}

package com.demo.adventure.authoring.gardener;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

/**
 * Gardener pipeline: runs deterministic exit checks and applies fixture description enrichment.
 */
public final class Gardener {
    private final FixtureDescriptionExpander fixtureDescriptionExpander;

    public Gardener() {
        this(new NoopFixtureDescriptionExpander());
    }

    public Gardener(FixtureDescriptionExpander fixtureDescriptionExpander) {
        this.fixtureDescriptionExpander = fixtureDescriptionExpander == null
                ? new NoopFixtureDescriptionExpander()
                : fixtureDescriptionExpander;
    }

    /**
     * Build the world, run deterministic Gardener checks, and apply the description pass.
     *
     * @param save source save to assemble
     * @return gardened result
     * @throws GameBuilderException when the underlying build fails
     */
    // Pattern: Orchestration + Verification
    // - Runs deterministic checks first, then applies optional AI description patches with a combined report.
    public GardenResult garden(GameSave save) throws GameBuilderException {
        Objects.requireNonNull(save, "save");

        GameSaveAssembler assembler = new GameSaveAssembler();
        WorldBuildResult buildResult = assembler.apply(save);
        WorldRecipe recipe = new WorldRecipe(save.seed(), save.startPlotId(), save.plots(), save.gates(), save.fixtures());

        WorldBuildReport exitReport = checkReturnPathsToStart(recipe);
        WorldBuildReport combinedReport = new WorldBuildReport(buildResult.report().getProblems());
        combinedReport.addAll(exitReport.getProblems());

        List<GardenerDescriptionPatch> patches = fixtureDescriptionExpander.expand(buildResult.registry());

        return new GardenResult(buildResult.startPlotId(), buildResult.seed(), buildResult.registry(), combinedReport, patches);
    }

    private static WorldBuildReport checkReturnPathsToStart(WorldRecipe recipe) {
        WorldBuildReport report = new WorldBuildReport();
        if (recipe == null) {
            return report;
        }
        UUID startPlotId = recipe.startPlotId();
        if (startPlotId == null) {
            report.add(new WorldBuildProblem(
                    "E_START_PLOT_MISSING",
                    "Gardener cannot check exits without a start plot",
                    "WORLD",
                    null
            ));
            return report;
        }

        Map<UUID, List<UUID>> adjacency = buildAdjacency(recipe.gates());
        Map<UUID, Boolean> memo = new HashMap<>();

        for (WorldRecipe.GateSpec gate : recipe.gates()) {
            if (gate == null) {
                continue;
            }
            UUID destination = gate.toPlotId();
            UUID source = gate.fromPlotId();
            if (destination == null || source == null) {
                continue;
            }
            boolean canReturn = memo.computeIfAbsent(destination, plot -> canReach(startPlotId, plot, adjacency));
            if (!canReturn) {
                report.add(new WorldBuildProblem(
                        "E_EXIT_NO_RETURN_PATH",
                        "Exit from " + source + " via " + gate.direction() + " cannot reach the start plot",
                        "GATE",
                        source
                ));
            }
        }

        return report;
    }

    private static Map<UUID, List<UUID>> buildAdjacency(List<WorldRecipe.GateSpec> gates) {
        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        if (gates == null) {
            return adjacency;
        }
        for (WorldRecipe.GateSpec gate : gates) {
            if (gate == null) {
                continue;
            }
            UUID from = gate.fromPlotId();
            UUID to = gate.toPlotId();
            if (from == null || to == null) {
                continue;
            }
            adjacency.computeIfAbsent(from, k -> new ArrayList<>()).add(to);
        }
        return adjacency;
    }

    private static boolean canReach(UUID targetPlotId, UUID sourcePlotId, Map<UUID, List<UUID>> adjacency) {
        if (targetPlotId == null || sourcePlotId == null) {
            return false;
        }
        if (Objects.equals(targetPlotId, sourcePlotId)) {
            return true;
        }

        Set<UUID> visited = new HashSet<>();
        Queue<UUID> queue = new ArrayDeque<>();
        queue.add(sourcePlotId);
        visited.add(sourcePlotId);

        while (!queue.isEmpty()) {
            UUID current = queue.poll();
            if (Objects.equals(current, targetPlotId)) {
                return true;
            }
            for (UUID neighbor : adjacency.getOrDefault(current, List.of())) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }
        return false;
    }
}

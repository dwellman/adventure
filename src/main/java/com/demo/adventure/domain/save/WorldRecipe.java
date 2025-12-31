package com.demo.adventure.domain.save;

import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.domain.model.Direction;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;

/**
 * Immutable recipe describing the pre-game world (Phase 0 inputs).
 */
public final class WorldRecipe {
    private final long seed;
    private final UUID startPlotId;
    private final List<PlotSpec> plots;
    private final List<GateSpec> gates;
    private final List<FixtureSpec> fixtures;

    public WorldRecipe(
            long seed, UUID startPlotId, List<PlotSpec> plots, List<GateSpec> gates, List<FixtureSpec> fixtures
    ) {
        this.seed = seed;
        this.startPlotId = Objects.requireNonNull(startPlotId, "startPlotId");
        this.plots = List.copyOf(Objects.requireNonNullElse(plots, List.of()));
        this.gates = List.copyOf(Objects.requireNonNullElse(gates, List.of()));
        this.fixtures = List.copyOf(Objects.requireNonNullElse(fixtures, List.of()));
    }

    public long seed() {
        return seed;
    }

    public UUID startPlotId() {
        return startPlotId;
    }

    public List<PlotSpec> plots() {
        return plots;
    }

    public List<GateSpec> gates() {
        return gates;
    }

    public List<FixtureSpec> fixtures() {
        return fixtures;
    }

    public record PlotSpec(
            UUID plotId,
            String name,
            String region,
            int locationX,
            int locationY,
            String description
    ) {
    }

    public record GateSpec(
            UUID fromPlotId,
            Direction direction,
            UUID toPlotId,
            boolean visible,
            String keyString,
            String label,
            String description
    ) {
        // Key expressions gate movement; regions are not enforced here so gates can bridge any plots/zones.
    }

    public record FixtureSpec(
            UUID id, String name, String description, UUID ownerId, boolean visible, Map<String, CellSpec> cells
    ) {
        public FixtureSpec {
            cells = normalizeCells(cells);
        }

        private static Map<String, CellSpec> normalizeCells(Map<String, CellSpec> cells) {
            if (cells == null || cells.isEmpty()) {
                return Map.of();
            }
            Map<String, CellSpec> normalized = new java.util.HashMap<>();
            for (Map.Entry<String, CellSpec> entry : cells.entrySet()) {
                String key = normalizeCellKey(entry.getKey());
                CellSpec spec = entry.getValue();
                if (!key.isBlank() && spec != null) {
                    normalized.put(key, spec);
                }
            }
            return Map.copyOf(normalized);
        }

        private static String normalizeCellKey(String name) {
            if (name == null) {
                return "";
            }
            return name.trim().toUpperCase(java.util.Locale.ROOT);
        }
    }
}

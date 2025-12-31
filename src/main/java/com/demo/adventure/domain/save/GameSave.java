package com.demo.adventure.domain.save;

import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.engine.mechanics.cells.CellSpec;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Serializable snapshot describing a world recipe plus the extra runtime entities
 * (items and actors) that are not part of the core map fixtures.
 */
public record GameSave(
        long seed,
        UUID startPlotId,
        String preamble,
        List<WorldRecipe.PlotSpec> plots,
        List<WorldRecipe.GateSpec> gates,
        List<WorldRecipe.FixtureSpec> fixtures,
        List<ItemRecipe> items,
        List<ActorRecipe> actors
) {
    public GameSave {
        Objects.requireNonNull(startPlotId, "startPlotId");
        preamble = preamble == null ? "" : preamble;
        plots = List.copyOf(Objects.requireNonNullElse(plots, List.of()));
        gates = List.copyOf(Objects.requireNonNullElse(gates, List.of()));
        fixtures = List.copyOf(Objects.requireNonNullElse(fixtures, List.of()));
        items = List.copyOf(Objects.requireNonNullElse(items, List.of()));
        actors = List.copyOf(Objects.requireNonNullElse(actors, List.of()));
    }

    public record ItemRecipe(
            UUID id,
            String name,
            String description,
            UUID ownerId,
            boolean visible,
            boolean fixture,
            String keyString,
            double footprintWidth,
            double footprintHeight,
            double capacityWidth,
            double capacityHeight,
            long weaponDamage,
            long armorMitigation,
            Map<String, CellSpec> cells
    ) {
        public ItemRecipe {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(ownerId, "ownerId");
            keyString = keyString == null ? "true" : keyString;
            footprintWidth = footprintWidth <= 0 ? 0.1 : footprintWidth;
            footprintHeight = footprintHeight <= 0 ? 0.1 : footprintHeight;
            capacityWidth = Math.max(0.0, capacityWidth);
            capacityHeight = Math.max(0.0, capacityHeight);
            weaponDamage = Math.max(0L, weaponDamage);
            armorMitigation = Math.max(0L, armorMitigation);
            cells = normalizeCells(cells);
        }
    }

    public record ActorRecipe(
            UUID id,
            String name,
            String description,
            UUID ownerId,
            boolean visible,
            List<String> skills,
            UUID equippedMainHandItemId,
            UUID equippedBodyItemId,
            Map<String, CellSpec> cells
    ) {
        public ActorRecipe {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(ownerId, "ownerId");
            skills = List.copyOf(Objects.requireNonNullElse(skills, List.of()));
            cells = normalizeCells(cells);
        }
    }

    private static Map<String, CellSpec> normalizeCells(Map<String, CellSpec> cells) {
        if (cells == null || cells.isEmpty()) {
            return Map.of();
        }
        Map<String, CellSpec> normalized = new HashMap<>();
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
        return Thing.normalizeCellKey(name);
    }
}

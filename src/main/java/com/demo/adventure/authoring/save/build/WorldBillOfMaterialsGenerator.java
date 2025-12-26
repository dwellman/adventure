package com.demo.adventure.authoring.save.build;

import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotKind;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Generates a bill of materials from an in-memory registry snapshot.
 */
public final class WorldBillOfMaterialsGenerator {
    private WorldBillOfMaterialsGenerator() {
    }

    /**
     * Build a BOM with map counts and fixture placements.
     *
     * @param registry populated registry
     * @return BOM describing the registry content
     */
    public static WorldBillOfMaterials fromRegistry(KernelRegistry registry) {
        Map<UUID, Thing> everything = registry == null ? Map.of() : registry.getEverything();
        int landCount = 0;
        int gateCount = 0;
        List<WorldBillOfMaterials.Entry> fixtureEntries = new ArrayList<>();

        Map<UUID, Plot> plotsById = new HashMap<>();
        for (Thing thing : everything.values()) {
            if (thing instanceof Plot plot) {
                plotsById.put(plot.getId(), plot);
                if (plot.getPlotKind() == PlotKind.LAND) {
                    landCount++;
                }
            }
        }

        for (Thing thing : everything.values()) {
            if (thing instanceof Gate) {
                gateCount++;
            } else if (thing instanceof Item item && item.isFixture()) {
                String notes = "fixture; " + resolveFixtureLocation(item, everything, plotsById);
                fixtureEntries.add(new WorldBillOfMaterials.Entry(1, item.getLabel(), notes));
            }
        }

        List<WorldBillOfMaterials.Section> sections = List.of(
                new WorldBillOfMaterials.Section(
                        "Map",
                        List.of(
                                new WorldBillOfMaterials.Entry(landCount, "Land plots", null),
                                new WorldBillOfMaterials.Entry(gateCount, "Gates", null)
                        )
                ),
                new WorldBillOfMaterials.Section(
                        "Fixtures",
                        fixtureEntries
                )
        );

        return new WorldBillOfMaterials("World Bill of Materials", sections);
    }

    private static String resolveFixtureLocation(Item item, Map<UUID, Thing> everything, Map<UUID, Plot> plotsById) {
        UUID currentId = item.getId();
        UUID ownerId = item.getOwnerId();
        int steps = 0;
        while (ownerId != null && steps < everything.size() + 1) {
            Thing owner = everything.get(ownerId);
            if (owner == null) {
                return "owner-chain-invalid";
            }
            if (owner instanceof Plot plot) {
                if (plot.getPlotKind() == PlotKind.LAND) {
                    String label = plot.getLabel() == null ? "" : plot.getLabel();
                    return label.isBlank() ? "owner-chain-invalid" : label;
                }
                return "owner-chain-invalid";
            }
            ownerId = owner.getOwnerId();
            steps++;
        }
        return "owner-chain-invalid";
    }
}

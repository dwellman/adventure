package com.demo.adventure.authoring.save.io;

import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

final class GameSaveYamlPlotBuilder {
    private GameSaveYamlPlotBuilder() {
    }

    static List<Map<String, Object>> structuredPlots(GameSave save) {
        Map<UUID, WorldRecipe.PlotSpec> plotById = save.plots().stream()
                .collect(Collectors.toMap(WorldRecipe.PlotSpec::plotId, Function.identity()));
        Map<UUID, UUID> ownerOf = GameSaveYamlWriterSupport.ownerMap(save);

        return save.plots().stream()
                .sorted(Comparator.comparing(WorldRecipe.PlotSpec::name))
                .map(plot -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("plot", plot.name());
                    map.put("plotKey", GameSaveYamlWriterSupport.keyFromName(plot.name()));
                    map.put("name", plot.name());
                    map.put("region", plot.region());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(plot.description()));
                    map.put("gates", new ArrayList<>(gatesForPlot(save, plot.plotId())));
                    map.put("fixtures", new ArrayList<>(fixturesForPlot(save, plot.plotId(), plotById, ownerOf)));
                    map.put("items", new ArrayList<>(itemsForPlot(save, plot.plotId(), plotById, ownerOf)));
                    map.put("actors", new ArrayList<>(actorsForPlot(save, plot.plotId(), plotById, ownerOf)));
                    return map;
                })
                .toList();
    }

    static List<Map<String, Object>> structuredPlots(GardenResult result) {
        Map<UUID, Plot> plots = GameSaveYamlWriterSupport.plotsById(result.registry());
        Map<UUID, UUID> ownerOf = GameSaveYamlWriterSupport.ownerMap(result.registry());

        return plots.values().stream()
                .sorted(Comparator.comparing(Plot::getLabel, Comparator.nullsLast(String::compareTo)))
                .map(plot -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("plot", plot.getLabel());
                    map.put("plotKey", GameSaveYamlWriterSupport.keyFromName(plot.getLabel()));
                    map.put("name", plot.getLabel());
                    map.put("region", plot.getRegion());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(plot));
                    map.put("gates", new ArrayList<>(gatesForPlot(result.registry(), plot.getId())));
                    map.put("fixtures", new ArrayList<>(fixturesForPlot(result, plot.getId(), plots, ownerOf)));
                    map.put("items", new ArrayList<>(itemsForPlot(result, plot.getId(), plots, ownerOf)));
                    map.put("actors", new ArrayList<>(actorsForPlot(result, plot.getId(), plots, ownerOf)));
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> gatesForPlot(GameSave save, UUID plotId) {
        return save.gates().stream()
                .filter(g -> g.fromPlotId().equals(plotId))
                .sorted(Comparator.comparing(WorldRecipe.GateSpec::label))
                .map(gate -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("fromPlot", GameSaveYamlWriterSupport.resolveKey(gate.fromPlotId(), save));
                    map.put("from", GameSaveYamlWriterSupport.resolveName(gate.fromPlotId(), save));
                    map.put("direction", gate.direction().toLongName());
                    map.put("toPlot", GameSaveYamlWriterSupport.resolveKey(gate.toPlotId(), save));
                    map.put("to", GameSaveYamlWriterSupport.resolveName(gate.toPlotId(), save));
                    map.put("visible", gate.visible());
                    map.put("keyString", gate.keyString());
                    map.put("label", gate.label());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(gate.description()));
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> gatesForPlot(KernelRegistry registry, UUID plotId) {
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .flatMap(gate -> GameSaveYamlWriterSupport.gateEntriesForPlot(registry, gate, plotId).stream())
                .sorted(Comparator.comparing(m -> m.getOrDefault("label", "").toString()))
                .toList();
    }

    private static List<Map<String, Object>> fixturesForPlot(
            GameSave save,
            UUID plotId,
            Map<UUID, WorldRecipe.PlotSpec> plots,
            Map<UUID, UUID> ownerOf
    ) {
        return save.fixtures().stream()
                .filter(f -> GameSaveYamlWriterSupport.owningPlotFromRecipe(f.ownerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(WorldRecipe.FixtureSpec::name))
                .map(fixture -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", GameSaveYamlWriterSupport.resolveKey(fixture.id(), save));
                    map.put("name", fixture.name());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(fixture.description()));
                    map.put("ownerKey", GameSaveYamlWriterSupport.resolveKey(fixture.ownerId(), save));
                    map.put("owner", GameSaveYamlWriterSupport.resolveName(fixture.ownerId(), save));
                    map.put("visible", fixture.visible());
                    Map<String, Object> cells = GameSaveYamlWriterSupport.cellsToYamlSpecs(fixture.cells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> fixturesForPlot(
            GardenResult result,
            UUID plotId,
            Map<UUID, Plot> plots,
            Map<UUID, UUID> ownerOf
    ) {
        KernelRegistry registry = result.registry();
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(f -> GameSaveYamlWriterSupport.owningPlotFromRegistry(f.getOwnerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(Item::getLabel, Comparator.nullsLast(String::compareTo)))
                .map(fixture -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", GameSaveYamlWriterSupport.resolveKey(fixture.getId(), registry));
                    map.put("name", fixture.getLabel());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(fixture));
                    map.put("ownerKey", GameSaveYamlWriterSupport.resolveKey(fixture.getOwnerId(), registry));
                    map.put("owner", GameSaveYamlWriterSupport.resolveName(fixture.getOwnerId(), registry));
                    map.put("visible", fixture.isVisible());
                    Map<String, Object> cells = GameSaveYamlWriterSupport.cellsToYamlCells(fixture.getCells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> itemsForPlot(
            GameSave save,
            UUID plotId,
            Map<UUID, WorldRecipe.PlotSpec> plots,
            Map<UUID, UUID> ownerOf
    ) {
        return save.items().stream()
                .filter(i -> GameSaveYamlWriterSupport.owningPlotFromRecipe(i.ownerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(GameSave.ItemRecipe::name))
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", GameSaveYamlWriterSupport.resolveKey(item.id(), save));
                    map.put("name", item.name());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(item.description()));
                    map.put("ownerKey", GameSaveYamlWriterSupport.resolveKey(item.ownerId(), save));
                    map.put("owner", GameSaveYamlWriterSupport.resolveName(item.ownerId(), save));
                    map.put("visible", item.visible());
                    map.put("fixture", item.fixture());
                    map.put("keyString", item.keyString());
                    map.put("footprintWidth", item.footprintWidth());
                    map.put("footprintHeight", item.footprintHeight());
                    map.put("capacityWidth", item.capacityWidth());
                    map.put("capacityHeight", item.capacityHeight());
                    if (item.weaponDamage() > 0) {
                        map.put("weaponDamage", item.weaponDamage());
                    }
                    if (item.armorMitigation() > 0) {
                        map.put("armorMitigation", item.armorMitigation());
                    }
                    Map<String, Object> cells = GameSaveYamlWriterSupport.cellsToYamlSpecs(item.cells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> itemsForPlot(
            GardenResult result,
            UUID plotId,
            Map<UUID, Plot> plots,
            Map<UUID, UUID> ownerOf
    ) {
        KernelRegistry registry = result.registry();
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> !i.isFixture())
                .filter(i -> GameSaveYamlWriterSupport.owningPlotFromRegistry(i.getOwnerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(Item::getLabel, Comparator.nullsLast(String::compareTo)))
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", GameSaveYamlWriterSupport.resolveKey(item.getId(), registry));
                    map.put("name", item.getLabel());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(item));
                    map.put("ownerKey", GameSaveYamlWriterSupport.resolveKey(item.getOwnerId(), registry));
                    map.put("owner", GameSaveYamlWriterSupport.resolveName(item.getOwnerId(), registry));
                    map.put("visible", item.isVisible());
                    map.put("fixture", false);
                    map.put("keyString", item.getKey());
                    map.put("footprintWidth", item.getFootprintWidth());
                    map.put("footprintHeight", item.getFootprintHeight());
                    map.put("capacityWidth", item.getCapacityWidth());
                    map.put("capacityHeight", item.getCapacityHeight());
                    if (item.getWeaponDamage() > 0) {
                        map.put("weaponDamage", item.getWeaponDamage());
                    }
                    if (item.getArmorMitigation() > 0) {
                        map.put("armorMitigation", item.getArmorMitigation());
                    }
                    Map<String, Object> cells = GameSaveYamlWriterSupport.cellsToYamlCells(item.getCells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> actorsForPlot(
            GameSave save,
            UUID plotId,
            Map<UUID, WorldRecipe.PlotSpec> plots,
            Map<UUID, UUID> ownerOf
    ) {
        return save.actors().stream()
                .filter(a -> GameSaveYamlWriterSupport.owningPlotFromRecipe(a.ownerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(GameSave.ActorRecipe::name))
                .map(actor -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", GameSaveYamlWriterSupport.resolveKey(actor.id(), save));
                    map.put("name", actor.name());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(actor.description()));
                    map.put("ownerKey", GameSaveYamlWriterSupport.resolveKey(actor.ownerId(), save));
                    map.put("owner", GameSaveYamlWriterSupport.resolveName(actor.ownerId(), save));
                    map.put("visible", actor.visible());
                    map.put("skills", actor.skills());
                    if (actor.equippedMainHandItemId() != null) {
                        map.put("equippedMainHandItemId", GameSaveYamlWriterSupport.resolveKey(actor.equippedMainHandItemId(), save));
                    }
                    if (actor.equippedBodyItemId() != null) {
                        map.put("equippedBodyItemId", GameSaveYamlWriterSupport.resolveKey(actor.equippedBodyItemId(), save));
                    }
                    Map<String, Object> cells = GameSaveYamlWriterSupport.cellsToYamlSpecs(actor.cells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> actorsForPlot(
            GardenResult result,
            UUID plotId,
            Map<UUID, Plot> plots,
            Map<UUID, UUID> ownerOf
    ) {
        KernelRegistry registry = result.registry();
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(a -> GameSaveYamlWriterSupport.owningPlotFromRegistry(a.getOwnerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(Actor::getLabel, Comparator.nullsLast(String::compareTo)))
                .map(actor -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", GameSaveYamlWriterSupport.resolveKey(actor.getId(), registry));
                    map.put("name", actor.getLabel());
                    map.put("description", GameSaveYamlWriterSupport.descriptionEntries(actor));
                    map.put("ownerKey", GameSaveYamlWriterSupport.resolveKey(actor.getOwnerId(), registry));
                    map.put("owner", GameSaveYamlWriterSupport.resolveName(actor.getOwnerId(), registry));
                    map.put("visible", actor.isVisible());
                    map.put("skills", actor.getSkills());
                    if (actor.getEquippedMainHandItemId() != null) {
                        map.put("equippedMainHandItemId", GameSaveYamlWriterSupport.resolveKey(actor.getEquippedMainHandItemId(), registry));
                    }
                    if (actor.getEquippedBodyItemId() != null) {
                        map.put("equippedBodyItemId", GameSaveYamlWriterSupport.resolveKey(actor.getEquippedBodyItemId(), registry));
                    }
                    Map<String, Object> cells = GameSaveYamlWriterSupport.cellsToYamlCells(actor.getCells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }
}

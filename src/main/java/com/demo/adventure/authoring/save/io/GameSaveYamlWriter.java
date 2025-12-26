package com.demo.adventure.authoring.save.io;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.DescriptionVersion;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Utility to emit a {@link GameSave} into a deterministic YAML document.
 */
public final class GameSaveYamlWriter {
    private GameSaveYamlWriter() {
    }

    /**
     * Write a {@link GameSave} to disk as YAML using deterministic ordering.
     *
     * @param save snapshot to emit
     * @param path target file path
     * @throws IOException when the file cannot be written
     */
    public static void write(GameSave save, Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, toYaml(save), StandardCharsets.UTF_8);
    }

    /**
     * Write a {@link GardenResult} (registry after Gardener pass) to disk as YAML using deterministic ordering.
     *
     * @param result gardened world result
     * @param path   target file path
     * @throws IOException when the file cannot be written
     */
    public static void write(GardenResult result, Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        Files.writeString(path, toYaml(result), StandardCharsets.UTF_8);
    }

    /**
     * Render a {@link GameSave} as a YAML string using deterministic ordering.
     *
     * @param save snapshot to emit
     * @return YAML text
     */
    public static String toYaml(GameSave save) {
        Objects.requireNonNull(save, "save");
        Yaml yaml = new Yaml(options());
        return yaml.dump(toDocument(save));
    }

    /**
     * Render a {@link GardenResult} as a YAML string using deterministic ordering.
     *
     * @param result gardened world to emit
     * @return YAML text
     */
    public static String toYaml(GardenResult result) {
        Objects.requireNonNull(result, "result");
        Yaml yaml = new Yaml(options());
        return yaml.dump(toDocument(result));
    }

    private static Map<String, Object> toDocument(GameSave save) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("seed", save.seed());
        root.put("startPlot", resolveName(save.startPlotId(), save));
        root.put("startPlotKey", resolveKey(save.startPlotId(), save));
        root.put("preamble", preamble(save));
        root.put("plots", structuredPlots(save));
        return root;
    }

    private static Map<String, Object> toDocument(GardenResult result) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("seed", result.seed());
        root.put("startPlot", resolveName(result.startPlotId(), result.registry()));
        root.put("startPlotKey", resolveKey(result.startPlotId(), result.registry()));
        root.put("preamble", preamble(result));
        root.put("plots", structuredPlots(result));
        return root;
    }

    private static List<Map<String, Object>> structuredPlots(GameSave save) {
        Map<UUID, WorldRecipe.PlotSpec> plotById = save.plots().stream()
                .collect(Collectors.toMap(WorldRecipe.PlotSpec::plotId, Function.identity()));
        Map<UUID, UUID> ownerOf = ownerMap(save);

        return save.plots().stream()
                .sorted(Comparator.comparing(WorldRecipe.PlotSpec::name))
                .map(plot -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("plot", plot.name());
                    map.put("plotKey", keyFromName(plot.name()));
                    map.put("name", plot.name());
                    map.put("region", plot.region());
                    map.put("description", descriptionEntries(plot.description()));
                    map.put("gates", new java.util.ArrayList<>(gatesForPlot(save, plot.plotId())));
                    map.put("fixtures", new java.util.ArrayList<>(fixturesForPlot(save, plot.plotId(), plotById, ownerOf)));
                    map.put("items", new java.util.ArrayList<>(itemsForPlot(save, plot.plotId(), plotById, ownerOf)));
                    map.put("actors", new java.util.ArrayList<>(actorsForPlot(save, plot.plotId(), plotById, ownerOf)));
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> structuredPlots(GardenResult result) {
        Map<UUID, Plot> plots = plotsById(result.registry());
        Map<UUID, UUID> ownerOf = ownerMap(result.registry());

        return plots.values().stream()
                .sorted(Comparator.comparing(Plot::getLabel, Comparator.nullsLast(String::compareTo)))
                .map(plot -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("plot", plot.getLabel());
                    map.put("plotKey", keyFromName(plot.getLabel()));
                    map.put("name", plot.getLabel());
                    map.put("region", plot.getRegion());
                    map.put("description", descriptionEntries(plot));
                    map.put("gates", new java.util.ArrayList<>(gatesForPlot(result.registry(), plot.getId())));
                    map.put("fixtures", new java.util.ArrayList<>(fixturesForPlot(result, plot.getId(), plots, ownerOf)));
                    map.put("items", new java.util.ArrayList<>(itemsForPlot(result, plot.getId(), plots, ownerOf)));
                    map.put("actors", new java.util.ArrayList<>(actorsForPlot(result, plot.getId(), plots, ownerOf)));
                    return map;
                })
                .toList();
    }

    private static DumperOptions options() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setWidth(120);
        return options;
    }

    private static String resolveName(UUID id, GameSave save) {
        if (id == null) {
            return null;
        }
        return idToNameMap(save).getOrDefault(id, id.toString());
    }

    private static String resolveName(UUID id, KernelRegistry registry) {
        if (id == null || registry == null) {
            return null;
        }
        Thing thing = registry.get(id);
        return thing == null ? id.toString() : thing.getLabel();
    }

    private static Map<UUID, String> idToNameMap(GameSave save) {
        Map<UUID, String> map = new LinkedHashMap<>();
        save.plots().forEach(p -> map.put(p.plotId(), p.name()));
        save.fixtures().forEach(f -> map.put(f.id(), f.name()));
        save.items().forEach(i -> map.put(i.id(), i.name()));
        save.actors().forEach(a -> map.put(a.id(), a.name()));
        return map;
    }

    private static String resolveKey(UUID id, GameSave save) {
        if (id == null) {
            return null;
        }
        return idToKeyMap(save).getOrDefault(id, id.toString());
    }

    private static Map<UUID, String> idToKeyMap(GameSave save) {
        Map<UUID, String> map = new LinkedHashMap<>();
        save.plots().forEach(p -> map.put(p.plotId(), keyFromName(p.name())));
        save.fixtures().forEach(f -> map.put(f.id(), keyFromName(f.name())));
        save.items().forEach(i -> map.put(i.id(), keyFromName(i.name())));
        save.actors().forEach(a -> map.put(a.id(), keyFromName(a.name())));
        return map;
    }

    private static String resolveKey(UUID id, KernelRegistry registry) {
        if (id == null || registry == null) {
            return null;
        }
        Thing thing = registry.get(id);
        return thing == null ? id.toString() : keyFromName(thing.getLabel());
    }

    private static String preamble(GameSave save) {
        if (save.preamble() != null && !save.preamble().isBlank()) {
            return save.preamble();
        }
        return actorPreamble(save.startPlotId(), save.actors());
    }

    private static String preamble(GardenResult result) {
        if (result.registry() == null) {
            return "";
        }
        return result.registry().getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(actor -> result.startPlotId().equals(actor.getOwnerId()))
                .map(Actor::getDescription)
                .findFirst()
                .orElse("");
    }

    private static String actorPreamble(UUID startPlotId, List<GameSave.ActorRecipe> actors) {
        if (startPlotId == null || actors == null) {
            return "";
        }
        return actors.stream()
                .filter(actor -> startPlotId.equals(actor.ownerId()))
                .map(GameSave.ActorRecipe::description)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    private static String keyFromName(String name) {
        if (name == null) {
            return "";
        }
        String lower = name.trim().toLowerCase();
        StringBuilder sb = new StringBuilder();
        boolean dash = false;
        for (char c : lower.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                dash = false;
            } else {
                if (!dash) {
                    sb.append('-');
                    dash = true;
                }
            }
        }
        String result = sb.toString();
        while (result.startsWith("-")) {
            result = result.substring(1);
        }
        while (result.endsWith("-")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static List<Map<String, Object>> gatesForPlot(GameSave save, UUID plotId) {
        return save.gates().stream()
                .filter(g -> g.fromPlotId().equals(plotId))
                .sorted(Comparator.comparing(WorldRecipe.GateSpec::label))
                .map(gate -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("fromPlot", resolveKey(gate.fromPlotId(), save));
                    map.put("from", resolveName(gate.fromPlotId(), save));
                    map.put("direction", gate.direction().toLongName());
                    map.put("toPlot", resolveKey(gate.toPlotId(), save));
                    map.put("to", resolveName(gate.toPlotId(), save));
                    map.put("visible", gate.visible());
                    map.put("keyString", gate.keyString());
                    map.put("label", gate.label());
                    map.put("description", descriptionEntries(gate.description()));
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> gatesForPlot(KernelRegistry registry, UUID plotId) {
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .flatMap(gate -> gateEntriesForPlot(registry, gate, plotId).stream())
                .sorted(Comparator.comparing(m -> m.getOrDefault("label", "").toString()))
                .toList();
    }

    private static List<Map<String, Object>> fixturesForPlot(GameSave save, UUID plotId, Map<UUID, WorldRecipe.PlotSpec> plots, Map<UUID, UUID> ownerOf) {
        return save.fixtures().stream()
                .filter(f -> owningPlotFromRecipe(f.ownerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(WorldRecipe.FixtureSpec::name))
                .map(fixture -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", resolveKey(fixture.id(), save));
                    map.put("name", fixture.name());
                    map.put("description", descriptionEntries(fixture.description()));
                    map.put("ownerKey", resolveKey(fixture.ownerId(), save));
                    map.put("owner", resolveName(fixture.ownerId(), save));
                    map.put("visible", fixture.visible());
                    Map<String, Object> cells = cellsToYamlSpecs(fixture.cells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> fixturesForPlot(GardenResult result, UUID plotId, Map<UUID, Plot> plots, Map<UUID, UUID> ownerOf) {
        KernelRegistry registry = result.registry();
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(f -> owningPlotFromRegistry(f.getOwnerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(Item::getLabel, Comparator.nullsLast(String::compareTo)))
                .map(fixture -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", resolveKey(fixture.getId(), registry));
                    map.put("name", fixture.getLabel());
                    map.put("description", descriptionEntries(fixture));
                    map.put("ownerKey", resolveKey(fixture.getOwnerId(), registry));
                    map.put("owner", resolveName(fixture.getOwnerId(), registry));
                    map.put("visible", fixture.isVisible());
                    Map<String, Object> cells = cellsToYamlCells(fixture.getCells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> itemsForPlot(GameSave save, UUID plotId, Map<UUID, WorldRecipe.PlotSpec> plots, Map<UUID, UUID> ownerOf) {
        return save.items().stream()
                .filter(i -> owningPlotFromRecipe(i.ownerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(GameSave.ItemRecipe::name))
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", resolveKey(item.id(), save));
                    map.put("name", item.name());
                    map.put("description", descriptionEntries(item.description()));
                    map.put("ownerKey", resolveKey(item.ownerId(), save));
                    map.put("owner", resolveName(item.ownerId(), save));
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
                    Map<String, Object> cells = cellsToYamlSpecs(item.cells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> itemsForPlot(GardenResult result, UUID plotId, Map<UUID, Plot> plots, Map<UUID, UUID> ownerOf) {
        KernelRegistry registry = result.registry();
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> !i.isFixture())
                .filter(i -> owningPlotFromRegistry(i.getOwnerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(Item::getLabel, Comparator.nullsLast(String::compareTo)))
                .map(item -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", resolveKey(item.getId(), registry));
                    map.put("name", item.getLabel());
                    map.put("description", descriptionEntries(item));
                    map.put("ownerKey", resolveKey(item.getOwnerId(), registry));
                    map.put("owner", resolveName(item.getOwnerId(), registry));
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
                    Map<String, Object> cells = cellsToYamlCells(item.getCells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> actorsForPlot(GameSave save, UUID plotId, Map<UUID, WorldRecipe.PlotSpec> plots, Map<UUID, UUID> ownerOf) {
        return save.actors().stream()
                .filter(a -> owningPlotFromRecipe(a.ownerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(GameSave.ActorRecipe::name))
                .map(actor -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", resolveKey(actor.id(), save));
                    map.put("name", actor.name());
                    map.put("description", descriptionEntries(actor.description()));
                    map.put("ownerKey", resolveKey(actor.ownerId(), save));
                    map.put("owner", resolveName(actor.ownerId(), save));
                    map.put("visible", actor.visible());
                    map.put("skills", actor.skills());
                    if (actor.equippedMainHandItemId() != null) {
                        map.put("equippedMainHandItemId", resolveKey(actor.equippedMainHandItemId(), save));
                    }
                    if (actor.equippedBodyItemId() != null) {
                        map.put("equippedBodyItemId", resolveKey(actor.equippedBodyItemId(), save));
                    }
                    Map<String, Object> cells = cellsToYamlSpecs(actor.cells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static List<Map<String, Object>> actorsForPlot(GardenResult result, UUID plotId, Map<UUID, Plot> plots, Map<UUID, UUID> ownerOf) {
        KernelRegistry registry = result.registry();
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(a -> owningPlotFromRegistry(a.getOwnerId(), plots, ownerOf).equals(plotId))
                .sorted(Comparator.comparing(Actor::getLabel, Comparator.nullsLast(String::compareTo)))
                .map(actor -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("key", resolveKey(actor.getId(), registry));
                    map.put("name", actor.getLabel());
                    map.put("description", descriptionEntries(actor));
                    map.put("ownerKey", resolveKey(actor.getOwnerId(), registry));
                    map.put("owner", resolveName(actor.getOwnerId(), registry));
                    map.put("visible", actor.isVisible());
                    map.put("skills", actor.getSkills());
                    if (actor.getEquippedMainHandItemId() != null) {
                        map.put("equippedMainHandItemId", resolveKey(actor.getEquippedMainHandItemId(), registry));
                    }
                    if (actor.getEquippedBodyItemId() != null) {
                        map.put("equippedBodyItemId", resolveKey(actor.getEquippedBodyItemId(), registry));
                    }
                    Map<String, Object> cells = cellsToYamlCells(actor.getCells());
                    if (cells != null) {
                        map.put("cells", cells);
                    }
                    return map;
                })
                .toList();
    }

    private static Map<String, Object> cellsToYamlSpecs(Map<String, CellSpec> cells) {
        if (cells == null || cells.isEmpty()) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        cells.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    CellSpec spec = entry.getValue();
                    if (spec == null) {
                        return;
                    }
                    Map<String, Object> cell = new LinkedHashMap<>();
                    cell.put("capacity", spec.capacity());
                    cell.put("amount", spec.amount());
                    out.put(entry.getKey(), cell);
                });
        return out.isEmpty() ? null : out;
    }

    private static Map<String, Object> cellsToYamlCells(Map<String, Cell> cells) {
        if (cells == null || cells.isEmpty()) {
            return null;
        }
        Map<String, Object> out = new LinkedHashMap<>();
        cells.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    Cell cellSpec = entry.getValue();
                    if (cellSpec == null) {
                        return;
                    }
                    Map<String, Object> cell = new LinkedHashMap<>();
                    cell.put("capacity", cellSpec.getCapacity());
                    cell.put("amount", cellSpec.getAmount());
                    out.put(entry.getKey(), cell);
                });
        return out.isEmpty() ? null : out;
    }

    private static Map<UUID, UUID> ownerMap(GameSave save) {
        Map<UUID, UUID> map = new LinkedHashMap<>();
        save.fixtures().forEach(f -> map.put(f.id(), f.ownerId()));
        save.items().forEach(i -> map.put(i.id(), i.ownerId()));
        save.actors().forEach(a -> map.put(a.id(), a.ownerId()));
        return map;
    }

    private static Map<UUID, UUID> ownerMap(KernelRegistry registry) {
        Map<UUID, UUID> map = new LinkedHashMap<>();
        if (registry == null) {
            return map;
        }
        registry.getEverything().values().forEach(t -> map.put(t.getId(), t.getOwnerId()));
        return map;
    }

    private static UUID owningPlotFromRecipe(UUID id, Map<UUID, WorldRecipe.PlotSpec> plots, Map<UUID, UUID> ownerOf) {
        Set<UUID> visited = new LinkedHashSet<>();
        UUID current = id;
        while (current != null) {
            if (plots.containsKey(current)) {
                return current;
            }
            if (!visited.add(current)) {
                throw new IllegalStateException("Ownership cycle detected for id " + current);
            }
            current = ownerOf.get(current);
        }
        throw new IllegalStateException("Owner chain did not resolve to a plot for id " + id);
    }

    private static UUID owningPlotFromRegistry(UUID id, Map<UUID, Plot> plots, Map<UUID, UUID> ownerOf) {
        Set<UUID> visited = new LinkedHashSet<>();
        UUID current = id;
        while (current != null) {
            if (plots.containsKey(current)) {
                return current;
            }
            if (!visited.add(current)) {
                throw new IllegalStateException("Ownership cycle detected for id " + current);
            }
            current = ownerOf.get(current);
        }
        throw new IllegalStateException("Owner chain did not resolve to a plot for id " + id);
    }

    private static Map<UUID, Plot> plotsById(KernelRegistry registry) {
        Map<UUID, Plot> map = new LinkedHashMap<>();
        if (registry == null) {
            return map;
        }
        registry.getEverything().values().forEach(t -> {
            if (t instanceof Plot plot) {
                map.put(plot.getId(), plot);
            }
        });
        return map;
    }

    private static List<Map<String, Object>> gateEntriesForPlot(KernelRegistry registry, Gate gate, UUID plotId) {
        List<Map<String, Object>> entries = new ArrayList<>();
        if (gate == null || plotId == null) {
            return entries;
        }
        if (Objects.equals(gate.getPlotAId(), plotId)) {
            entries.add(gateEntry(registry, gate.getPlotAId(), gate.getDirection(), gate));
        }
        if (Objects.equals(gate.getPlotBId(), plotId)) {
            entries.add(gateEntry(registry, gate.getPlotBId(), Direction.oppositeOf(gate.getDirection()), gate));
        }
        return entries;
    }

    private static Map<String, Object> gateEntry(KernelRegistry registry, UUID fromPlotId, Direction direction, Gate gate) {
        Map<String, Object> map = new LinkedHashMap<>();
        UUID toPlotId = gate.otherSide(fromPlotId);

        String fromName = resolveName(fromPlotId, registry);
        String toName = resolveName(toPlotId, registry);
        String computedLabel = (fromName == null ? "" : fromName) + " -> " + (toName == null ? "" : toName);
        map.put("label", computedLabel.trim());
        map.put("direction", direction == null ? null : direction.toLongName());

        Map<String, Object> plotA = new LinkedHashMap<>();
        plotA.put("fromPlot", resolveKey(fromPlotId, registry));
        plotA.put("toPlot", resolveKey(toPlotId, registry));
        plotA.put("visible", gate.isVisible());
        plotA.put("keyString", gate.getKeyString());
        plotA.put("description", descriptionEntries(gate, fromPlotId));
        map.put("plotA", plotA);

        Map<String, Object> plotB = new LinkedHashMap<>();
        plotB.put("fromPlot", resolveKey(toPlotId, registry));
        plotB.put("toPlot", resolveKey(fromPlotId, registry));
        plotB.put("visible", gate.isVisible());
        plotB.put("keyString", gate.getKeyString());
        plotB.put("description", descriptionEntries(gate, toPlotId));
        map.put("plotB", plotB);
        return map;
    }

    private static List<Map<String, Object>> descriptionEntries(Thing thing) {
        List<DescriptionVersion> history = thing.getDescriptionHistory();
        if (history == null || history.isEmpty()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("text", thing.getDescription());
            entry.put("worldClock", -1);
            return List.of(entry);
        }
        return history.stream()
                .map(v -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("text", v.text());
                    entry.put("worldClock", v.worldClock());
                    return entry;
                })
                .toList();
    }

    private static List<Map<String, Object>> descriptionEntries(Gate gate, UUID fromPlotId) {
        if (gate == null) {
            return List.of();
        }
        List<DescriptionVersion> history = gate.getDescriptionHistoryFrom(fromPlotId);
        if (history == null || history.isEmpty()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("text", gate.getDescriptionFrom(fromPlotId));
            entry.put("worldClock", -1);
            return List.of(entry);
        }
        return history.stream()
                .map(v -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("text", v.text());
                    entry.put("worldClock", v.worldClock());
                    return entry;
                })
                .toList();
    }

    private static List<Map<String, Object>> descriptionEntries(String text) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("text", text);
        entry.put("worldClock", -1);
        return List.of(entry);
    }
}

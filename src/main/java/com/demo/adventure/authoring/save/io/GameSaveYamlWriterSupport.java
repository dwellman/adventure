package com.demo.adventure.authoring.save.io;

import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.DescriptionVersion;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.yaml.snakeyaml.DumperOptions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class GameSaveYamlWriterSupport {
    private GameSaveYamlWriterSupport() {
    }

    static DumperOptions options() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setWidth(120);
        return options;
    }

    static String resolveName(UUID id, GameSave save) {
        if (id == null) {
            return null;
        }
        return idToNameMap(save).getOrDefault(id, id.toString());
    }

    static String resolveName(UUID id, KernelRegistry registry) {
        if (id == null || registry == null) {
            return null;
        }
        Thing thing = registry.get(id);
        return thing == null ? id.toString() : thing.getLabel();
    }

    static String resolveKey(UUID id, GameSave save) {
        if (id == null) {
            return null;
        }
        return idToKeyMap(save).getOrDefault(id, id.toString());
    }

    static String resolveKey(UUID id, KernelRegistry registry) {
        if (id == null || registry == null) {
            return null;
        }
        Thing thing = registry.get(id);
        return thing == null ? id.toString() : keyFromName(thing.getLabel());
    }

    static String preamble(GameSave save) {
        if (save.preamble() != null && !save.preamble().isBlank()) {
            return save.preamble();
        }
        return actorPreamble(save.startPlotId(), save.actors());
    }

    static String preamble(GardenResult result) {
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

    static String keyFromName(String name) {
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

    static Map<UUID, UUID> ownerMap(GameSave save) {
        Map<UUID, UUID> map = new LinkedHashMap<>();
        save.fixtures().forEach(f -> map.put(f.id(), f.ownerId()));
        save.items().forEach(i -> map.put(i.id(), i.ownerId()));
        save.actors().forEach(a -> map.put(a.id(), a.ownerId()));
        return map;
    }

    static Map<UUID, UUID> ownerMap(KernelRegistry registry) {
        Map<UUID, UUID> map = new LinkedHashMap<>();
        if (registry == null) {
            return map;
        }
        registry.getEverything().values().forEach(t -> map.put(t.getId(), t.getOwnerId()));
        return map;
    }

    static UUID owningPlotFromRecipe(UUID id, Map<UUID, WorldRecipe.PlotSpec> plots, Map<UUID, UUID> ownerOf) {
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

    static UUID owningPlotFromRegistry(UUID id, Map<UUID, Plot> plots, Map<UUID, UUID> ownerOf) {
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

    static Map<UUID, Plot> plotsById(KernelRegistry registry) {
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

    static List<Map<String, Object>> gateEntriesForPlot(KernelRegistry registry, Gate gate, UUID plotId) {
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

    static Map<String, Object> gateEntry(KernelRegistry registry, UUID fromPlotId, Direction direction, Gate gate) {
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

    static List<Map<String, Object>> descriptionEntries(Thing thing) {
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

    static List<Map<String, Object>> descriptionEntries(Gate gate, UUID fromPlotId) {
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

    static List<Map<String, Object>> descriptionEntries(String text) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("text", text);
        entry.put("worldClock", -1);
        return List.of(entry);
    }

    static Map<String, Object> cellsToYamlSpecs(Map<String, CellSpec> cells) {
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

    static Map<String, Object> cellsToYamlCells(Map<String, Cell> cells) {
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

    private static Map<UUID, String> idToNameMap(GameSave save) {
        Map<UUID, String> map = new LinkedHashMap<>();
        save.plots().forEach(p -> map.put(p.plotId(), p.name()));
        save.fixtures().forEach(f -> map.put(f.id(), f.name()));
        save.items().forEach(i -> map.put(i.id(), i.name()));
        save.actors().forEach(a -> map.put(a.id(), a.name()));
        return map;
    }

    private static Map<UUID, String> idToKeyMap(GameSave save) {
        Map<UUID, String> map = new LinkedHashMap<>();
        save.plots().forEach(p -> map.put(p.plotId(), keyFromName(p.name())));
        save.fixtures().forEach(f -> map.put(f.id(), keyFromName(f.name())));
        save.items().forEach(i -> map.put(i.id(), keyFromName(i.name())));
        save.actors().forEach(a -> map.put(a.id(), keyFromName(a.name())));
        return map;
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
}

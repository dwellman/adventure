package com.demo.adventure.authoring.save.io;

import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.save.ActorRecipeBuilder;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.ItemRecipeBuilder;
import com.demo.adventure.domain.save.WorldRecipe;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Parses a YAML game save into a {@link GameSave} using id-based references only.
 */
public final class GameSaveYamlLoader {
    private static final int AUTO_GRID_WIDTH = 6;

    private GameSaveYamlLoader() {
    }

    /**
     * Load a {@link GameSave} from a YAML file.
     *
     * @param path path to YAML document
     * @return parsed save
     * @throws IOException when the file cannot be read
     */
    public static GameSave load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        String yaml = Files.readString(path, StandardCharsets.UTF_8);
        return load(yaml);
    }

    /**
     * Load a {@link GameSave} from YAML text.
     *
     * @param yamlText YAML content
     * @return parsed save
     */
    @SuppressWarnings("unchecked")
    public static GameSave load(String yamlText) {
        Objects.requireNonNull(yamlText, "yamlText");
        Yaml yaml = new Yaml();
        Object raw = yaml.load(yamlText);
        if (!(raw instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("YAML root must be a mapping");
        }

        long seed = parseLong(root.get("seed"), "seed");
        String preamble = optionalString(root.get("preamble"), "preamble");
        Map<String, UUID> plotKeys = new HashMap<>();
        Map<String, UUID> thingKeys = new HashMap<>();

        List<WorldRecipe.PlotSpec> plots = new ArrayList<>();
        List<PlotInput> plotInputs = new ArrayList<>();
        List<GateInput> gateInputs = new ArrayList<>();
        List<FixtureInput> fixtureInputs = new ArrayList<>();
        List<ItemInput> itemInputs = new ArrayList<>();
        List<ActorInput> actorInputs = new ArrayList<>();
        for (Object entry : list(root.get("plots"), "plots")) {
            Map<String, Object> map = map(entry, "plot");
            String plotName = str(map.containsKey("name") ? map.get("name") : map.get("plot"), "plots.name");
            String plotKey = key(map.get("plotKey"), map.get("plot"), map.get("name"), "plots.plotKey");
            UUID plotId = uuid("plot", plotKey);
            if (plotKeys.put(plotKey, plotId) != null) {
                throw new IllegalArgumentException("Duplicate plot key: " + plotKey);
            }
            Integer locationX = optionalInt(map.get("locationX"), "plots.locationX");
            Integer locationY = optionalInt(map.get("locationY"), "plots.locationY");
            if ((locationX == null) != (locationY == null)) {
                throw new IllegalArgumentException("plots.locationX and plots.locationY must both be set or both be omitted for plotKey: " + plotKey);
            }
            plotInputs.add(new PlotInput(
                    plotKey,
                    plotId,
                    plotName,
                    str(map.get("region"), "plots.region"),
                    description(map.get("description"), "plots.description"),
                    locationX,
                    locationY
            ));

            for (Object gateObj : list(map.get("gates"), "plots.gates")) {
                Map<String, Object> gateMap = map(gateObj, "plots.gates.gate");
                Map<String, Object> plotA = gateMap.get("plotA") instanceof Map<?, ?> rawA ? (Map<String, Object>) rawA : null;
                String fromKey = plotA == null ? plotKey : key(plotA.get("fromPlot"), null, null, "plots.gates.plotA.fromPlot");
                String toKey = plotA == null ? key(gateMap.get("toPlot"), gateMap.get("toPlotKey"), null, "plots.gates.toPlot")
                        : key(plotA.get("toPlot"), null, null, "plots.gates.plotA.toPlot");
                gateInputs.add(new GateInput(
                        fromKey,
                        toKey,
                        str(gateMap.get("direction"), "plots.gates.direction"),
                        plotA == null ? bool(gateMap.get("visible")) : bool(plotA.get("visible")),
                        plotA == null ? keyStringOrDefault(gateMap.get("keyString"), "plots.gates.keyString") : keyStringOrDefault(plotA.get("keyString"), "plots.gates.plotA.keyString"),
                        str(gateMap.get("label"), "plots.gates.label"),
                        description(plotA == null ? gateMap.get("description") : plotA.get("description"), "plots.gates.description")
                ));
            }

            for (Object fixtureObj : list(map.get("fixtures"), "plots.fixtures")) {
                Map<String, Object> fixtureMap = map(fixtureObj, "plots.fixtures.fixture");
                String key = key(fixtureMap.get("key"), null, null, "plots.fixtures.key");
                UUID id = uuid("fixture", key);
                if (thingKeys.put(key, id) != null) {
                    throw new IllegalArgumentException("Duplicate fixture key: " + key);
                }
                fixtureInputs.add(new FixtureInput(
                        key,
                        id,
                        str(fixtureMap.get("name"), "plots.fixtures.name"),
                        description(fixtureMap.get("description"), "plots.fixtures.description"),
                        optionalKey(fixtureMap.get("ownerKey"), null, plotKey),
                        bool(fixtureMap.get("visible")),
                        parseCells(fixtureMap.get("cells"), "plots.fixtures.cells")
                ));
            }

            for (Object itemObj : list(map.get("items"), "plots.items")) {
                Map<String, Object> itemMap = map(itemObj, "plots.items.item");
                String key = key(itemMap.get("key"), null, null, "plots.items.key");
                UUID id = uuid("item", key);
                if (thingKeys.put(key, id) != null) {
                    throw new IllegalArgumentException("Duplicate item key: " + key);
                }
                itemInputs.add(new ItemInput(
                        key,
                        id,
                        str(itemMap.get("name"), "plots.items.name"),
                        description(itemMap.get("description"), "plots.items.description"),
                        optionalKey(itemMap.get("ownerKey"), null, plotKey),
                        bool(itemMap.get("visible")),
                        bool(itemMap.get("fixture")),
                        keyStringOrDefault(itemMap.get("keyString"), "plots.items.keyString"),
                        optionalDouble(itemMap.get("footprintWidth"), 0.1),
                        optionalDouble(itemMap.get("footprintHeight"), 0.1),
                        optionalDouble(itemMap.get("capacityWidth"), 0.0),
                        optionalDouble(itemMap.get("capacityHeight"), 0.0),
                        optionalLong(itemMap.get("weaponDamage"), 0L),
                        optionalLong(itemMap.get("armorMitigation"), 0L),
                        parseCells(itemMap.get("cells"), "plots.items.cells")
                ));
            }

            for (Object actorObj : list(map.get("actors"), "plots.actors")) {
                Map<String, Object> actorMap = map(actorObj, "plots.actors.actor");
                String key = key(actorMap.get("key"), null, null, "plots.actors.key");
                UUID id = uuid("actor", key);
                if (thingKeys.put(key, id) != null) {
                    throw new IllegalArgumentException("Duplicate actor key: " + key);
                }
                actorInputs.add(new ActorInput(
                        key,
                        id,
                        str(actorMap.get("name"), "plots.actors.name"),
                        description(actorMap.get("description"), "plots.actors.description"),
                        optionalKey(actorMap.get("ownerKey"), null, plotKey),
                        bool(actorMap.get("visible")),
                        stringList(actorMap.get("skills"), "plots.actors.skills"),
                        optionalKeyOrNull(actorMap.get("equippedMainHandItemId"), "plots.actors.equippedMainHandItemId"),
                        optionalKeyOrNull(actorMap.get("equippedBodyItemId"), "plots.actors.equippedBodyItemId"),
                        parseCells(actorMap.get("cells"), "plots.actors.cells")
                ));
            }
        }

        Map<String, Coord> autoCoords = deriveCoordinates(plotInputs);
        for (PlotInput input : plotInputs) {
            Coord coord = autoCoords.get(input.key());
            int locationX = input.locationX() != null ? input.locationX() : coord.x();
            int locationY = input.locationY() != null ? input.locationY() : coord.y();
            plots.add(new WorldRecipe.PlotSpec(
                    input.id(),
                    input.name(),
                    input.region(),
                    locationX,
                    locationY,
                    input.description()
            ));
        }

        String startPlotKey = key(root.get("startPlotKey"), root.get("startPlot"), null, "startPlotKey");
        UUID startPlotId = requirePlot(startPlotKey, plotKeys);

        List<WorldRecipe.GateSpec> gates = gateInputs.stream()
                .map(input -> new WorldRecipe.GateSpec(
                        requirePlot(input.fromKey(), plotKeys),
                        Direction.parse(input.direction()),
                        requirePlot(input.toKey(), plotKeys),
                        input.visible(),
                        input.keyString(),
                        input.label(),
                        input.description()
                ))
                .toList();

        List<WorldRecipe.FixtureSpec> fixtures = fixtureInputs.stream()
                .map(input -> new WorldRecipe.FixtureSpec(
                        input.id(),
                        input.name(),
                        input.description(),
                        resolveOwner(input.ownerKey(), plotKeys, thingKeys, "fixtures.ownerKey"),
                        input.visible(),
                        input.cells()
                ))
                .toList();

        List<GameSave.ActorRecipe> actors = actorInputs.stream()
                .map(input -> new ActorRecipeBuilder()
                        .withId(input.id())
                        .withName(input.name())
                        .withDescription(input.description())
                        .withOwnerId(resolveOwner(input.ownerKey(), plotKeys, thingKeys, "actors.ownerKey"))
                        .withVisible(input.visible())
                        .withSkills(input.skills())
                        .withEquippedMainHandItemId(resolveOptionalThing(input.equippedMainHandItemKey(), thingKeys, "actors.equippedMainHandItemId"))
                        .withEquippedBodyItemId(resolveOptionalThing(input.equippedBodyItemKey(), thingKeys, "actors.equippedBodyItemId"))
                        .withCells(input.cells())
                        .build())
                .toList();

        List<GameSave.ItemRecipe> items = itemInputs.stream()
                .map(input -> new ItemRecipeBuilder()
                        .withId(input.id())
                        .withName(input.name())
                        .withDescription(input.description())
                        .withOwnerId(resolveOwner(input.ownerKey(), plotKeys, thingKeys, "items.ownerKey"))
                        .withVisible(input.visible())
                        .withFixture(input.fixture())
                        .withKeyString(input.keyString())
                        .withFootprint(input.footprintWidth(), input.footprintHeight())
                        .withCapacity(input.capacityWidth(), input.capacityHeight())
                        .withWeaponDamage(input.weaponDamage())
                        .withArmorMitigation(input.armorMitigation())
                        .withCells(input.cells())
                        .build())
                .toList();

        String resolvedPreamble = preamble.isBlank() ? actorPreamble(startPlotId, actors) : preamble;
        return new GameSave(seed, startPlotId, resolvedPreamble, plots, gates, fixtures, items, actors);
    }

    private static List<?> list(Object value, String field) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Field '" + field + "' must be a list");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object value, String field) {
        if (value instanceof Map<?, ?> raw) {
            return (Map<String, Object>) raw;
        }
        throw new IllegalArgumentException("Field '" + field + "' must be a map");
    }

    private static String str(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        return Objects.toString(value);
    }

    @SuppressWarnings("unchecked")
    private static String description(Object value, String field) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return Objects.toString(value);
        }
        if (value instanceof List<?> list) {
            String last = "";
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> raw) {
                    Object text = ((Map<String, Object>) raw).get("text");
                    if (text != null) {
                        last = text.toString();
                    }
                } else if (entry != null) {
                    last = entry.toString();
                }
            }
            return last;
        }
        throw new IllegalArgumentException("Field '" + field + "' must be a string or list of descriptions");
    }

    private static Map<String, CellSpec> parseCells(Object raw, String field) {
        if (raw == null) {
            return Map.of();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, CellSpec> cells = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String name = entry.getKey().toString();
            if (!(entry.getValue() instanceof Map<?, ?> cellMap)) {
                continue;
            }
            long capacity = parseLong(cellMap.get("capacity"), field + "." + name + ".capacity");
            long amount = optionalLong(cellMap.get("amount"), 0L);
            cells.put(normalizeCellKey(name), new CellSpec(capacity, amount));
        }
        return Map.copyOf(cells);
    }

    private static List<String> stringList(Object value, String field) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    result.add(entry.toString());
                }
            }
            return result;
        }
        if (value instanceof String str) {
            return List.of(str);
        }
        throw new IllegalArgumentException("Field '" + field + "' must be a string or list of strings");
    }

    private static long parseLong(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Field '" + field + "' is not a number: " + value, ex);
        }
    }

    private static long optionalLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(Objects.toString(value));
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean bool(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value == null) {
            return false;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static double optionalDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static Integer optionalInt(Object value, String field) {
        if (value == null) {
            return null;
        }
        return (int) parseLong(value, field);
    }

    private static String optionalString(Object value, String field) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof List<?> list) {
            return description(list, field);
        }
        return Objects.toString(value);
    }

    private static String optionalKey(Object value, Object legacyValue, String fallbackPlotKey) {
        if (value == null) {
            if (legacyValue != null) {
                return key(legacyValue, null, null, "ownerKey");
            }
            return fallbackPlotKey;
        }
        return key(value, null, null, "ownerKey");
    }

    private static String optionalKeyOrNull(Object value, String field) {
        if (value == null) {
            return null;
        }
        return key(value, null, null, field);
    }

    private static String normalizeCellKey(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }

    private static UUID resolveOwner(Object keyValue, Map<String, UUID> plotKeys, Map<String, UUID> thingKeys, String field) {
        String key = key(keyValue, null, null, field);
        UUID found = thingKeys.get(key);
        if (found != null) {
            return found;
        }
        found = plotKeys.get(key);
        if (found != null) {
            return found;
        }
        throw new IllegalArgumentException("Owner not found: " + key);
    }

    private static UUID resolveOptionalThing(String key, Map<String, UUID> thingKeys, String field) {
        if (key == null || key.isBlank()) {
            return null;
        }
        UUID found = thingKeys.get(key);
        if (found != null) {
            return found;
        }
        throw new IllegalArgumentException("Thing not found for " + field + ": " + key);
    }

    private static UUID requirePlot(String key, Map<String, UUID> plotKeys) {
        UUID id = plotKeys.get(key);
        if (id == null) {
            throw new IllegalArgumentException("Plot not found: " + key);
        }
        return id;
    }

    private static UUID uuid(String kind, String key) {
        return UUID.nameUUIDFromBytes((kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static String key(Object primary, Object legacy, Object legacy2, String field) {
        Object value = primary != null ? primary : (legacy != null ? legacy : legacy2);
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        String raw = Objects.toString(value).trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        boolean dash = false;
        for (char c : raw.toCharArray()) {
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
        String normalized = sb.toString();
        while (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        while (normalized.endsWith("-")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Field '" + field + "' is required");
        }
        return normalized;
    }

    private static String keyStringOrDefault(Object value, String field) {
        if (value == null) {
            return "true";
        }
        String text = Objects.toString(value).trim();
        if (text.isEmpty()) {
            return "true";
        }
        return text;
    }

    private static String actorPreamble(UUID startPlotId, List<GameSave.ActorRecipe> actors) {
        if (startPlotId == null) {
            return "";
        }
        return actors.stream()
                .filter(actor -> startPlotId.equals(actor.ownerId()))
                .map(GameSave.ActorRecipe::description)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    private static Map<String, Coord> deriveCoordinates(List<PlotInput> plots) {
        Map<String, Coord> coords = new HashMap<>();
        List<PlotInput> sorted = plots.stream()
                .sorted(Comparator.comparing(PlotInput::region)
                        .thenComparing(PlotInput::key))
                .toList();
        int index = 0;
        for (PlotInput input : sorted) {
            int x = index % AUTO_GRID_WIDTH;
            int y = index / AUTO_GRID_WIDTH;
            coords.put(input.key(), new Coord(x, y));
            index++;
        }
        return coords;
    }

    private record PlotInput(
            String key,
            UUID id,
            String name,
            String region,
            String description,
            Integer locationX,
            Integer locationY
    ) {
    }

    private record Coord(int x, int y) {
    }

    private record GateInput(
            String fromKey,
            String toKey,
            String direction,
            boolean visible,
            String keyString,
            String label,
            String description
    ) {
    }

    private record FixtureInput(
            String key,
            UUID id,
            String name,
            String description,
            String ownerKey,
            boolean visible,
            Map<String, CellSpec> cells
    ) {
    }

    private record ItemInput(
            String key,
            UUID id,
            String name,
            String description,
            String ownerKey,
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
    }

    private record ActorInput(
            String key,
            UUID id,
            String name,
            String description,
            String ownerKey,
            boolean visible,
            List<String> skills,
            String equippedMainHandItemKey,
            String equippedBodyItemKey,
            Map<String, CellSpec> cells
    ) {
    }
}

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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Loader for a structured game definition that splits map/fixtures/items/actors into sub-YAML files.
 *
 * game.yaml shape:
 * id: spy-adventure
 * title: Spy Adventure (Hong Kong 1970)
 * seed: 1970
 * startPlotKey: kowloon-night-market
 * preamble: ...
 * includes:
 *   map: world/map.yaml
 *   fixtures: world/fixtures.yaml
 *   items: world/items.yaml
 *   actors: world/actors.yaml
 *   descriptions: narrative/descriptions.yaml
 */
public final class StructuredGameSaveLoader {
    private StructuredGameSaveLoader() {
    }

    public static GameSave load(Path gameDefinitionPath) throws IOException {
        Objects.requireNonNull(gameDefinitionPath, "gameDefinitionPath");
        Map<String, Object> root = readYaml(gameDefinitionPath);

        long seed = parseLong(root.get("seed"), "seed");
        String preamble = optionalString(root.get("preamble"));
        String startPlotKey = requireString(root.get("startPlotKey"), "startPlotKey");

        Map<String, String> includes = includes(root.get("includes"));
        Path base = gameDefinitionPath.getParent() == null ? Path.of(".") : gameDefinitionPath.getParent();

        Path mapPath = base.resolve(includes.get("map"));
        Map<String, Object> mapSection = readYaml(mapPath);
        Map<String, Object> fixturesSection = readOptionalYaml(base.resolve(includes.get("fixtures")));
        Map<String, Object> itemsSection = readOptionalYaml(base.resolve(includes.get("items")));
        Map<String, Object> actorsSection = readOptionalYaml(base.resolve(includes.get("actors")));
        Map<String, Object> descriptionsSection = readOptionalYaml(base.resolve(includes.get("descriptions")));

        // Grounding: structured-first; fail loud on missing keys/owners.
        ParsedPlots parsedPlots = parsePlots(mapSection.get("plots"));
        List<WorldRecipe.PlotSpec> plots = parsedPlots.plots();
        Map<String, UUID> idsByKey = new HashMap<>(parsedPlots.idsByKey());
        UUID startPlotId = idsByKey.get(startPlotKey);
        if (startPlotId == null) {
            throw new IllegalArgumentException("startPlotKey not found in plots: " + startPlotKey + " (from " + mapPath + ")");
        }

        List<WorldRecipe.GateSpec> gates = parseGates(mapSection.get("gates"), idsByKey);
        List<WorldRecipe.FixtureSpec> fixtures = parseFixtures(fixturesSection.get("fixtures"), idsByKey);
        fixtures.forEach(f -> idsByKey.put(keyFromName(f.name()), f.id()));

        List<GameSave.ActorRecipe> actors = parseActors(actorsSection.get("actors"), idsByKey);
        actors.forEach(a -> idsByKey.put(keyFromName(a.name()), a.id()));

        List<GameSave.ItemRecipe> items = parseItems(itemsSection.get("items"), idsByKey);
        items.forEach(i -> idsByKey.put(keyFromName(i.name()), i.id()));

        applyDescriptionOverrides(descriptionsSection, plots, fixtures, items, actors);

        return new GameSave(seed, startPlotId, preamble, plots, gates, fixtures, items, actors);
    }

    private static Map<String, Object> readYaml(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            throw new IOException("File not found: " + path);
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        Object raw = new Yaml().load(text);
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Root must be a map: " + path);
        }
        //noinspection unchecked
        return (Map<String, Object>) map;
    }

    private static Map<String, Object> readOptionalYaml(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return Map.of();
        }
        return readYaml(path);
    }

    private static Map<String, String> includes(Object raw) {
        Map<String, String> defaults = Map.of(
                "map", "world/map.yaml",
                "fixtures", "world/fixtures.yaml",
                "items", "world/items.yaml",
                "actors", "world/actors.yaml",
                "descriptions", "narrative/descriptions.yaml"
        );
        if (raw == null) {
            return defaults;
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return defaults;
        }
        Map<String, String> includes = new HashMap<>(defaults);
        for (Map.Entry<?, ?> e : map.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) {
                continue;
            }
            includes.put(e.getKey().toString(), e.getValue().toString());
        }
        return includes;
    }

    // NOTE: Plot keys in YAML must be used directly for startPlotKey and ownerKey resolution.
    // Do NOT recompute keys from names; doing so breaks stable references in structured games.
    private static ParsedPlots parsePlots(Object raw) {
        List<WorldRecipe.PlotSpec> plots = new ArrayList<>();
        Map<String, UUID> idsByKey = new HashMap<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String key = requireString(map.get("key"), "plots.key");
            UUID id = uuid("plot", key);
            String name = requireString(map.get("name"), "plots.name");
            String region = requireString(map.get("region"), "plots.region");
            int x = optionalInt(map.get("locationX"));
            int y = optionalInt(map.get("locationY"));
            String description = description(map.get("description"));
            plots.add(new WorldRecipe.PlotSpec(id, name, region, x, y, description));
            idsByKey.put(key, id);
        }
        return new ParsedPlots(plots, idsByKey);
    }

    private static List<WorldRecipe.GateSpec> parseGates(Object raw, Map<String, UUID> idsByKey) {
        List<WorldRecipe.GateSpec> gates = new ArrayList<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String fromKey = requireString(map.get("from"), "gates.from");
            String toKey = requireString(map.get("to"), "gates.to");
            Direction dir = Direction.parse(requireString(map.get("direction"), "gates.direction"));
            boolean visible = bool(map.get("visible"));
            String keyString = optionalString(map.get("keyString"), "true");
            String label = optionalString(map.get("label"), fromKey + " -> " + toKey);
            String description = description(map.get("description"));
            UUID fromId = requireOwner(fromKey, idsByKey);
            UUID toId = requireOwner(toKey, idsByKey);
            gates.add(new WorldRecipe.GateSpec(fromId, dir, toId, visible, keyString, label, description));
        }
        return gates;
    }

    private static List<WorldRecipe.FixtureSpec> parseFixtures(Object raw, Map<String, UUID> idsByKey) {
        List<WorldRecipe.FixtureSpec> fixtures = new ArrayList<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String key = requireString(map.get("key"), "fixtures.key");
            UUID id = uuid("fixture", key);
            String name = requireString(map.get("name"), "fixtures.name");
            String description = description(map.get("description"));
            String ownerKey = requireString(map.get("ownerKey"), "fixtures.ownerKey");
            UUID ownerId = requireOwner(ownerKey, idsByKey);
            boolean visible = bool(map.get("visible"));
            Map<String, CellSpec> cells = parseCells(map.get("cells"), "fixtures.cells");
            fixtures.add(new WorldRecipe.FixtureSpec(id, name, description, ownerId, visible, cells));
        }
        return fixtures;
    }

    private static List<GameSave.ItemRecipe> parseItems(Object raw, Map<String, UUID> idsByKey) {
        // Items can own other items (e.g., backpack holding gear), so we first pre-register all item keys
        // to populate idsByKey before resolving ownership.
        List<GameSave.ItemRecipe> items = new ArrayList<>();
        List<Map<?, ?>> entries = new ArrayList<>();
        for (Object entry : list(raw)) {
            if (entry instanceof Map<?, ?> map) {
                entries.add(map);
                Object keyObj = map.get("key");
                if (keyObj != null) {
                    idsByKey.put(keyObj.toString(), uuid("item", keyObj.toString()));
                }
            }
        }

        for (Map<?, ?> map : entries) {
            String key = requireString(map.get("key"), "items.key");
            UUID id = idsByKey.get(key);
            if (id == null) {
                id = uuid("item", key);
                idsByKey.put(key, id);
            }
            String name = requireString(map.get("name"), "items.name");
            String description = description(map.get("description"));
            String ownerKey = requireString(map.get("ownerKey"), "items.ownerKey");
            UUID ownerId = requireOwner(ownerKey, idsByKey);
            boolean visible = bool(map.get("visible"));
            boolean fixture = bool(map.get("fixture"));
            String keyString = optionalString(map.get("keyString"), "true");
            double footprintWidth = optionalDouble(map.get("footprintWidth"), 0.1);
            double footprintHeight = optionalDouble(map.get("footprintHeight"), 0.1);
            double capacityWidth = optionalDouble(map.get("capacityWidth"), 0.0);
            double capacityHeight = optionalDouble(map.get("capacityHeight"), 0.0);
            long weaponDamage = optionalLong(map.get("weaponDamage"), 0L);
            long armorMitigation = optionalLong(map.get("armorMitigation"), 0L);
            Map<String, CellSpec> cells = parseCells(map.get("cells"), "items.cells");
            items.add(new ItemRecipeBuilder()
                    .withId(id)
                    .withName(name)
                    .withDescription(description)
                    .withOwnerId(ownerId)
                    .withVisible(visible)
                    .withFixture(fixture)
                    .withKeyString(keyString)
                    .withFootprint(footprintWidth, footprintHeight)
                    .withCapacity(capacityWidth, capacityHeight)
                    .withWeaponDamage(weaponDamage)
                    .withArmorMitigation(armorMitigation)
                    .withCells(cells)
                    .build());
        }
        return items;
    }

    private static List<GameSave.ActorRecipe> parseActors(Object raw, Map<String, UUID> idsByKey) {
        List<GameSave.ActorRecipe> actors = new ArrayList<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            String key = requireString(map.get("key"), "actors.key");
            UUID id = uuid("actor", key);
            String name = requireString(map.get("name"), "actors.name");
            String description = description(map.get("description"));
            String ownerKey = requireString(map.get("ownerKey"), "actors.ownerKey");
            UUID ownerId = requireOwner(ownerKey, idsByKey);
            boolean visible = bool(map.get("visible"));
            List<String> skills = stringList(map.get("skills"));
            UUID mainHandId = requireOptionalOwner(optionalString(map.get("equippedMainHandItemId")), idsByKey, "actors.equippedMainHandItemId");
            UUID bodyId = requireOptionalOwner(optionalString(map.get("equippedBodyItemId")), idsByKey, "actors.equippedBodyItemId");
            Map<String, CellSpec> cells = parseCells(map.get("cells"), "actors.cells");
            actors.add(new ActorRecipeBuilder()
                    .withId(id)
                    .withName(name)
                    .withDescription(description)
                    .withOwnerId(ownerId)
                    .withVisible(visible)
                    .withSkills(skills)
                    .withEquippedMainHandItemId(mainHandId)
                    .withEquippedBodyItemId(bodyId)
                    .withCells(cells)
                    .build());
        }
        return actors;
    }

    private static Map<String, CellSpec> parseCells(Object raw, String field) {
        if (raw == null) {
            return Map.of();
        }
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, CellSpec> result = new HashMap<>();
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
            result.put(normalizeCellKey(name), new CellSpec(capacity, amount));
        }
        return Map.copyOf(result);
    }

    private static void applyDescriptionOverrides(
            Map<String, Object> overrides,
            List<WorldRecipe.PlotSpec> plots,
            List<WorldRecipe.FixtureSpec> fixtures,
            List<GameSave.ItemRecipe> items,
            List<GameSave.ActorRecipe> actors
    ) {
        if (overrides == null || overrides.isEmpty()) {
            return;
        }
        Map<String, String> plotDesc = toDescriptionMap(overrides.get("plots"));
        Map<String, String> fixtureDesc = toDescriptionMap(overrides.get("fixtures"));
        Map<String, String> itemDesc = toDescriptionMap(overrides.get("items"));
        Map<String, String> actorDesc = toDescriptionMap(overrides.get("actors"));

        plots.replaceAll(p -> {
            String key = keyFromName(p.name());
            String desc = plotDesc.get(key);
            return desc == null ? p : new WorldRecipe.PlotSpec(p.plotId(), p.name(), p.region(), p.locationX(), p.locationY(), desc);
        });

        fixtures.replaceAll(f -> {
            String key = keyFromName(f.name());
            String desc = fixtureDesc.get(key);
            return desc == null ? f : new WorldRecipe.FixtureSpec(f.id(), f.name(), desc, f.ownerId(), f.visible(), f.cells());
        });

        items.replaceAll(i -> {
            String key = keyFromName(i.name());
            String desc = itemDesc.get(key);
            return desc == null ? i : new ItemRecipeBuilder()
                    .withId(i.id())
                    .withName(i.name())
                    .withDescription(desc)
                    .withOwnerId(i.ownerId())
                    .withVisible(i.visible())
                    .withFixture(i.fixture())
                    .withKeyString(i.keyString())
                    .withFootprint(i.footprintWidth(), i.footprintHeight())
                    .withCapacity(i.capacityWidth(), i.capacityHeight())
                    .withWeaponDamage(i.weaponDamage())
                    .withArmorMitigation(i.armorMitigation())
                    .withCells(i.cells())
                    .build();
        });

        actors.replaceAll(a -> {
            String key = keyFromName(a.name());
            String desc = actorDesc.get(key);
            return desc == null ? a : new ActorRecipeBuilder()
                    .withId(a.id())
                    .withName(a.name())
                    .withDescription(desc)
                    .withOwnerId(a.ownerId())
                    .withVisible(a.visible())
                    .withSkills(a.skills())
                    .withEquippedMainHandItemId(a.equippedMainHandItemId())
                    .withEquippedBodyItemId(a.equippedBodyItemId())
                    .withCells(a.cells())
                    .build();
        });
    }

    private static Map<String, String> toDescriptionMap(Object raw) {
        Map<String, String> map = new HashMap<>();
        for (Object entry : list(raw)) {
            if (!(entry instanceof Map<?, ?> m)) {
                continue;
            }
            Object keyObj = m.get("key");
            Object descObj = m.get("description");
            if (keyObj != null && descObj != null) {
                map.put(keyObj.toString(), description(descObj));
            }
        }
        return map;
    }

    private static UUID requireOwner(String key, Map<String, UUID> idsByKey) {
        UUID id = idsByKey.get(key);
        if (id == null) {
            throw new IllegalArgumentException("Owner not found for key: " + key);
        }
        return id;
    }

    private static UUID requireOptionalOwner(String key, Map<String, UUID> idsByKey, String field) {
        if (key == null || key.isBlank()) {
            return null;
        }
        UUID id = idsByKey.get(key);
        if (id == null) {
            throw new IllegalArgumentException("Owner not found for " + field + ": " + key);
        }
        return id;
    }

    private static String description(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String s) {
            return s;
        }
        if (value instanceof List<?> list) {
            String last = "";
            for (Object entry : list) {
                if (entry instanceof Map<?, ?> raw) {
                    Object text = raw.get("text");
                    if (text != null) {
                        last = text.toString();
                    }
                } else if (entry != null) {
                    last = entry.toString();
                }
            }
            return last;
        }
        return value.toString();
    }

    private static List<String> stringList(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            List<String> out = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    out.add(entry.toString());
                }
            }
            return out;
        }
        if (raw instanceof String s) {
            return List.of(s);
        }
        return List.of();
    }

    private static List<?> list(Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list;
        }
        throw new IllegalArgumentException("Expected list but got: " + raw.getClass());
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

    private static int optionalInt(Object value) {
        if (value == null) {
            return 0;
        }
        return (int) parseLong(value, "location");
    }

    private static long optionalLong(Object value, long defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String optionalString(Object value) {
        return value == null ? "" : value.toString();
    }

    private static String optionalString(Object value, String defaultValue) {
        return value == null ? defaultValue : value.toString();
    }

    private static double optionalDouble(Object value, double defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static String requireString(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required.");
        }
        return value.toString();
    }

    private static long parseLong(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Field '" + field + "' is required.");
        }
        if (value instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Field '" + field + "' is not a number: " + value, ex);
        }
    }

    private static UUID uuid(String kind, String key) {
        return UUID.nameUUIDFromBytes((kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private record ParsedPlots(List<WorldRecipe.PlotSpec> plots, Map<String, UUID> idsByKey) {
    }

    private static String keyFromName(String name) {
        if (name == null) {
            return "";
        }
        String lower = name.trim().toLowerCase(Locale.ROOT);
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

    private static String normalizeCellKey(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }
}

package com.demo.adventure.authoring.lang.gdl;

import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.save.ActorRecipeBuilder;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.ItemRecipeBuilder;
import com.demo.adventure.domain.save.WorldRecipe;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class GdlProgramCompiler {
    private static final int AUTO_GRID_WIDTH = 6;

    GameSave compile(GdlProgram program, String source) throws GdlCompileException {
        Map<String, ThingDefinition> things = new LinkedHashMap<>();
        Map<String, ActorDefinition> actors = new LinkedHashMap<>();

        for (GdlDeclaration declaration : program.declarations()) {
            if (declaration.type() == GdlDeclarationType.THING) {
                ThingDefinition def = things.computeIfAbsent(
                        normalizeKey(declaration.subjectId(), declaration.line(), declaration.column(), source),
                        key -> new ThingDefinition(key, declaration.line(), declaration.column())
                );
                mergeFixtureAttributes(def.fixtures, declaration.fixtureId(), declaration.attributes(), source);
            } else if (declaration.type() == GdlDeclarationType.ACTOR) {
                ActorDefinition def = actors.computeIfAbsent(
                        normalizeKey(declaration.subjectId(), declaration.line(), declaration.column(), source),
                        key -> new ActorDefinition(key, declaration.line(), declaration.column())
                );
                mergeFixtureAttributes(def.fixtures, declaration.fixtureId(), declaration.attributes(), source);
            }
        }

        List<PlotInput> plots = new ArrayList<>();
        List<GateInput> gates = new ArrayList<>();
        List<FixtureInput> fixtures = new ArrayList<>();
        Map<String, MutableItemInput> items = new LinkedHashMap<>();
        List<ActorInput> actorInputs = new ArrayList<>();
        Map<String, ContainsInput> contains = new LinkedHashMap<>();

        String startPlotKey = null;
        Long seed = null;
        String preamble = "";

        for (ThingDefinition def : things.values()) {
            Map<String, GdlAttribute> selfAttrs = collectSelfAttributes(def, source);
            String kind = stringValue(selfAttrs, Set.of("kind", "type"), "plot", source);
            String normalizedKind = kind == null ? "plot" : kind.trim().toLowerCase(Locale.ROOT);

            if ("game".equals(normalizedKind)) {
                Long fallbackSeed = seed == null ? Long.valueOf(0L) : seed;
                seed = numberValue(selfAttrs, Set.of("seed"), fallbackSeed, source);
                String maybePreamble = stringValue(selfAttrs, Set.of("preamble"), null, source);
                if (maybePreamble != null) {
                    preamble = maybePreamble;
                }
                String explicitStart = stringValue(selfAttrs, Set.of("startplot", "startplotkey"), null, source);
                if (explicitStart != null && !explicitStart.isBlank()) {
                    startPlotKey = normalizeKey(explicitStart, def.line, def.column, source);
                }
                continue;
            }

            if ("item".equals(normalizedKind)) {
                MutableItemInput item = buildItem(def, selfAttrs, source);
                items.put(item.key(), item);
                continue;
            }

            if ("fixture".equals(normalizedKind)) {
                FixtureInput fixture = buildFixture(def.key, def.line, def.column, selfAttrs, source);
                fixtures.add(fixture);
                continue;
            }

            if (!"plot".equals(normalizedKind)) {
                throw error("Unknown thing kind: " + normalizedKind, def.line, def.column, source, def.key);
            }

            PlotInput plot = buildPlot(def, selfAttrs, source);
            plots.add(plot);
            if (plot.start()) {
                if (startPlotKey != null) {
                    throw error("Multiple start plots defined", def.line, def.column, source, def.key);
                }
                startPlotKey = plot.key();
            }

            List<FixtureDecl> fixtureDecls = collectNonSelfFixtures(def);
            for (FixtureDecl fixtureDecl : fixtureDecls) {
                if (fixtureDecl.attributes().containsKey("leadsto") || fixtureDecl.attributes().containsKey("to")) {
                    gates.add(buildGate(plot.key(), fixtureDecl, source));
                    continue;
                }
                FixtureInput fixture = buildFixtureForPlot(plot.key(), fixtureDecl, source);
                fixtures.add(fixture);
                maybeAddContains(contains, fixture.key(), fixtureDecl.attributes(), fixtureDecl.line(), fixtureDecl.column(), source);
            }

            maybeAddContains(contains, plot.key(), selfAttrs, def.line, def.column, source);
        }

        for (ActorDefinition def : actors.values()) {
            Map<String, GdlAttribute> selfAttrs = collectSelfAttributes(def, source);
            ActorInput actor = buildActor(def, selfAttrs, source);
            actorInputs.add(actor);
            if (actor.isPlayer()) {
                if (startPlotKey != null) {
                    throw error("Multiple start plots defined", def.line, def.column, source, def.key);
                }
                startPlotKey = actor.ownerKey();
            }

            for (FixtureDecl fixtureDecl : collectNonSelfFixtures(def)) {
                FixtureInput fixture = buildFixtureForOwner(def.key, fixtureDecl, source);
                fixtures.add(fixture);
                maybeAddContains(contains, fixture.key(), fixtureDecl.attributes(), fixtureDecl.line(), fixtureDecl.column(), source);
            }
        }

        if (seed == null) {
            seed = 0L;
        }

        if (plots.isEmpty()) {
            throw error("At least one plot is required", 1, 1, source, "");
        }

        if (startPlotKey == null) {
            if (plots.size() == 1) {
                startPlotKey = plots.get(0).key();
            } else {
                throw error("start plot not defined (use start=true on a plot or player=true on an actor)", 1, 1, source, "");
            }
        }

        Map<String, UUID> plotIds = new HashMap<>();
        for (PlotInput plot : plots) {
            if (plotIds.put(plot.key(), uuid("plot", plot.key())) != null) {
                throw error("Duplicate plot key: " + plot.key(), plot.line(), plot.column(), source, plot.key());
            }
        }

        Map<String, UUID> thingIds = new HashMap<>();
        for (FixtureInput fixture : fixtures) {
            if (thingIds.put(fixture.key(), uuid("fixture", fixture.key())) != null) {
                throw error("Duplicate fixture key: " + fixture.key(), fixture.line(), fixture.column(), source, fixture.key());
            }
        }
        for (MutableItemInput item : items.values()) {
            if (thingIds.put(item.key(), uuid("item", item.key())) != null) {
                throw error("Duplicate item key: " + item.key(), item.line(), item.column(), source, item.key());
            }
        }
        for (ActorInput actor : actorInputs) {
            if (thingIds.put(actor.key(), uuid("actor", actor.key())) != null) {
                throw error("Duplicate actor key: " + actor.key(), actor.line(), actor.column(), source, actor.key());
            }
        }

        applyContains(contains, items, plotIds, thingIds, source);

        Map<String, Coord> autoCoords = deriveCoordinates(plots);
        List<WorldRecipe.PlotSpec> plotSpecs = new ArrayList<>();
        for (PlotInput plot : plots) {
            Coord coord = autoCoords.get(plot.key());
            int x = plot.locationX() != null ? plot.locationX() : coord.x();
            int y = plot.locationY() != null ? plot.locationY() : coord.y();
            plotSpecs.add(new WorldRecipe.PlotSpec(
                    plotIds.get(plot.key()),
                    plot.name(),
                    plot.region(),
                    x,
                    y,
                    plot.description()
            ));
        }

        UUID startPlotId = plotIds.get(startPlotKey);
        if (startPlotId == null) {
            throw error("start plot not found: " + startPlotKey, 1, 1, source, startPlotKey);
        }

        List<WorldRecipe.GateSpec> gateSpecs = new ArrayList<>();
        for (GateInput gate : gates) {
            UUID fromId = plotIds.get(gate.fromKey());
            UUID toId = plotIds.get(gate.toKey());
            if (fromId == null || toId == null) {
                throw error("Gate references unknown plot", gate.line(), gate.column(), source, gate.fromKey() + " -> " + gate.toKey());
            }
            Direction direction;
            try {
                direction = Direction.parse(gate.direction());
            } catch (IllegalArgumentException ex) {
                throw error(ex.getMessage(), gate.line(), gate.column(), source, gate.direction());
            }
            gateSpecs.add(new WorldRecipe.GateSpec(
                    fromId,
                    direction,
                    toId,
                    gate.visible(),
                    gate.keyString(),
                    gate.label(),
                    gate.description()
            ));
        }

        List<WorldRecipe.FixtureSpec> fixtureSpecs = new ArrayList<>();
        for (FixtureInput fixture : fixtures) {
            UUID ownerId = resolveOwner(fixture.ownerKey(), plotIds, thingIds, fixture.line(), fixture.column(), source);
            fixtureSpecs.add(new WorldRecipe.FixtureSpec(
                    uuid("fixture", fixture.key()),
                    fixture.name(),
                    fixture.description(),
                    ownerId,
                    fixture.visible(),
                    java.util.Map.of()
            ));
        }

        List<GameSave.ItemRecipe> itemSpecs = new ArrayList<>();
        for (MutableItemInput item : items.values()) {
            String ownerKey = item.ownerKey();
            if (ownerKey == null) {
                throw error("Item owner is required: " + item.key(), item.line(), item.column(), source, item.key());
            }
            UUID ownerId = resolveOwner(ownerKey, plotIds, thingIds, item.line(), item.column(), source);
            itemSpecs.add(new ItemRecipeBuilder()
                    .withId(uuid("item", item.key()))
                    .withName(item.name())
                    .withDescription(item.description())
                    .withOwnerId(ownerId)
                    .withVisible(item.visible())
                    .withFixture(item.fixture())
                    .withKeyString(item.keyString())
                    .withFootprint(item.footprintWidth(), item.footprintHeight())
                    .withCapacity(item.capacityWidth(), item.capacityHeight())
                    .withWeaponDamage(0L)
                    .withArmorMitigation(0L)
                    .withCells(java.util.Map.of())
                    .build());
        }

        List<GameSave.ActorRecipe> actorSpecs = new ArrayList<>();
        for (ActorInput actor : actorInputs) {
            UUID ownerId = resolveOwner(actor.ownerKey(), plotIds, thingIds, actor.line(), actor.column(), source);
            actorSpecs.add(new ActorRecipeBuilder()
                    .withId(uuid("actor", actor.key()))
                    .withName(actor.name())
                    .withDescription(actor.description())
                    .withOwnerId(ownerId)
                    .withVisible(actor.visible())
                    .withSkills(actor.skills())
                    .withEquippedMainHandItemId(null)
                    .withEquippedBodyItemId(null)
                    .withCells(java.util.Map.of())
                    .build());
        }

        String resolvedPreamble = preamble == null ? "" : preamble;
        if (resolvedPreamble.isBlank()) {
            resolvedPreamble = actorPreamble(startPlotId, actorSpecs);
        }

        return new GameSave(seed, startPlotId, resolvedPreamble, plotSpecs, gateSpecs, fixtureSpecs, itemSpecs, actorSpecs);
    }

    private static void mergeFixtureAttributes(
            Map<String, Map<String, GdlAttribute>> fixtures,
            String fixtureId,
            Map<String, GdlAttribute> attributes,
            String source
    ) throws GdlCompileException {
        String fixtureKey = normalizeKey(fixtureId, attributes.values().stream()
                .findFirst()
                .map(GdlAttribute::line)
                .orElse(1), attributes.values().stream()
                .findFirst()
                .map(GdlAttribute::column)
                .orElse(1), source);
        Map<String, GdlAttribute> existing = fixtures.computeIfAbsent(fixtureKey, key -> new LinkedHashMap<>());
        for (Map.Entry<String, GdlAttribute> entry : attributes.entrySet()) {
            String name = entry.getKey();
            GdlAttribute incoming = entry.getValue();
            if ("contains".equalsIgnoreCase(name) && existing.containsKey(name)) {
                GdlValue merged = mergeContains(existing.get(name), incoming, source);
                existing.put(name, new GdlAttribute(merged, incoming.line(), incoming.column()));
                continue;
            }
            if (existing.containsKey(name)) {
                throw error("Duplicate attribute '" + name + "'", incoming.line(), incoming.column(), source, name);
            }
            existing.put(name, incoming);
        }
    }

    private static GdlValue mergeContains(GdlAttribute left, GdlAttribute right, String source) throws GdlCompileException {
        if (!(left.value() instanceof GdlValue.GdlList leftList)
                || !(right.value() instanceof GdlValue.GdlList rightList)) {
            throw error("contains must be a list", right.line(), right.column(), source, "contains");
        }
        List<GdlValue> merged = new ArrayList<>();
        merged.addAll(leftList.values());
        merged.addAll(rightList.values());
        return new GdlValue.GdlList(List.copyOf(merged));
    }

    private static PlotInput buildPlot(ThingDefinition def, Map<String, GdlAttribute> attrs, String source)
            throws GdlCompileException {
        String name = requiredString(attrs, Set.of("name"), def, source);
        String region = requiredString(attrs, Set.of("region"), def, source);
        String description = stringValue(attrs, Set.of("description"), "", source);
        Integer locationX = intValueOptional(attrs, Set.of("locationx"), source);
        Integer locationY = intValueOptional(attrs, Set.of("locationy"), source);
        if ((locationX == null) != (locationY == null)) {
            throw error("locationX and locationY must both be set or both omitted", def.line, def.column, source, def.key);
        }
        boolean start = booleanValue(attrs, Set.of("start", "startplot"), false, source);
        return new PlotInput(def.key, name, region, description, locationX, locationY, start, def.line, def.column);
    }

    private static FixtureInput buildFixture(
            String key,
            int line,
            int column,
            Map<String, GdlAttribute> attrs,
            String source
    ) throws GdlCompileException {
        String name = requiredString(attrs, Set.of("name"), line, column, source, key);
        String description = stringValue(attrs, Set.of("description"), "", source);
        String ownerKey = requiredString(attrs, Set.of("owner", "ownerkey"), line, column, source, key);
        boolean visible = booleanValue(attrs, Set.of("visible"), true, source);
        return new FixtureInput(key, name, description, ownerKey, visible, line, column);
    }

    private static FixtureInput buildFixtureForPlot(String plotKey, FixtureDecl decl, String source) throws GdlCompileException {
        Map<String, GdlAttribute> attrs = decl.attributes();
        String key = stringValue(attrs, Set.of("key", "id", "fixturekey"), null, source);
        if (key == null || key.isBlank()) {
            key = plotKey + "-" + decl.fixtureId();
        }
        String normalizedKey = normalizeKey(key, decl.line(), decl.column(), source);
        String name = requiredString(attrs, Set.of("name"), decl.line(), decl.column(), source, normalizedKey);
        String description = stringValue(attrs, Set.of("description"), "", source);
        String ownerKey = stringValue(attrs, Set.of("owner", "ownerkey"), plotKey, source);
        boolean visible = booleanValue(attrs, Set.of("visible"), true, source);
        return new FixtureInput(normalizedKey, name, description, ownerKey, visible, decl.line(), decl.column());
    }

    private static FixtureInput buildFixtureForOwner(String ownerKey, FixtureDecl decl, String source) throws GdlCompileException {
        Map<String, GdlAttribute> attrs = decl.attributes();
        String key = stringValue(attrs, Set.of("key", "id", "fixturekey"), null, source);
        if (key == null || key.isBlank()) {
            key = ownerKey + "-" + decl.fixtureId();
        }
        String normalizedKey = normalizeKey(key, decl.line(), decl.column(), source);
        String name = requiredString(attrs, Set.of("name"), decl.line(), decl.column(), source, normalizedKey);
        String description = stringValue(attrs, Set.of("description"), "", source);
        String resolvedOwner = stringValue(attrs, Set.of("owner", "ownerkey"), ownerKey, source);
        boolean visible = booleanValue(attrs, Set.of("visible"), true, source);
        return new FixtureInput(normalizedKey, name, description, resolvedOwner, visible, decl.line(), decl.column());
    }

    private static GateInput buildGate(String fromKey, FixtureDecl decl, String source) throws GdlCompileException {
        Map<String, GdlAttribute> attrs = decl.attributes();
        String toKey = stringValue(attrs, Set.of("leadsto", "to"), null, source);
        if (toKey == null || toKey.isBlank()) {
            throw error("leadsTo is required for gates", decl.line(), decl.column(), source, decl.fixtureId());
        }
        String direction = stringValue(attrs, Set.of("direction"), decl.fixtureId(), source);
        boolean visible = booleanValue(attrs, Set.of("visible"), true, source);
        String keyString = stringValue(attrs, Set.of("keystring"), "true", source);
        String label = stringValue(attrs, Set.of("label"), fromKey + " -> " + normalizeKey(toKey, decl.line(), decl.column(), source), source);
        String description = stringValue(attrs, Set.of("description"), "", source);
        return new GateInput(
                fromKey,
                normalizeKey(toKey, decl.line(), decl.column(), source),
                direction,
                visible,
                keyString,
                label,
                description,
                decl.line(),
                decl.column()
        );
    }

    private static MutableItemInput buildItem(ThingDefinition def, Map<String, GdlAttribute> attrs, String source)
            throws GdlCompileException {
        String name = requiredString(attrs, Set.of("name"), def, source);
        String description = stringValue(attrs, Set.of("description"), "", source);
        String ownerKey = stringValue(attrs, Set.of("owner", "ownerkey"), null, source);
        boolean visible = booleanValue(attrs, Set.of("visible"), true, source);
        boolean fixture = booleanValue(attrs, Set.of("fixture"), false, source);
        String keyString = stringValue(attrs, Set.of("keystring"), "true", source);
        double footprintWidth = numberValue(attrs, Set.of("footprintwidth"), 0.1, source);
        double footprintHeight = numberValue(attrs, Set.of("footprintheight"), 0.1, source);
        double capacityWidth = numberValue(attrs, Set.of("capacitywidth"), 0.0, source);
        double capacityHeight = numberValue(attrs, Set.of("capacityheight"), 0.0, source);
        return new MutableItemInput(
                def.key,
                name,
                description,
                ownerKey,
                visible,
                fixture,
                keyString,
                footprintWidth,
                footprintHeight,
                capacityWidth,
                capacityHeight,
                def.line,
                def.column
        );
    }

    private static ActorInput buildActor(ActorDefinition def, Map<String, GdlAttribute> attrs, String source)
            throws GdlCompileException {
        String name = requiredString(attrs, Set.of("name"), def, source);
        String description = stringValue(attrs, Set.of("description"), "", source);
        String ownerKey = requiredString(attrs, Set.of("owner", "ownerkey"), def, source);
        boolean visible = booleanValue(attrs, Set.of("visible"), true, source);
        boolean player = booleanValue(attrs, Set.of("player", "isplayer"), false, source);
        List<String> skills = listValue(attrs, Set.of("skills"), source);
        return new ActorInput(def.key, name, description, ownerKey, visible, skills, player, def.line, def.column);
    }

    private static void applyContains(
            Map<String, ContainsInput> contains,
            Map<String, MutableItemInput> items,
            Map<String, UUID> plotIds,
            Map<String, UUID> thingIds,
            String source
    ) throws GdlCompileException {
        for (Map.Entry<String, ContainsInput> entry : contains.entrySet()) {
            String ownerKey = entry.getKey();
            ContainsInput input = entry.getValue();
            if (!plotIds.containsKey(ownerKey) && !thingIds.containsKey(ownerKey)) {
                throw error("contains owner not found: " + ownerKey, input.line(), input.column(), source, ownerKey);
            }
            for (String rawItemKey : input.items()) {
                String itemKey = normalizeKey(rawItemKey, input.line(), input.column(), source);
                MutableItemInput item = items.get(itemKey);
                if (item == null) {
                    throw error("contains references unknown item: " + itemKey, input.line(), input.column(), source, itemKey);
                }
                if (item.ownerKey() != null && !item.ownerKey().equals(ownerKey)) {
                    throw error("Item owner already set: " + itemKey, item.line(), item.column(), source, itemKey);
                }
                item.setOwnerKey(ownerKey);
            }
        }
    }

    private static void maybeAddContains(
            Map<String, ContainsInput> contains,
            String ownerKey,
            Map<String, GdlAttribute> attrs,
            int line,
            int column,
            String source
    ) throws GdlCompileException {
        GdlAttribute attr = findAttribute(attrs, Set.of("contains"));
        if (attr == null) {
            return;
        }
        List<String> items = listValue(attr, source, "contains");
        if (items.isEmpty()) {
            return;
        }
        contains.put(ownerKey, new ContainsInput(items, attr.line(), attr.column()));
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

    private static UUID resolveOwner(
            String ownerKey,
            Map<String, UUID> plotIds,
            Map<String, UUID> thingIds,
            int line,
            int column,
            String source
    ) throws GdlCompileException {
        String normalized = normalizeKey(ownerKey, line, column, source);
        UUID found = thingIds.get(normalized);
        if (found != null) {
            return found;
        }
        found = plotIds.get(normalized);
        if (found != null) {
            return found;
        }
        throw error("Owner not found: " + normalized, line, column, source, ownerKey);
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

    private static String requiredString(
            Map<String, GdlAttribute> attrs,
            Set<String> keys,
            DefinitionBase def,
            String source
    ) throws GdlCompileException {
        return requiredString(attrs, keys, def.line, def.column, source, def.key);
    }

    private static String requiredString(
            Map<String, GdlAttribute> attrs,
            Set<String> keys,
            int line,
            int column,
            String source,
            String context
    ) throws GdlCompileException {
        String value = stringValue(attrs, keys, null, source);
        if (value == null || value.isBlank()) {
            throw error("Missing required attribute: " + keys.iterator().next(), line, column, source, context);
        }
        return value;
    }

    private static String stringValue(Map<String, GdlAttribute> attrs, Set<String> keys, String fallback, String source)
            throws GdlCompileException {
        GdlAttribute attr = findAttribute(attrs, keys);
        if (attr == null) {
            return fallback;
        }
        if (attr.value() instanceof GdlValue.GdlString str) {
            return str.value();
        }
        throw error("Expected string value", attr.line(), attr.column(), source, keys.iterator().next());
    }

    private static boolean booleanValue(
            Map<String, GdlAttribute> attrs,
            Set<String> keys,
            boolean fallback,
            String source
    )
            throws GdlCompileException {
        GdlAttribute attr = findAttribute(attrs, keys);
        if (attr == null) {
            return fallback;
        }
        if (attr.value() instanceof GdlValue.GdlBoolean b) {
            return b.value();
        }
        throw error("Expected boolean value", attr.line(), attr.column(), source, keys.iterator().next());
    }

    private static double numberValue(
            Map<String, GdlAttribute> attrs,
            Set<String> keys,
            double fallback,
            String source
    )
            throws GdlCompileException {
        GdlAttribute attr = findAttribute(attrs, keys);
        if (attr == null) {
            return fallback;
        }
        if (attr.value() instanceof GdlValue.GdlNumber n) {
            return n.value();
        }
        throw error("Expected numeric value", attr.line(), attr.column(), source, keys.iterator().next());
    }

    private static Long numberValue(
            Map<String, GdlAttribute> attrs,
            Set<String> keys,
            Long fallback,
            String source
    )
            throws GdlCompileException {
        GdlAttribute attr = findAttribute(attrs, keys);
        if (attr == null) {
            return fallback;
        }
        if (attr.value() instanceof GdlValue.GdlNumber n) {
            return (long) n.value();
        }
        throw error("Expected numeric value", attr.line(), attr.column(), source, keys.iterator().next());
    }

    private static Integer intValueOptional(
            Map<String, GdlAttribute> attrs,
            Set<String> keys,
            String source
    )
            throws GdlCompileException {
        GdlAttribute attr = findAttribute(attrs, keys);
        if (attr == null) {
            return null;
        }
        if (attr.value() instanceof GdlValue.GdlNumber n) {
            return (int) Math.round(n.value());
        }
        throw error("Expected numeric value", attr.line(), attr.column(), source, keys.iterator().next());
    }

    private static List<String> listValue(
            Map<String, GdlAttribute> attrs,
            Set<String> keys,
            String source
    ) throws GdlCompileException {
        GdlAttribute attr = findAttribute(attrs, keys);
        if (attr == null) {
            return List.of();
        }
        return listValue(attr, source, keys.iterator().next());
    }

    private static List<String> listValue(GdlAttribute attr, String source, String keyName) throws GdlCompileException {
        if (!(attr.value() instanceof GdlValue.GdlList list)) {
            throw error("Expected list value", attr.line(), attr.column(), source, keyName);
        }
        List<String> result = new ArrayList<>();
        for (GdlValue value : list.values()) {
            if (value instanceof GdlValue.GdlString str) {
                result.add(str.value());
            } else {
                throw error("Expected list of strings", attr.line(), attr.column(), source, keyName);
            }
        }
        return result;
    }

    private static GdlAttribute findAttribute(Map<String, GdlAttribute> attrs, Set<String> keys) {
        for (String key : keys) {
            GdlAttribute attr = attrs.get(key);
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }

    private static Map<String, GdlAttribute> collectSelfAttributes(DefinitionBase def, String source)
            throws GdlCompileException {
        Map<String, GdlAttribute> merged = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, GdlAttribute>> entry : def.fixtures.entrySet()) {
            if (!isSelfFixture(entry.getKey(), def.key)) {
                continue;
            }
            for (Map.Entry<String, GdlAttribute> attr : entry.getValue().entrySet()) {
                if (merged.containsKey(attr.getKey())) {
                    GdlAttribute existing = merged.get(attr.getKey());
                    throw error("Duplicate attribute '" + attr.getKey() + "'", existing.line(), existing.column(), source, attr.getKey());
                }
                merged.put(attr.getKey(), attr.getValue());
            }
        }
        return merged;
    }

    private static List<FixtureDecl> collectNonSelfFixtures(DefinitionBase def) {
        List<FixtureDecl> fixtures = new ArrayList<>();
        for (Map.Entry<String, Map<String, GdlAttribute>> entry : def.fixtures.entrySet()) {
            if (isSelfFixture(entry.getKey(), def.key)) {
                continue;
            }
            fixtures.add(new FixtureDecl(entry.getKey(), entry.getValue(), def.line, def.column));
        }
        return fixtures;
    }

    private static boolean isSelfFixture(String fixtureId, String subjectId) {
        String normalized = fixtureId.toLowerCase(Locale.ROOT);
        String subject = subjectId.toLowerCase(Locale.ROOT);
        return normalized.equals("self") || normalized.equals(subject);
    }

    private static String normalizeKey(String raw, int line, int column, String source) throws GdlCompileException {
        if (raw == null) {
            throw error("Key is required", line, column, source, "");
        }
        String trimmed = raw.trim().toLowerCase(Locale.ROOT);
        if (trimmed.isBlank()) {
            throw error("Key is required", line, column, source, raw);
        }
        StringBuilder sb = new StringBuilder();
        boolean dash = false;
        for (char c : trimmed.toCharArray()) {
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
            throw error("Key is required", line, column, source, raw);
        }
        return normalized;
    }

    private static UUID uuid(String kind, String key) {
        return UUID.nameUUIDFromBytes((kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static GdlCompileException error(String message, int line, int column, String source, String token) {
        return new GdlCompileException(message, line, column, token, source);
    }

    private static class DefinitionBase {
        protected final String key;
        protected final int line;
        protected final int column;
        protected final Map<String, Map<String, GdlAttribute>> fixtures = new LinkedHashMap<>();

        private DefinitionBase(String key, int line, int column) {
            this.key = key;
            this.line = line;
            this.column = column;
        }
    }

    private static final class ThingDefinition extends DefinitionBase {
        private ThingDefinition(String key, int line, int column) {
            super(key, line, column);
        }
    }

    private static final class ActorDefinition extends DefinitionBase {
        private ActorDefinition(String key, int line, int column) {
            super(key, line, column);
        }
    }

    private record FixtureDecl(String fixtureId, Map<String, GdlAttribute> attributes, int line, int column) {
    }

    private record PlotInput(
            String key,
            String name,
            String region,
            String description,
            Integer locationX,
            Integer locationY,
            boolean start,
            int line,
            int column
    ) {
    }

    private record GateInput(
            String fromKey,
            String toKey,
            String direction,
            boolean visible,
            String keyString,
            String label,
            String description,
            int line,
            int column
    ) {
    }

    private record FixtureInput(
            String key,
            String name,
            String description,
            String ownerKey,
            boolean visible,
            int line,
            int column
    ) {
    }

    private record ActorInput(
            String key,
            String name,
            String description,
            String ownerKey,
            boolean visible,
            List<String> skills,
            boolean isPlayer,
            int line,
            int column
    ) {
    }

    private record ContainsInput(List<String> items, int line, int column) {
    }

    private record Coord(int x, int y) {
    }

    private static final class MutableItemInput {
        private final String key;
        private final String name;
        private final String description;
        private String ownerKey;
        private final boolean visible;
        private final boolean fixture;
        private final String keyString;
        private final double footprintWidth;
        private final double footprintHeight;
        private final double capacityWidth;
        private final double capacityHeight;
        private final int line;
        private final int column;

        private MutableItemInput(
                String key,
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
                int line,
                int column
        ) {
            this.key = key;
            this.name = name;
            this.description = description;
            this.ownerKey = ownerKey;
            this.visible = visible;
            this.fixture = fixture;
            this.keyString = keyString;
            this.footprintWidth = footprintWidth;
            this.footprintHeight = footprintHeight;
            this.capacityWidth = capacityWidth;
            this.capacityHeight = capacityHeight;
            this.line = line;
            this.column = column;
        }

        public String key() {
            return key;
        }

        public String name() {
            return name;
        }

        public String description() {
            return description;
        }

        public String ownerKey() {
            return ownerKey;
        }

        public void setOwnerKey(String ownerKey) {
            this.ownerKey = ownerKey;
        }

        public boolean visible() {
            return visible;
        }

        public boolean fixture() {
            return fixture;
        }

        public String keyString() {
            return keyString;
        }

        public double footprintWidth() {
            return footprintWidth;
        }

        public double footprintHeight() {
            return footprintHeight;
        }

        public double capacityWidth() {
            return capacityWidth;
        }

        public double capacityHeight() {
            return capacityHeight;
        }

        public int line() {
            return line;
        }

        public int column() {
            return column;
        }
    }
}

package com.demo.adventure.authoring.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.ItemRecipeBuilder;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.domain.save.WorldRecipe;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.save.build.WorldValidator;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterials;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterialsGenerator;
import com.demo.adventure.domain.save.ActorRecipeBuilder;
import com.demo.adventure.authoring.zone.AnchorRole;
import com.demo.adventure.authoring.zone.AnchorSpec;
import com.demo.adventure.authoring.zone.MappingDifficulty;
import com.demo.adventure.authoring.zone.PacingProfile;
import com.demo.adventure.authoring.zone.TopologyBias;
import com.demo.adventure.authoring.zone.ZoneBuildResult;
import com.demo.adventure.authoring.zone.ZoneGraphBuilder;
import com.demo.adventure.authoring.zone.ZoneSpec;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Deque;
import java.util.UUID;

/**
 * CLI to build a world from simple zone + content lists.
 *
 * Usage:
 *   zonebuilder --in input.yaml --out game.yaml
 */
public final class ZoneBuilderCli extends BuuiConsole {
    private static final String METRICS_FILENAME = "zone-metrics.json";
    private static final String REPORT_FILENAME = "zone-report.txt";

    public static void main(String[] args) throws Exception {
        Path input = null;
        Path output = Path.of("logs/zone-game.yaml");
        Path structuredOut = null;
        String structuredId = null;
        String structuredTitle = null;
        boolean emitMetrics = false;
        boolean failOnProblems = false;
        Long gameSeedOverride = null;
        boolean emitReport = false;
        boolean emitBom = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--in", "-i" -> input = Path.of(args[++i]);
                case "--out", "-o" -> output = Path.of(args[++i]);
                case "--structured-out" -> structuredOut = Path.of(args[++i]);
                case "--id" -> structuredId = args[++i];
                case "--title" -> structuredTitle = args[++i];
                case "--metrics" -> emitMetrics = true;
                case "--report" -> emitReport = true;
                case "--bom" -> emitBom = true;
                case "--strict" -> failOnProblems = true;
                case "--seed" -> gameSeedOverride = Long.parseLong(args[++i]);
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                default -> {
                    System.err.println("Unknown arg: " + args[i]);
                    printHelp();
                    return;
                }
            }
        }
        if (input == null) {
            System.err.println("Input file is required.");
            printHelp();
            return;
        }

        new ZoneBuilderCli().run(input, output, structuredOut, structuredId, structuredTitle, emitMetrics, emitReport, emitBom, failOnProblems, gameSeedOverride);
    }

    private void run(Path input, Path output, Path structuredOut, String structuredId, String structuredTitle, boolean emitMetrics, boolean emitReport, boolean emitBom, boolean failOnProblems, Long gameSeedOverride) throws IOException {
        Map<String, Object> root = loadYaml(input);
        GameInput game = parseGame(root, gameSeedOverride);
        List<ZoneSpec> zones = parseZones(root);

        ZoneGraphBuilder generator = new ZoneGraphBuilder();
        List<WorldRecipe.PlotSpec> plots = new ArrayList<>();
        List<WorldRecipe.GateSpec> gates = new ArrayList<>();
        Map<String, UUID> plotIdsByKey = new HashMap<>();
        Map<AnchorRole, List<UUID>> anchorRoleIds = new HashMap<>();
        UUID startPlotId = null;
        long seed = game.seed;

        // Build each zone and merge.
        for (int i = 0; i < zones.size(); i++) {
            ZoneSpec spec = zones.get(i);
            long zoneSeed = seed + i;
            ZoneBuildResult zone = generator.generate(spec, zoneSeed);
            if (startPlotId == null) {
                startPlotId = zone.recipe().startPlotId();
            }
            plots.addAll(zone.recipe().plots());
            gates.addAll(zone.recipe().gates());
            // Map both raw anchor key and zone-prefixed key to IDs for placements.
            for (Map.Entry<String, UUID> e : zone.anchorPlotIds().entrySet()) {
                plotIdsByKey.put(e.getKey(), e.getValue());
                plotIdsByKey.put(spec.id() + ":" + e.getKey(), e.getValue());
            }
            for (Map.Entry<AnchorRole, List<UUID>> e : zone.anchorRolePlotIds().entrySet()) {
                anchorRoleIds.computeIfAbsent(e.getKey(), k -> new ArrayList<>()).addAll(e.getValue());
            }
        }

        List<GameContent> fixtures = parseFixtures(root, plotIdsByKey, anchorRoleIds, startPlotId);
        List<GameContent> items = parseItems(root, plotIdsByKey, anchorRoleIds, startPlotId);
        List<GameContent> actors = parseActors(root, plotIdsByKey, anchorRoleIds, startPlotId);
        List<PuzzleInput> puzzles = parsePuzzles(root);
        List<BridgeInput> bridges = parseBridges(root);
        List<String> warnings = new ArrayList<>();

        // Apply puzzles to gates.
        gates = applyPuzzles(gates, plotIdsByKey, puzzles, warnings);
        gates.addAll(applyBridges(bridges, zonesById(zones), plotIdsByKey));

        GameSave save = new GameSave(
                seed,
                Objects.requireNonNull(startPlotId, "No start plot from zones"),
                game.preamble,
                plots,
                gates,
                fixtures.stream().map(GameContent::asFixture).toList(),
                items.stream().map(GameContent::asItem).toList(),
                actors.stream().map(GameContent::asActor).toList()
        );

        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        GameSaveYamlWriter.write(save, output);
        print("Wrote game to " + output.toAbsolutePath());

        if (structuredOut != null) {
            if (structuredId == null || structuredId.isBlank()) {
                structuredId = zones.get(0).id();
            }
            if (structuredTitle == null || structuredTitle.isBlank()) {
                structuredTitle = structuredId;
            }
            GameStructExporter.export(save, structuredId, structuredTitle, null, structuredOut);
            print("Wrote structured game to " + structuredOut.toAbsolutePath());
        }

        ValidationReport validation = validate(save, warnings);
        if (emitMetrics && output.getParent() != null) {
            Path metricsPath = output.getParent().resolve(METRICS_FILENAME);
            Files.writeString(metricsPath, validation.asJson(), StandardCharsets.UTF_8);
            print("Metrics written to " + metricsPath.toAbsolutePath());
        }
        if (emitReport && output.getParent() != null) {
            Path reportPath = output.getParent().resolve(REPORT_FILENAME);
            Files.writeString(reportPath, validation.reportText(), StandardCharsets.UTF_8);
            print("Report written to " + reportPath.toAbsolutePath());
        }
        if (emitBom) {
            try {
                WorldBuildResult built = new GameSaveAssembler().apply(save);
                WorldBillOfMaterials bom = WorldBillOfMaterialsGenerator.fromRegistry(built.registry());
                printText(bom.toString());
            } catch (Exception ex) {
                System.err.println("BOM generation failed: " + ex.getMessage());
            }
        }
        if (failOnProblems && (!validation.problems().isEmpty() || !validation.warnings().isEmpty())) {
            throw new IllegalStateException("Validation failed: " + validation.problems().size() + " problem(s), " + validation.warnings().size() + " warning(s)");
        }
    }

    private Map<String, Object> loadYaml(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        Object raw = new Yaml().load(text);
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IOException("Input must be a YAML map.");
        }
        //noinspection unchecked
        return (Map<String, Object>) map;
    }

    private static GameInput parseGame(Map<String, Object> root, Long seedOverride) {
        Map<String, Object> game = map(root.get("game"));
        long seed = seedOverride != null ? seedOverride : number(game.get("seed"), 1000L);
        String preamble = str(game.get("preamble"), "");
        return new GameInput(seed, preamble);
    }

    private static List<ZoneSpec> parseZones(Map<String, Object> root) {
        List<ZoneSpec> zones = new ArrayList<>();
        List<?> list = list(root.get("zones"));
        if (list.isEmpty()) {
            throw new IllegalArgumentException("zones are required");
        }
        for (Object entry : list) {
            Map<String, Object> m = map(entry);
            String id = str(m.get("id"), "zone-" + zones.size());
            String region = str(m.get("region"), id.toUpperCase(Locale.ROOT));
            int target = numberInt(m.get("targetPlotCount"), 12);
            MappingDifficulty diff = enumVal(m.get("difficulty"), MappingDifficulty.MEDIUM, MappingDifficulty.class);
            PacingProfile pacing = enumVal(m.get("pacing"), PacingProfile.BALANCED, PacingProfile.class);
            TopologyBias topo = enumVal(m.get("topology"), TopologyBias.BRANCHY, TopologyBias.class);

            List<AnchorSpec> anchors = new ArrayList<>();
            for (Object a : list(m.get("anchors"))) {
                Map<String, Object> am = map(a);
                String key = str(am.get("key"), "");
                String name = str(am.get("name"), key);
                AnchorRole role = enumVal(am.get("role"), AnchorRole.VISTA, AnchorRole.class);
                String desc = str(am.get("description"), "");
                anchors.add(new AnchorSpec(key, name, role, desc));
            }
            zones.add(new ZoneSpec(id, region, target, diff, pacing, topo, anchors));
        }
        return zones;
    }

    private static List<GameContent> parseFixtures(Map<String, Object> root, Map<String, UUID> plotIds, Map<AnchorRole, List<UUID>> anchorRoleIds, UUID fallbackPlotId) {
        List<GameContent> out = new ArrayList<>();
        for (Object entry : list(root.get("fixtures"))) {
            out.add(parseContent(entry, plotIds, anchorRoleIds, fallbackPlotId, true, false));
        }
        return out;
    }

    private static List<GameContent> parseItems(Map<String, Object> root, Map<String, UUID> plotIds, Map<AnchorRole, List<UUID>> anchorRoleIds, UUID fallbackPlotId) {
        List<GameContent> out = new ArrayList<>();
        for (Object entry : list(root.get("items"))) {
            out.add(parseContent(entry, plotIds, anchorRoleIds, fallbackPlotId, false, false));
        }
        return out;
    }

    private static List<GameContent> parseActors(Map<String, Object> root, Map<String, UUID> plotIds, Map<AnchorRole, List<UUID>> anchorRoleIds, UUID fallbackPlotId) {
        List<GameContent> out = new ArrayList<>();
        for (Object entry : list(root.get("actors"))) {
            out.add(parseContent(entry, plotIds, anchorRoleIds, fallbackPlotId, false, true));
        }
        return out;
    }

    private static List<PuzzleInput> parsePuzzles(Map<String, Object> root) {
        List<PuzzleInput> puzzles = new ArrayList<>();
        for (Object entry : list(root.get("puzzles"))) {
            Map<String, Object> m = map(entry);
            puzzles.add(new PuzzleInput(
                    str(m.get("from"), ""),
                    str(m.get("to"), ""),
                    str(m.get("direction"), ""),
                    str(m.get("labelContains"), ""),
                    str(m.get("keyString"), "true"),
                    str(m.get("description"), "")
            ));
        }
        return puzzles;
    }

    private static List<BridgeInput> parseBridges(Map<String, Object> root) {
        List<BridgeInput> bridges = new ArrayList<>();
        for (Object entry : list(root.get("bridges"))) {
            Map<String, Object> m = map(entry);
            bridges.add(new BridgeInput(
                    str(m.get("fromZone"), ""),
                    str(m.get("fromAnchor"), ""),
                    str(m.get("toZone"), ""),
                    str(m.get("toAnchor"), ""),
                    str(m.get("direction"), ""),
                    str(m.get("keyString"), "true"),
                    str(m.get("description"), "")
            ));
        }
        return bridges;
    }

    private static List<WorldRecipe.GateSpec> applyPuzzles(
            List<WorldRecipe.GateSpec> gates,
            Map<String, UUID> plotIdsByKey,
            List<PuzzleInput> puzzles,
            List<String> warnings
    ) {
        if (puzzles.isEmpty()) {
            return gates;
        }
        List<WorldRecipe.GateSpec> updated = new ArrayList<>(gates);
        for (PuzzleInput puzzle : puzzles) {
            UUID from = plotIdsByKey.get(puzzle.from());
            UUID to = plotIdsByKey.get(puzzle.to());
            Direction direction = puzzle.direction().isBlank() ? null : tryDirection(puzzle.direction());
            String labelContains = puzzle.labelContains().toLowerCase(Locale.ROOT);
            int gateIndex = findGate(updated, from, to, direction, labelContains);
            if (gateIndex < 0 && direction != null) {
                gateIndex = findGate(updated, from, to, null, labelContains);
            }
            if (gateIndex >= 0) {
                WorldRecipe.GateSpec gate = updated.get(gateIndex);
                updated.set(gateIndex, new WorldRecipe.GateSpec(
                        gate.fromPlotId(),
                        gate.direction(),
                        gate.toPlotId(),
                        gate.visible(),
                        puzzle.keyString(),
                        gate.label(),
                        puzzle.description().isBlank() ? gate.description() : puzzle.description()
                ));
            } else {
                warnings.add("Puzzle unmatched: from=" + puzzle.from() + " to=" + puzzle.to() + " dir=" + puzzle.direction() + " labelContains=" + puzzle.labelContains());
            }
        }
        return updated;
    }

    private static List<WorldRecipe.GateSpec> applyBridges(
            List<BridgeInput> bridges,
            Map<String, ZoneSpec> zonesById,
            Map<String, UUID> plotIdsByKey
    ) {
        List<WorldRecipe.GateSpec> out = new ArrayList<>();
        for (BridgeInput bridge : bridges) {
            ZoneSpec fromZone = zonesById.get(bridge.fromZone());
            ZoneSpec toZone = zonesById.get(bridge.toZone());
            if (fromZone == null || toZone == null) {
                continue;
            }
            UUID fromPlot = plotIdsByKey.get(bridge.fromAnchor());
            if (fromPlot == null) {
                fromPlot = plotIdsByKey.get(fromZone.id() + ":" + bridge.fromAnchor());
            }
            UUID toPlot = plotIdsByKey.get(bridge.toAnchor());
            if (toPlot == null) {
                toPlot = plotIdsByKey.get(toZone.id() + ":" + bridge.toAnchor());
            }
            if (fromPlot == null || toPlot == null) {
                continue;
            }
            Direction dir = tryDirection(bridge.direction());
            if (dir == null) {
                dir = Direction.PORTAL;
            }
            String label = fromZone.id() + ":" + bridge.fromAnchor() + " -> " + toZone.id() + ":" + bridge.toAnchor();
            out.add(new WorldRecipe.GateSpec(
                    fromPlot,
                    dir,
                    toPlot,
                    true,
                    bridge.keyString(),
                    label,
                    bridge.description()
            ));
        }
        return out;
    }

    private static int findGate(List<WorldRecipe.GateSpec> gates, UUID from, UUID to, Direction dir, String labelContains) {
        for (int i = 0; i < gates.size(); i++) {
            WorldRecipe.GateSpec g = gates.get(i);
            if (from != null && !g.fromPlotId().equals(from)) {
                continue;
            }
            if (to != null && !g.toPlotId().equals(to)) {
                continue;
            }
            if (dir != null && g.direction() != dir) {
                continue;
            }
            if (!labelContains.isBlank()) {
                String label = g.label() == null ? "" : g.label().toLowerCase(Locale.ROOT);
                if (!label.contains(labelContains)) {
                    continue;
                }
            }
            return i;
        }
        if (from != null) {
            for (int i = 0; i < gates.size(); i++) {
                WorldRecipe.GateSpec g = gates.get(i);
                if (g.fromPlotId().equals(from)) {
                    if (dir == null || g.direction() == dir) {
                        return i;
                    }
                }
            }
        }
        return -1;
    }

    private static Direction tryDirection(String token) {
        try {
            return Direction.parse(token);
        } catch (Exception ex) {
            return null;
        }
    }

    private static GameContent parseContent(
            Object entry,
            Map<String, UUID> plotIds,
            Map<AnchorRole, List<UUID>> anchorRoleIds,
            UUID fallbackPlotId,
            boolean fixture,
            boolean actor
    ) {
        Map<String, Object> m = map(entry);
        String name = str(m.get("name"), "");
        String ownerKey = str(m.get("ownerKey"), "");
        String ownerRoleRaw = str(m.get("ownerRole"), "");
        UUID ownerId = ownerKey.isBlank() ? null : plotIds.get(ownerKey);
        if (ownerId == null && !ownerRoleRaw.isBlank()) {
            AnchorRole role = enumVal(ownerRoleRaw, null, AnchorRole.class);
            if (role != null) {
                List<UUID> ids = anchorRoleIds.getOrDefault(role, List.of());
                if (!ids.isEmpty()) {
                    ownerId = ids.get(0);
                }
            }
        }
        if (ownerId == null) {
            ownerId = fallbackPlotId != null ? fallbackPlotId : plotIds.values().stream().findFirst().orElse(null);
        }
        return new GameContent(
                UUID.nameUUIDFromBytes((name + ownerKey).getBytes(StandardCharsets.UTF_8)),
                name,
                str(m.get("description"), ""),
                ownerId,
                bool(m.get("visible"), true),
                fixture,
                actor,
                str(m.get("keyString"), "true")
        );
    }

    private static void printHelp() {
        printText("""
                zonebuilder --in input.yaml --out game.yaml
                Optional: --structured-out <dir> [--id GAME_ID] [--title "Game Title"] [--metrics] [--strict]
                Input YAML shape:
                  game:
                    seed: 1234
                    preamble: "Welcome..."
                  zones:
                    - id: coast
                      region: COAST
                      targetPlotCount: 14
                      difficulty: MEDIUM
                      pacing: BALANCED
                      topology: BRANCHY
                      anchors:
                        - key: entry
                          name: Wreck Beach
                          role: ENTRY
                          description: "You wake on the shore."
                        - key: exit
                          name: Treehouse
                          role: EXIT
                  fixtures/items/actors: [{name, description, ownerKey, ownerRole, visible, keyString}]
                  puzzles: [{from, to, direction, labelContains, keyString, description}]
                """);
    }

    private record GameInput(long seed, String preamble) {
    }

    private record PuzzleInput(String from, String to, String direction, String labelContains, String keyString, String description) {
    }

    private record BridgeInput(
            String fromZone,
            String fromAnchor,
            String toZone,
            String toAnchor,
            String direction,
            String keyString,
            String description
    ) {
    }

    private record GameContent(
            UUID id,
            String name,
            String description,
            UUID ownerId,
            boolean visible,
            boolean fixture,
            boolean actor,
            String keyString
    ) {
        WorldRecipe.FixtureSpec asFixture() {
            return new WorldRecipe.FixtureSpec(id, name, description, ownerId, visible, java.util.Map.of());
        }

        GameSave.ItemRecipe asItem() {
            return new ItemRecipeBuilder()
                    .withId(id)
                    .withName(name)
                    .withDescription(description)
                    .withOwnerId(ownerId)
                    .withVisible(visible)
                    .withFixture(fixture)
                    .withKeyString(keyString)
                    .withFootprint(0.1, 0.1)
                    .withCapacity(0.0, 0.0)
                    .withWeaponDamage(0L)
                    .withArmorMitigation(0L)
                    .withCells(java.util.Map.of())
                    .build();
        }

        GameSave.ActorRecipe asActor() {
            return new ActorRecipeBuilder()
                    .withId(id)
                    .withName(name)
                    .withDescription(description)
                    .withOwnerId(ownerId)
                    .withVisible(visible)
                    .withCells(java.util.Map.of())
                    .build();
        }
    }

    private record ValidationReport(String json, List<String> problems, List<String> warnings) {
        String asJson() {
            return json == null ? "{}" : json;
        }

        String reportText() {
            if (problems == null || problems.isEmpty()) {
                if (warnings == null || warnings.isEmpty()) {
                    return "No problems.";
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Warnings (").append(warnings.size()).append(")").append('\n');
                warnings.forEach(w -> sb.append("- ").append(w).append('\n'));
                return sb.toString();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("Problems (").append(problems.size()).append(")").append('\n');
            problems.forEach(p -> sb.append("- ").append(p).append('\n'));
            if (warnings != null && !warnings.isEmpty()) {
                sb.append("Warnings (").append(warnings.size()).append(")").append('\n');
                warnings.forEach(w -> sb.append("- ").append(w).append('\n'));
            }
            return sb.toString();
        }
    }

    private static ValidationReport validate(GameSave save, List<String> warnings) {
        // Pattern: Verification
        // - Validates the recipe and built world, producing a report for tooling and CI gates.
        if (save == null) {
            return new ValidationReport("{}", List.of("Save is null"), warnings);
        }
        WorldRecipe recipe = new WorldRecipe(save.seed(), save.startPlotId(), save.plots(), save.gates(), save.fixtures());
        WorldValidator validator = new WorldValidator();
        WorldBuildReport recipeReport = validator.validateRecipe(recipe);
        WorldBuildReport worldReport;
        try {
            GameSaveAssembler assembler = new GameSaveAssembler();
            WorldBuildResult result = assembler.apply(save);
            worldReport = validator.validateWorld(recipe, result.registry());
        } catch (Exception ex) {
            worldReport = new WorldBuildReport();
            worldReport.add(new WorldBuildProblem("E_BUILD_FAILED", ex.getMessage(), "WORLD", null));
        }

        List<String> problems = new ArrayList<>();
        recipeReport.getProblems().forEach(p -> problems.add(p.code() + ":" + p.message()));
        worldReport.getProblems().forEach(p -> problems.add(p.code() + ":" + p.message()));
        List<String> warningList = warnings == null ? List.of() : List.copyOf(warnings);

        int plotCount = save.plots().size();
        int gateCount = save.gates().size();
        int fixtureCount = save.fixtures().size();
        int itemCount = save.items().size();
        int actorCount = save.actors().size();
        Metrics m = metrics(save);

        String json = """
                {
                  "plots": %d,
                  "gates": %d,
                  "fixtures": %d,
                  "items": %d,
                  "actors": %d,
                  "loops": %d,
                  "avgDegree": %.2f,
                  "longestPath": %d,
                  "problems": %d,
                  "warnings": %d
                }
                """.formatted(plotCount, gateCount, fixtureCount, itemCount, actorCount, m.loops, m.avgDegree, m.longestPath, problems.size(), warningList.size()).trim();
        return new ValidationReport(json, problems, warningList);
    }

    private record Metrics(int loops, double avgDegree, int longestPath) {
    }

    private static Metrics metrics(GameSave save) {
        Map<UUID, List<UUID>> adj = new HashMap<>();
        for (WorldRecipe.GateSpec g : save.gates()) {
            adj.computeIfAbsent(g.fromPlotId(), k -> new ArrayList<>()).add(g.toPlotId());
            adj.computeIfAbsent(g.toPlotId(), k -> new ArrayList<>()).add(g.fromPlotId());
        }
        int nodes = save.plots().size();
        int edges = save.gates().size();
        int loops = Math.max(0, edges - nodes + 1);
        double avgDegree = nodes == 0 ? 0d : adj.values().stream().mapToInt(List::size).sum() / (double) nodes;
        int longestPath = longestPath(save.startPlotId(), adj);
        return new Metrics(loops, avgDegree, longestPath);
    }

    private static int longestPath(UUID start, Map<UUID, List<UUID>> adj) {
        if (start == null) {
            return 0;
        }
        Map<UUID, Integer> dist = new HashMap<>();
        Deque<UUID> dq = new ArrayDeque<>();
        dq.add(start);
        dist.put(start, 0);
        int longest = 0;
        while (!dq.isEmpty()) {
            UUID u = dq.removeFirst();
            int d = dist.getOrDefault(u, 0);
            longest = Math.max(longest, d);
            for (UUID v : adj.getOrDefault(u, List.of())) {
                if (!dist.containsKey(v)) {
                    dist.put(v, d + 1);
                    dq.addLast(v);
                }
            }
        }
        return longest;
    }

    // Helpers for parsing
    private static Map<String, Object> map(Object raw) {
        if (raw instanceof Map<?, ?> m) {
            //noinspection unchecked
            return (Map<String, Object>) m;
        }
        return new LinkedHashMap<>();
    }

    private static List<?> list(Object raw) {
        if (raw instanceof List<?> l) {
            return l;
        }
        return List.of();
    }

    private static String str(Object raw, String def) {
        return raw == null ? def : raw.toString();
    }

    private static boolean bool(Object raw, boolean def) {
        if (raw == null) {
            return def;
        }
        if (raw instanceof Boolean b) {
            return b;
        }
        return Boolean.parseBoolean(raw.toString());
    }

    private static long number(Object raw, long def) {
        if (raw == null) {
            return def;
        }
        if (raw instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(raw.toString());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static int numberInt(Object raw, int def) {
        if (raw == null) {
            return def;
        }
        if (raw instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(raw.toString());
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private static <E extends Enum<E>> E enumVal(Object raw, E def, Class<E> type) {
        if (raw == null) {
            return def;
        }
        try {
            return Enum.valueOf(type, raw.toString().trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return def;
        }
    }
    private static Map<String, ZoneSpec> zonesById(List<ZoneSpec> zones) {
        Map<String, ZoneSpec> map = new HashMap<>();
        for (ZoneSpec z : zones) {
            map.put(z.id(), z);
        }
        return map;
    }
}

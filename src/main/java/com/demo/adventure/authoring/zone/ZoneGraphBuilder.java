package com.demo.adventure.authoring.zone;

import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.save.WorldRecipe;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Zone-first graph generator: builds a spine between anchors, adds branches and a loop,
 * sprinkles landmarks, then embeds onto rough coordinates with compass directions.
 */
public final class ZoneGraphBuilder {

    private static final Direction[] DIRECTION_ORDER = new Direction[]{
            Direction.N, Direction.E, Direction.W, Direction.S,
            Direction.NE, Direction.NW, Direction.SE, Direction.SW,
            Direction.UP, Direction.DOWN
    };

    private static final String ZONE_CONFIG_FILE = "zone-config.yaml";

    public ZoneBuildResult generate(ZoneSpec spec, long seed) {
        Objects.requireNonNull(spec, "spec");
        Random random = new Random(seed);
        Map<String, PlotNode> nodesByKey = new HashMap<>();
        Set<String> usedKeys = new HashSet<>();
        List<PlotNode> nodes = new ArrayList<>();
        Map<String, UUID> anchorPlotIds = new HashMap<>();
        Map<AnchorRole, List<UUID>> anchorRolePlotIds = new HashMap<>();
        List<WorldRecipe.GateSpec> gates = new ArrayList<>();
        Map<UUID, Set<Direction>> usedDirections = new HashMap<>();

        List<AnchorSpec> anchorOrder = sortedAnchors(spec.anchors());
        for (AnchorSpec anchor : anchorOrder) {
            PlotNode node = new PlotNode(
                    plotId(spec.id(), anchor.key()),
                    anchor.key(),
                    anchor.name(),
                    anchor.description(),
                    anchor.role(),
                    spec.region());
            nodes.add(node);
            nodesByKey.put(anchor.key(), node);
            anchorPlotIds.put(anchor.key(), node.id());
            anchorRolePlotIds.computeIfAbsent(anchor.role(), r -> new ArrayList<>()).add(node.id());
            usedKeys.add(anchor.key());
        }

        // Spine: link anchors with breathing room based on difficulty/pacing.
        List<PlotNode> spine = new ArrayList<>();
        for (int i = 0; i < anchorOrder.size() - 1; i++) {
            AnchorSpec from = anchorOrder.get(i);
            AnchorSpec to = anchorOrder.get(i + 1);
            PlotNode fromNode = nodesByKey.get(from.key());
            PlotNode toNode = nodesByKey.get(to.key());
            spine.add(fromNode);

            int remainingBudget = spec.targetPlotCount() - nodes.size();
            int anchorsRemaining = anchorOrder.size() - i - 1;
            int maxFillers = Math.max(0, remainingBudget - anchorsRemaining);
            int fillers = Math.min(maxFillers, pickSpineDistance(spec, random));

            PlotNode current = fromNode;
            for (int j = 0; j < fillers; j++) {
                PlotNode filler = newFiller(spec, "Spine " + (spine.size() + j + 1), random, usedKeys);
                nodes.add(filler);
                spine.add(filler);
                connect(current, filler, gates, usedDirections, random);
                current = filler;
            }
            connect(current, toNode, gates, usedDirections, random);
        }
        spine.add(nodesByKey.get(anchorOrder.get(anchorOrder.size() - 1).key()));

        // Branches off the spine.
        addBranches(spec, random, nodes, gates, usedDirections, spine, usedKeys);

        // Loop to reduce backtracking.
        addLoop(spec, random, nodes, gates, usedDirections, spine, usedKeys);

        // Landmarks for traversal plots lacking flavor.
        applyLandmarks(nodes, random, spec, seed);

        Map<UUID, Coord> coords = embedCoordinates(spine.get(0).id(), gates);
        List<WorldRecipe.PlotSpec> plotSpecs = nodes.stream()
                .map(n -> new WorldRecipe.PlotSpec(
                        n.id(),
                        n.name(),
                        n.region(),
                        coords.getOrDefault(n.id(), new Coord(0, 0)).x(),
                        coords.getOrDefault(n.id(), new Coord(0, 0)).y(),
                        n.description()))
                .toList();

        WorldRecipe recipe = new WorldRecipe(
                seed,
                spine.get(0).id(),
                plotSpecs,
                gates,
                List.of());

        ZoneBuildResult.Metrics metrics = metrics(recipe);
        return new ZoneBuildResult(recipe, metrics, anchorPlotIds, anchorRolePlotIds);
    }

    private static List<AnchorSpec> sortedAnchors(List<AnchorSpec> anchors) {
        List<AnchorRole> order = List.of(
                AnchorRole.ENTRY,
                AnchorRole.GATEKEEPER,
                AnchorRole.SET_PIECE,
                AnchorRole.RESOURCE,
                AnchorRole.VISTA,
                AnchorRole.EXIT
        );
        return anchors.stream()
                .sorted((a, b) -> {
                    int ai = order.indexOf(a.role());
                    int bi = order.indexOf(b.role());
                    int cmp = Integer.compare(ai, bi);
                    if (cmp != 0) {
                        return cmp;
                    }
                    return a.key().compareToIgnoreCase(b.key());
                })
                .toList();
    }

    private static int pickSpineDistance(ZoneSpec spec, Random random) {
        int min;
        int max;
        switch (spec.difficulty()) {
            case EASY -> {
                min = 1;
                max = 3;
            }
            case HARD -> {
                min = 3;
                max = 6;
            }
            default -> {
                min = 2;
                max = 5;
            }
        }
        if (spec.pacing() == PacingProfile.FAST) {
            max = Math.max(min + 1, max - 1);
        } else if (spec.pacing() == PacingProfile.EXPLORATORY) {
            max += 1;
        }
        return min + random.nextInt(Math.max(1, max - min + 1));
    }

    private static int pickBranchLength(ZoneSpec spec, Random random) {
        int max = switch (spec.pacing()) {
            case FAST -> 1;
            case EXPLORATORY -> 3;
            default -> 2;
        };
        int min = 1;
        return min + random.nextInt(Math.max(1, max));
    }

    private static PlotNode newFiller(ZoneSpec spec, String hint, Random random, Set<String> usedKeys) {
        List<String> connectorNames = ZoneGraphConfigHolder.CONFIG.connectorNames();
        if (connectorNames.isEmpty()) {
            throw new IllegalStateException("connectorNames is empty in src/main/resources/storybook/shared/" + ZONE_CONFIG_FILE);
        }
        String baseName = connectorNames.get(random.nextInt(connectorNames.size()));
        String uniqueName = baseName;
        String key = keyFromName(uniqueName);
        int suffix = 2;
        while (usedKeys.contains(key)) {
            uniqueName = baseName + " " + suffix;
            key = keyFromName(uniqueName);
            suffix++;
        }
        usedKeys.add(key);
        return new PlotNode(
                plotId(spec.id(), key),
                key,
                uniqueName,
                "",
                null,
                spec.region());
    }

    private static void connect(
            PlotNode from,
            PlotNode to,
            List<WorldRecipe.GateSpec> gates,
            Map<UUID, Set<Direction>> usedDirections,
            Random random
    ) {
        Direction direction = chooseDirection(from.id(), to.id(), usedDirections, random);
        if (direction == null) {
            return;
        }
        String label = from.name() + " -> " + to.name();
        WorldRecipe.GateSpec gate = new WorldRecipe.GateSpec(
                from.id(),
                direction,
                to.id(),
                true,
                "true",
                label,
                "");
        gates.add(gate);
        usedDirections.computeIfAbsent(from.id(), k -> new HashSet<>()).add(direction);
        Direction opposite = Direction.oppositeOf(direction);
        if (opposite != null) {
            usedDirections.computeIfAbsent(to.id(), k -> new HashSet<>()).add(opposite);
        }
    }

    private static Direction chooseDirection(UUID fromId, UUID toId, Map<UUID, Set<Direction>> used, Random random) {
        List<Direction> candidates = new ArrayList<>();
        Collections.addAll(candidates, DIRECTION_ORDER);
        Collections.shuffle(candidates, random);
        Set<Direction> usedFrom = used.getOrDefault(fromId, Set.of());
        Set<Direction> usedTo = used.getOrDefault(toId, Set.of());
        for (Direction dir : candidates) {
            Direction opposite = Direction.oppositeOf(dir);
            if (usedFrom.contains(dir)) {
                continue;
            }
            if (opposite != null && usedTo.contains(opposite)) {
                continue;
            }
            return dir;
        }
        return null;
    }

    private static void addBranches(
            ZoneSpec spec,
            Random random,
            List<PlotNode> nodes,
            List<WorldRecipe.GateSpec> gates,
            Map<UUID, Set<Direction>> usedDirections,
            List<PlotNode> spine,
            Set<String> usedKeys
    ) {
        int every = switch (spec.pacing()) {
            case FAST -> 3;
            case EXPLORATORY -> 2;
            default -> 2;
        };
        for (int i = 1; i < spine.size() - 1; i += every) {
            if (nodes.size() >= spec.targetPlotCount()) {
                break;
            }
            PlotNode base = spine.get(i);
            int length = Math.min(pickBranchLength(spec, random), spec.targetPlotCount() - nodes.size());
            if (length <= 0) {
                continue;
            }
            PlotNode current = base;
            for (int j = 0; j < length; j++) {
                PlotNode branch = newFiller(spec, "Branch", random, usedKeys);
                nodes.add(branch);
                connect(current, branch, gates, usedDirections, random);
                current = branch;
            }
        }
    }

    private static void addLoop(
            ZoneSpec spec,
            Random random,
            List<PlotNode> nodes,
            List<WorldRecipe.GateSpec> gates,
            Map<UUID, Set<Direction>> usedDirections,
            List<PlotNode> spine,
            Set<String> usedKeys
    ) {
        if (spine.size() < 4 || nodes.size() >= spec.targetPlotCount()) {
            return;
        }
        int attempts = 3;
        while (attempts-- > 0) {
            int a = random.nextInt(spine.size() - 2);
            int b = a + 2 + random.nextInt(Math.max(1, Math.min(5, spine.size() - a - 1)));
            if (b >= spine.size()) {
                continue;
            }
            PlotNode from = spine.get(a);
            PlotNode to = spine.get(b);
            int remainingBudget = spec.targetPlotCount() - nodes.size();
            int fillers = Math.min(remainingBudget, 2 + random.nextInt(3)); // 2-4
            PlotNode current = from;
            for (int i = 0; i < fillers; i++) {
                PlotNode filler = newFiller(spec, "Loop", random, usedKeys);
                nodes.add(filler);
                connect(current, filler, gates, usedDirections, random);
                current = filler;
            }
            connect(current, to, gates, usedDirections, random);
            break;
        }
    }

    private static void applyLandmarks(List<PlotNode> nodes, Random random, ZoneSpec spec, long seed) {
        List<String> hooks = ZoneGraphConfigHolder.CONFIG.landmarkHooks();
        if (hooks.isEmpty()) {
            throw new IllegalStateException("landmarkHooks is empty in src/main/resources/storybook/shared/" + ZONE_CONFIG_FILE);
        }
        int hookIdx = random.nextInt(hooks.size());
        for (PlotNode node : nodes) {
            if (node.description != null && !node.description.isBlank()) {
                continue;
            }
            String hook = hooks.get(hookIdx % hooks.size());
            hookIdx++;
            node.description = DescriptionTemplates.pickFor(node.region(), node.role.orElse(null), seed, hookIdx) + " " + hook;
        }
    }

    private static Map<UUID, Coord> embedCoordinates(UUID startId, List<WorldRecipe.GateSpec> gates) {
        Map<UUID, Coord> coords = new HashMap<>();
        coords.put(startId, new Coord(0, 0));
        boolean updated;
        int safety = gates.size() * 2 + 4;
        do {
            updated = false;
            for (WorldRecipe.GateSpec gate : gates) {
                Coord from = coords.get(gate.fromPlotId());
                Coord to = coords.get(gate.toPlotId());
                if (from != null && to == null) {
                    coords.put(gate.toPlotId(), offset(from, gate.direction()));
                    updated = true;
                } else if (to != null && from == null) {
                    Direction back = Direction.oppositeOf(gate.direction());
                    if (back != null) {
                        coords.put(gate.fromPlotId(), offset(to, back));
                        updated = true;
                    }
                }
            }
        } while (updated && safety-- > 0);
        // Any still-unplaced nodes cluster near origin.
        coords.putIfAbsent(startId, new Coord(0, 0));
        return coords;
    }

    private static Coord offset(Coord origin, Direction dir) {
        int dx = 0;
        int dy = 0;
        switch (dir) {
            case N -> dy = 1;
            case S -> dy = -1;
            case E -> dx = 1;
            case W -> dx = -1;
            case NE -> {
                dx = 1;
                dy = 1;
            }
            case NW -> {
                dx = -1;
                dy = 1;
            }
            case SE -> {
                dx = 1;
                dy = -1;
            }
            case SW -> {
                dx = -1;
                dy = -1;
            }
            case UP -> dy = 1;
            case DOWN -> dy = -1;
        }
        return new Coord(origin.x + dx, origin.y + dy);
    }

    private static ZoneBuildResult.Metrics metrics(WorldRecipe recipe) {
        int nodes = recipe.plots().size();
        int edges = recipe.gates().size();
        int components = 1; // generator keeps it connected
        int loops = Math.max(0, edges - nodes + components);

        Map<UUID, List<UUID>> adj = new HashMap<>();
        for (WorldRecipe.GateSpec gate : recipe.gates()) {
            adj.computeIfAbsent(gate.fromPlotId(), k -> new ArrayList<>()).add(gate.toPlotId());
            adj.computeIfAbsent(gate.toPlotId(), k -> new ArrayList<>()).add(gate.fromPlotId());
        }

        double avgDegree = nodes == 0 ? 0d : (adj.values().stream().mapToInt(List::size).sum() / (double) nodes);
        int longest = longestPath(recipe.startPlotId(), adj);
        return new ZoneBuildResult.Metrics(nodes, edges, loops, avgDegree, longest);
    }

    private static int longestPath(UUID start, Map<UUID, List<UUID>> adj) {
        if (start == null) {
            return 0;
        }
        int longest = 0;
        Deque<UUID> queue = new ArrayDeque<>();
        Map<UUID, Integer> dist = new HashMap<>();
        queue.add(start);
        dist.put(start, 0);
        while (!queue.isEmpty()) {
            UUID u = queue.removeFirst();
            int d = dist.getOrDefault(u, 0);
            longest = Math.max(longest, d);
            for (UUID v : adj.getOrDefault(u, List.of())) {
                if (!dist.containsKey(v)) {
                    dist.put(v, d + 1);
                    queue.addLast(v);
                }
            }
        }
        return longest;
    }

    private static UUID plotId(String zoneId, String key) {
        String slug = zoneId + ":" + key;
        return UUID.nameUUIDFromBytes(slug.getBytes(StandardCharsets.UTF_8));
    }

    private static String keyFromName(String name) {
        if (name == null) {
            return "plot";
        }
        String lower = name.trim().toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder();
        boolean dash = false;
        for (char c : lower.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                sb.append(c);
                dash = false;
            } else if (!dash) {
                sb.append('-');
                dash = true;
            }
        }
        String result = sb.toString();
        while (result.startsWith("-")) {
            result = result.substring(1);
        }
        while (result.endsWith("-")) {
            result = result.substring(0, result.length() - 1);
        }
        return result.isBlank() ? "plot" : result;
    }

    private record Coord(int x, int y) {
    }

    private record ZoneGraphConfig(List<String> landmarkHooks, List<String> connectorNames) {
    }

    private static ZoneGraphConfig loadZoneConfig() {
        Map<String, Object> root = StorybookSharedLoader.load(ZONE_CONFIG_FILE);
        List<String> hooks = stringList(root.get("landmarkHooks"), "landmarkHooks");
        List<String> connectors = stringList(root.get("connectorNames"), "connectorNames");
        return new ZoneGraphConfig(hooks, connectors);
    }

    private static List<String> stringList(Object raw, String field) {
        if (raw == null) {
            return List.of();
        }
        if (!(raw instanceof List<?> list)) {
            throw new IllegalStateException("Expected list for " + field);
        }
        List<String> out = new ArrayList<>();
        for (Object entry : list) {
            if (entry == null) {
                throw new IllegalStateException("Null entry in " + field);
            }
            out.add(entry.toString());
        }
        return List.copyOf(out);
    }

    private static final class ZoneGraphConfigHolder {
        private static final ZoneGraphConfig CONFIG = loadZoneConfig();
    }

    private static final class PlotNode {
        private final UUID id;
        private final String key;
        private final String name;
        private String description;
        private final String region;
        private final Optional<AnchorRole> role;

        PlotNode(UUID id, String key, String name, String description, AnchorRole role, String region) {
            this.id = id;
            this.key = key;
            this.name = name;
            this.description = description == null ? "" : description;
            this.role = Optional.ofNullable(role);
            this.region = region;
        }

        UUID id() {
            return id;
        }

        String name() {
            return name;
        }

        String description() {
            return description;
        }

        String region() {
            return region;
        }
    }
}

package com.demo.adventure.authoring.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.authoring.lang.gdl.GdlLoader;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Utility to export a GameSave YAML or GDL into structured game + subfiles.
 */
public final class GameStructExporter extends BuuiConsole {

    public static void main(String[] args) throws Exception {
        int code = new GameStructExporter().run(args);
        if (code != 0) {
            System.exit(code);
        }
    }

    int run(String[] args) throws Exception {
        Path input = null;
        Path outDir = null;
        String id = null;
        String title = null;
        String preambleOverride = null;
        boolean forceGdl = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h":
                case "--help":
                    printUsage();
                    return 0;
                case "--gdl":
                    forceGdl = true;
                    break;
                case "--in":
                    input = Path.of(requireNext(args, ++i, "--in"));
                    break;
                case "--out":
                    outDir = Path.of(requireNext(args, ++i, "--out"));
                    break;
                case "--id":
                    id = requireNext(args, ++i, "--id");
                    break;
                case "--title":
                    title = requireNext(args, ++i, "--title");
                    break;
                case "--preamble":
                    preambleOverride = requireNext(args, ++i, "--preamble");
                    break;
                default:
                    throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }

        if (input == null || outDir == null || id == null || title == null) {
            printUsage();
            return 1;
        }

        boolean useGdl = forceGdl || (input.getFileName() != null
                && input.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gdl"));
        GameSave save;
        try {
            if (useGdl) {
                save = GdlLoader.load(input);
            } else {
                save = GameSaveYamlLoader.load(input);
            }
        } catch (GdlCompileException ex) {
            System.err.println("Failed to load GDL: " + ex.getMessage());
            return 1;
        } catch (Exception ex) {
            String label = useGdl ? "GDL" : "YAML";
            System.err.println("Failed to load " + label + ": " + ex.getMessage());
            return 1;
        }
        export(save, id, title, preambleOverride, outDir);
        println("Exported structured game to " + outDir.toAbsolutePath());
        return 0;
    }

    private void printUsage() {
        printText("""
                GameStructExporter
                Export a GameSave YAML or GDL into structured game definition files.

                Usage:
                  GameStructExporter --in <file> [--gdl] --out <dir> --id <game-id> --title <title> [--preamble TEXT]

                Options:
                  --gdl    Treat input as GDL (required when extension is not .gdl).
                """);
    }

    private static String requireNext(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + flag);
        }
        return args[index];
    }

    public static void export(GameSave save, String id, String title, String preambleOverride, Path outDir) throws IOException {
        Files.createDirectories(outDir);
        Path worldDir = outDir.resolve("world");
        Path narrativeDir = outDir.resolve("narrative");
        Files.createDirectories(worldDir);
        Files.createDirectories(narrativeDir);

        Map<UUID, String> plotKeys = new LinkedHashMap<>();
        Map<UUID, String> thingKeys = new LinkedHashMap<>();
        save.plots().forEach(p -> plotKeys.put(p.plotId(), keyFromName(p.name())));
        save.fixtures().forEach(f -> thingKeys.put(f.id(), keyFromName(f.name())));
        save.items().forEach(i -> thingKeys.put(i.id(), keyFromName(i.name())));
        save.actors().forEach(a -> thingKeys.put(a.id(), keyFromName(a.name())));
        Map<UUID, String> ownerKeys = new LinkedHashMap<>();
        ownerKeys.putAll(plotKeys);
        ownerKeys.putAll(thingKeys);

        writeGameYaml(save, id, title, preambleOverride, plotKeys.get(save.startPlotId()), outDir.resolve("game.yaml"));
        writeMapYaml(save, plotKeys, worldDir.resolve("map.yaml"));
        writeFixturesYaml(save, ownerKeys, worldDir.resolve("fixtures.yaml"));
        writeItemsYaml(save, ownerKeys, worldDir.resolve("items.yaml"));
        writeActorsYaml(save, ownerKeys, worldDir.resolve("actors.yaml"));
        writeDescriptionsYaml(save, narrativeDir.resolve("descriptions.yaml"));
    }

    private static void writeGameYaml(GameSave save, String id, String title, String preambleOverride, String startPlotKey, Path path) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", id);
        root.put("title", title);
        root.put("seed", save.seed());
        root.put("startPlotKey", startPlotKey);
        root.put("preamble", preambleOverride == null ? save.preamble() : preambleOverride);
        Map<String, String> includes = new LinkedHashMap<>();
        includes.put("map", "world/map.yaml");
        includes.put("fixtures", "world/fixtures.yaml");
        includes.put("items", "world/items.yaml");
        includes.put("actors", "world/actors.yaml");
        includes.put("descriptions", "narrative/descriptions.yaml");
        root.put("includes", includes);
        dump(path, root);
    }

    private static void writeMapYaml(GameSave save, Map<UUID, String> plotKeys, Path path) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> plots = new ArrayList<>();
        save.plots().stream()
                .sorted(Comparator.comparing(WorldRecipe.PlotSpec::name))
                .forEach(p -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", plotKeys.get(p.plotId()));
                    m.put("name", p.name());
                    m.put("region", p.region());
                    m.put("locationX", p.locationX());
                    m.put("locationY", p.locationY());
                    m.put("description", p.description());
                    plots.add(m);
                });

        Map<UUID, WorldRecipe.PlotSpec> plotById = new LinkedHashMap<>();
        save.plots().forEach(p -> plotById.put(p.plotId(), p));

        List<Map<String, Object>> gates = new ArrayList<>();
        save.gates().stream()
                .sorted(Comparator.comparing(WorldRecipe.GateSpec::label))
                .forEach(g -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("from", plotKeys.get(g.fromPlotId()));
                    m.put("direction", g.direction().toLongName());
                    m.put("to", plotKeys.get(g.toPlotId()));
                    m.put("visible", g.visible());
                    m.put("keyString", g.keyString());
                    m.put("label", g.label());
                    m.put("description", g.description());
                    gates.add(m);
                });

        root.put("plots", plots);
        root.put("gates", gates);
        dump(path, root);
    }

    private static void writeFixturesYaml(GameSave save, Map<UUID, String> ownerKeys, Path path) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> fixtures = new ArrayList<>();
        save.fixtures().stream()
                .sorted(Comparator.comparing(WorldRecipe.FixtureSpec::name))
                .forEach(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", keyFromName(f.name()));
                    m.put("name", f.name());
                    m.put("description", f.description());
                    m.put("ownerKey", ownerKeys.get(f.ownerId()));
                    m.put("visible", f.visible());
                    Map<String, Object> cells = cellsToYaml(f.cells());
                    if (cells != null) {
                        m.put("cells", cells);
                    }
                    fixtures.add(m);
                });
        root.put("fixtures", fixtures);
        dump(path, root);
    }

    private static void writeItemsYaml(GameSave save, Map<UUID, String> ownerKeys, Path path) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        save.items().stream()
                .sorted(Comparator.comparing(GameSave.ItemRecipe::name))
                .forEach(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", keyFromName(i.name()));
                    m.put("name", i.name());
                    m.put("description", i.description());
                    m.put("ownerKey", ownerKeys.getOrDefault(i.ownerId(), keyFromName(i.name())));
                    m.put("visible", i.visible());
                    m.put("fixture", i.fixture());
                    m.put("keyString", i.keyString());
                    m.put("footprintWidth", i.footprintWidth());
                    m.put("footprintHeight", i.footprintHeight());
                    m.put("capacityWidth", i.capacityWidth());
                    m.put("capacityHeight", i.capacityHeight());
                    if (i.weaponDamage() > 0) {
                        m.put("weaponDamage", i.weaponDamage());
                    }
                    if (i.armorMitigation() > 0) {
                        m.put("armorMitigation", i.armorMitigation());
                    }
                    Map<String, Object> cells = cellsToYaml(i.cells());
                    if (cells != null) {
                        m.put("cells", cells);
                    }
                    items.add(m);
                });
        root.put("items", items);
        dump(path, root);
    }

    private static void writeActorsYaml(GameSave save, Map<UUID, String> ownerKeys, Path path) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> actors = new ArrayList<>();
        save.actors().stream()
                .sorted(Comparator.comparing(GameSave.ActorRecipe::name))
                .forEach(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", keyFromName(a.name()));
                    m.put("name", a.name());
                    m.put("description", a.description());
                    m.put("ownerKey", ownerKeys.getOrDefault(a.ownerId(), keyFromName(a.name())));
                    m.put("visible", a.visible());
                    m.put("skills", a.skills());
                    if (a.equippedMainHandItemId() != null) {
                        m.put("equippedMainHandItemId", ownerKeys.get(a.equippedMainHandItemId()));
                    }
                    if (a.equippedBodyItemId() != null) {
                        m.put("equippedBodyItemId", ownerKeys.get(a.equippedBodyItemId()));
                    }
                    Map<String, Object> cells = cellsToYaml(a.cells());
                    if (cells != null) {
                        m.put("cells", cells);
                    }
                    actors.add(m);
                });
        root.put("actors", actors);
        dump(path, root);
    }

    private static void writeDescriptionsYaml(GameSave save, Path path) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("plots", save.plots().stream()
                .sorted(Comparator.comparing(WorldRecipe.PlotSpec::name))
                .map(p -> descriptionEntry(keyFromName(p.name()), p.description()))
                .toList());
        root.put("fixtures", save.fixtures().stream()
                .sorted(Comparator.comparing(WorldRecipe.FixtureSpec::name))
                .map(f -> descriptionEntry(keyFromName(f.name()), f.description()))
                .toList());
        root.put("items", save.items().stream()
                .sorted(Comparator.comparing(GameSave.ItemRecipe::name))
                .map(i -> descriptionEntry(keyFromName(i.name()), i.description()))
                .toList());
        root.put("actors", save.actors().stream()
                .sorted(Comparator.comparing(GameSave.ActorRecipe::name))
                .map(a -> descriptionEntry(keyFromName(a.name()), a.description()))
                .toList());
        dump(path, root);
    }

    private static Map<String, Object> cellsToYaml(Map<String, CellSpec> cells) {
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

    private static Map<String, Object> descriptionEntry(String key, String description) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("key", key);
        entry.put("description", description);
        // Simple history structure for downstream editing/versioning.
        List<Map<String, Object>> history = new ArrayList<>();
        Map<String, Object> version = new LinkedHashMap<>();
        version.put("text", description);
        version.put("worldClock", 0);
        history.add(version);
        entry.put("history", history);
        return entry;
    }

    private static void dump(Path path, Object root) throws IOException {
        Files.createDirectories(path.getParent());
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setWidth(120);
        Yaml yaml = new Yaml(options);
        Files.writeString(path, yaml.dump(root), StandardCharsets.UTF_8);
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
}

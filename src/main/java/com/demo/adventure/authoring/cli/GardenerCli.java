package com.demo.adventure.authoring.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.authoring.lang.gdl.GdlLoader;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.authoring.gardener.GardenerPatch;
import com.demo.adventure.authoring.gardener.GardenerPatchApplier;
import com.demo.adventure.authoring.gardener.GardenerPatchValidator;
import com.demo.adventure.authoring.gardener.WorldFingerprint;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * CLI to apply a gardener narration patch to a GameSave and emit the patched YAML.
 * This keeps topology unchanged while updating player-facing text.
 */
public final class GardenerCli extends BuuiConsole {

    public static void main(String[] args) throws Exception {
        Path input = null;
        Path patchPath = null;
        Path output = Path.of("logs/gardened.yaml");
        boolean validateOnly = false;
        boolean forceGdl = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--in", "-i" -> input = Path.of(args[++i]);
                case "--patch", "-p" -> patchPath = Path.of(args[++i]);
                case "--out", "-o" -> output = Path.of(args[++i]);
                case "--validate-only" -> validateOnly = true;
                case "--gdl" -> forceGdl = true;
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
        if (input == null || patchPath == null) {
            printHelp();
            return;
        }
        try {
            new GardenerCli().run(input, patchPath, output, validateOnly, forceGdl);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
    }

    private void run(Path input, Path patchPath, Path output, boolean validateOnly, boolean forceGdl) throws IOException {
        GameSave save = loadSave(input, forceGdl);
        GardenerPatch patch = readPatch(patchPath);

        // Pattern: Verification
        // - Require patch/world fingerprint match before applying any narration changes.
        String fingerprint = WorldFingerprint.fingerprint(save);
        String patchFingerprint = patch.metadata() == null ? "" : patch.metadata().worldFingerprint();
        if (patchFingerprint != null && !patchFingerprint.isBlank() && !patchFingerprint.equals(fingerprint)) {
            System.err.println("Patch worldFingerprint mismatch. Patch is for a different world.");
            return;
        }

        GardenerPatchValidator.ValidationResult result = GardenerPatchValidator.validate(
                save,
                patch,
                40,
                160,
                32,
                120
        );
        if (!result.ok()) {
            System.err.println("Patch failed validation:");
            result.problems().forEach(p -> System.err.println("- " + p));
            return;
        }

        // Pattern: Verification
        // - Enforce full plot coverage and warn on missing things to prevent partial patching.
        Coverage coverage = computeCoverage(save, patch);
        if (coverage.missingPlots() > 0) {
            System.err.println("Patch failed coverage: missing plot patches " + coverage.missingPlots() + "/" + coverage.totalPlots() +
                    ". First missing plotId: " + coverage.firstMissingPlotId());
            System.err.println("Patch must include every plotId. Regenerate the patch with full plot coverage.");
            return;
        }

        if (validateOnly) {
            int plotChanges = countPlotChanges(save, patch);
            int thingChanges = countThingChanges(save, patch);
            print("Validation OK");
            print("Fingerprint: " + fingerprint + " (matches)");
            print("Plots changed: " + plotChanges + " | Things changed: " + thingChanges);
            String fpPrefix = fingerprint == null ? "" : fingerprint.substring(0, Math.min(8, fingerprint.length()));
            print("Coverage: plots " + coverage.patchedPlots() + "/" + coverage.totalPlots() + " (PASS), things " +
                    coverage.patchedThings() + "/" + coverage.totalThings() + (coverage.missingThings() > 0 ? " (WARN)" : " (PASS)") +
                    " | fingerprint " + fpPrefix);
            if (coverage.missingThings() > 0) {
                print("Warning: missing thing patches, sample missing IDs: " + coverage.sampleMissingThings());
            }
            if (result.warnings() != null && !result.warnings().isEmpty()) {
                result.warnings().forEach(w -> print("Warning: " + w));
            }
            return;
        }

        GameSave patched = GardenerPatchApplier.apply(save, patch);
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        GameSaveYamlWriter.write(patched, output);
        print("Patched game written to " + output.toAbsolutePath());
        if (result.warnings() != null && !result.warnings().isEmpty()) {
            result.warnings().forEach(w -> print("Warning: " + w));
        }
    }

    private GardenerPatch readPatch(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        Object raw = new Yaml().load(text);
        if (!(raw instanceof Map<?, ?> m)) {
            throw new IOException("Patch must be a YAML map");
        }
        GardenerPatch.Metadata meta = new GardenerPatch.Metadata(
                number(m.get("seed")),
                str(m.get("worldFingerprint")),
                str(m.get("modelId")),
                str(m.get("promptVersion"))
        );

        Map<UUID, GardenerPatch.PlotPatch> plots = new HashMap<>();
        Object plotsObj = m.get("plots");
        if (plotsObj instanceof Map<?, ?> plotMap) {
            for (Map.Entry<?, ?> e : plotMap.entrySet()) {
                UUID id = uuid(e.getKey());
                if (id == null || !(e.getValue() instanceof Map<?, ?> pm)) {
                    continue;
                }
                plots.put(id, new GardenerPatch.PlotPatch(str(pm.get("displayTitle")), str(pm.get("description"))));
            }
        }

        Map<UUID, GardenerPatch.ThingPatch> things = new HashMap<>();
        Object thingsObj = m.get("things");
        if (thingsObj instanceof Map<?, ?> thingMap) {
            for (Map.Entry<?, ?> e : thingMap.entrySet()) {
                UUID id = uuid(e.getKey());
                if (id == null || !(e.getValue() instanceof Map<?, ?> tm)) {
                    continue;
                }
                things.put(id, new GardenerPatch.ThingPatch(str(tm.get("displayName")), str(tm.get("description"))));
            }
        }

        return new GardenerPatch(meta, plots, things);
    }

    private static UUID uuid(Object key) {
        try {
            return UUID.fromString(String.valueOf(key));
        } catch (Exception ex) {
            return null;
        }
    }

    private static String str(Object obj) {
        return obj == null ? null : obj.toString();
    }

    private static long number(Object obj) {
        if (obj instanceof Number n) {
            return n.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(obj));
        } catch (Exception ex) {
            return 0L;
        }
    }

    private static void printHelp() {
        printText("""
                GardenerCli --in game.yaml|game.gdl [--gdl] --patch patch.yaml --out patched.yaml [--validate-only]
                Applies a narration patch (titles/descriptions only) to a GameSave without changing topology.
                """);
    }

    private GameSave loadSave(Path input, boolean forceGdl) throws IOException {
        boolean useGdl = forceGdl || (input.getFileName() != null
                && input.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gdl"));
        try {
            if (useGdl) {
                return GdlLoader.load(input);
            }
            return GameSaveYamlLoader.load(input);
        } catch (GdlCompileException ex) {
            throw new IOException("Failed to load GDL: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            String label = useGdl ? "GDL" : "YAML";
            throw new IOException("Failed to load " + label + ": " + ex.getMessage(), ex);
        }
    }

    private static int countPlotChanges(GameSave save, GardenerPatch patch) {
        if (patch.plots() == null) {
            return 0;
        }
        int changes = 0;
        Map<UUID, GardenerPatch.PlotPatch> map = patch.plots();
        for (var p : save.plots()) {
            GardenerPatch.PlotPatch pp = map.get(p.plotId());
            if (pp == null) continue;
            if (!safeEquals(p.name(), pp.displayTitle()) || !safeEquals(p.description(), pp.description())) {
                changes++;
            }
        }
        return changes;
    }

    private static int countThingChanges(GameSave save, GardenerPatch patch) {
        if (patch.things() == null) {
            return 0;
        }
        int changes = 0;
        Map<UUID, GardenerPatch.ThingPatch> map = patch.things();
        for (var f : save.fixtures()) {
            GardenerPatch.ThingPatch tp = map.get(f.id());
            if (tp != null && (!safeEquals(f.name(), tp.displayName()) || !safeEquals(f.description(), tp.description()))) {
                changes++;
            }
        }
        for (var i : save.items()) {
            GardenerPatch.ThingPatch tp = map.get(i.id());
            if (tp != null && (!safeEquals(i.name(), tp.displayName()) || !safeEquals(i.description(), tp.description()))) {
                changes++;
            }
        }
        for (var a : save.actors()) {
            GardenerPatch.ThingPatch tp = map.get(a.id());
            if (tp != null && (!safeEquals(a.name(), tp.displayName()) || !safeEquals(a.description(), tp.description()))) {
                changes++;
            }
        }
        return changes;
    }

    private static boolean safeEquals(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private record Coverage(int totalPlots, int patchedPlots, int missingPlots,
                            int totalThings, int patchedThings, int missingThings,
                            UUID firstMissingPlotId, UUID firstMissingThingId,
                            String sampleMissingThings) {
    }

    // Pattern: Verification
    // - Computes coverage counts to detect missing plot/thing patches before writing output.
    private static Coverage computeCoverage(GameSave save, GardenerPatch patch) {
        var plotIds = save.plots().stream().map(p -> p.plotId()).toList();
        var thingIds = new java.util.ArrayList<UUID>();
        save.fixtures().forEach(f -> thingIds.add(f.id()));
        save.items().forEach(i -> thingIds.add(i.id()));
        save.actors().forEach(a -> thingIds.add(a.id()));

        int patchedPlots = patch.plots() == null ? 0 : patch.plots().size();
        int patchedThings = patch.things() == null ? 0 : patch.things().size();

        UUID firstMissingPlot = null;
        int missingPlotCount = 0;
        for (UUID id : plotIds) {
            if (patch.plots() == null || !patch.plots().containsKey(id)) {
                missingPlotCount++;
                if (firstMissingPlot == null) {
                    firstMissingPlot = id;
                }
            }
        }

        UUID firstMissingThing = null;
        int missingThingCount = 0;
        for (UUID id : thingIds) {
            if (patch.things() == null || !patch.things().containsKey(id)) {
                missingThingCount++;
                if (firstMissingThing == null) {
                    firstMissingThing = id;
                }
            }
        }

        // build sample missing things (up to 3)
        StringBuilder sampleMissing = new StringBuilder();
        int listed = 0;
        for (UUID id : thingIds) {
            if (patch.things() == null || !patch.things().containsKey(id)) {
                if (listed < 3) {
                    if (sampleMissing.length() > 0) sampleMissing.append(", ");
                    sampleMissing.append(id);
                    listed++;
                }
            }
        }

        return new Coverage(
                plotIds.size(),
                patchedPlots,
                missingPlotCount,
                thingIds.size(),
                patchedThings,
                missingThingCount,
                firstMissingPlot,
                firstMissingThing,
                sampleMissing.toString()
        );
    }
}

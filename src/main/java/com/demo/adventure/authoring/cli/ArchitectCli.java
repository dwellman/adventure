package com.demo.adventure.authoring.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.domain.save.WorldRecipe;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic "architect" that turns a simple plot list into a walkabout GameSave YAML.
 * Accepts Markdown lines like: P001 **Name** — Description text
 */
public final class ArchitectCli extends BuuiConsole {
    private static final Pattern PLOT_LINE = Pattern.compile("^P(\\d+)\\s+\\*?\\*?(.+?)\\*?\\*?\\s+—\\s+(.*)$");

    public static void main(String[] args) {
        int code = new ArchitectCli().run(args);
        if (code != 0) {
            System.exit(code);
        }
    }

    int run(String[] args) {
        if (args == null || args.length == 0) {
            printUsage();
            return 1;
        }

        Path input = null;
        Path output = null;
        long seed = 1L;
        String region = "CITY";
        String preamble = null;
        String startKey = null;
        boolean skipValidate = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-h":
                case "--help":
                    printUsage();
                    return 0;
                case "-i":
                case "--in":
                    if (++i >= args.length) {
                        System.err.println("Missing value for " + arg);
                        return 1;
                    }
                    input = Path.of(args[i]);
                    break;
                case "-o":
                case "--out":
                    if (++i >= args.length) {
                        System.err.println("Missing value for " + arg);
                        return 1;
                    }
                    output = Path.of(args[i]);
                    break;
                case "--seed":
                    if (++i >= args.length) {
                        System.err.println("Missing value for --seed");
                        return 1;
                    }
                    seed = Long.parseLong(args[i]);
                    break;
                case "--region":
                    if (++i >= args.length) {
                        System.err.println("Missing value for --region");
                        return 1;
                    }
                    region = args[i];
                    break;
                case "--preamble":
                    if (++i >= args.length) {
                        System.err.println("Missing value for --preamble");
                        return 1;
                    }
                    preamble = args[i];
                    break;
                case "--start":
                    if (++i >= args.length) {
                        System.err.println("Missing value for --start");
                        return 1;
                    }
                    startKey = normalizeKey(args[i]);
                    break;
                case "--skip-validate":
                    skipValidate = true;
                    break;
                default:
                    if (input == null) {
                        input = Path.of(arg);
                    } else {
                        System.err.println("Unexpected argument: " + arg);
                        printUsage();
                        return 1;
                    }
            }
        }

        if (input == null) {
            System.err.println("Input file is required.");
            printUsage();
            return 1;
        }

        List<PlotLine> plots;
        try {
            plots = parsePlots(input);
        } catch (IOException ex) {
            System.err.println("Failed to read input: " + ex.getMessage());
            return 1;
        } catch (IllegalArgumentException ex) {
            System.err.println("Failed to parse plots: " + ex.getMessage());
            return 1;
        }

        if (plots.isEmpty()) {
            System.err.println("No plots found in input.");
            return 1;
        }

        UUID startId;
        if (startKey != null) {
            String targetKey = startKey;
            startId = plots.stream()
                    .filter(p -> p.key().equals(targetKey))
                    .findFirst()
                    .map(PlotLine::id)
                    .orElseThrow(() -> new IllegalArgumentException("Start plot key not found: " + targetKey));
        } else {
            startId = plots.get(0).id();
        }

        WorldRecipe recipe = toRecipe(seed, region, plots, startId);
        GameSave save = new GameSave(seed, startId, preamble != null ? preamble : plots.get(0).description(), recipe.plots(), recipe.gates(), List.of(), List.of(), List.of());

        if (!skipValidate) {
            try {
                new GameSaveAssembler().apply(save);
            } catch (GameBuilderException gbe) {
                System.err.println("Validation failed: " + gbe.getMessage());
                if (gbe.getReport() != null) {
                    System.err.println(gbe.getReport());
                }
                return 1;
            } catch (Exception ex) {
                System.err.println("Validation failed: " + ex.getMessage());
                return 1;
            }
        }

        try {
            if (output != null) {
                GameSaveYamlWriter.write(save, output);
                print("Wrote YAML to " + output.toAbsolutePath());
            } else {
                printText(GameSaveYamlWriter.toYaml(save));
            }
        } catch (Exception ex) {
            System.err.println("Failed to write output: " + ex.getMessage());
            return 1;
        }

        return 0;
    }

    private void printUsage() {
        printText("""
                Architect CLI
                Parse a simple plot list (e.g., P001 **Name** — Description) into a walkabout GameSave YAML.

                Usage:
                  architect --in FILE [--out FILE|--stdout] [--seed N] [--region NAME] [--preamble TEXT] [--start P###] [--skip-validate]
                """);
    }

    private static List<PlotLine> parsePlots(Path file) throws IOException {
        List<PlotLine> plots = new ArrayList<>();
        for (String rawLine : Files.readAllLines(file, StandardCharsets.UTF_8)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            Matcher m = PLOT_LINE.matcher(line);
            if (!m.matches()) {
                continue;
            }
            String key = normalizeKey("p" + m.group(1));
            String name = m.group(2).trim();
            String description = m.group(3).trim();
            plots.add(new PlotLine(key, name, description, uuid("plot", key)));
        }
        return plots;
    }

    private static WorldRecipe toRecipe(long seed, String region, List<PlotLine> plots, UUID startId) {
        List<WorldRecipe.PlotSpec> plotSpecs = new ArrayList<>();
        List<WorldRecipe.GateSpec> gateSpecs = new ArrayList<>();

        int idx = 0;
        for (PlotLine plot : plots) {
            plotSpecs.add(new WorldRecipe.PlotSpec(
                    plot.id(),
                    plot.name(),
                    region,
                    idx,
                    0,
                    plot.description()
            ));
            idx++;
        }

        for (int i = 0; i < plots.size() - 1; i++) {
            PlotLine a = plots.get(i);
            PlotLine b = plots.get(i + 1);
            gateSpecs.add(new WorldRecipe.GateSpec(
                    a.id(),
                    Direction.E,
                    b.id(),
                    true,
                    "true",
                    a.name() + " -> " + b.name(),
                    "Path between " + a.name() + " and " + b.name()
            ));
            gateSpecs.add(new WorldRecipe.GateSpec(
                    b.id(),
                    Direction.W,
                    a.id(),
                    true,
                    "true",
                    b.name() + " -> " + a.name(),
                    "Path between " + b.name() + " and " + a.name()
            ));
        }

        return new WorldRecipe(seed, startId, plotSpecs, gateSpecs, List.of());
    }

    private static UUID uuid(String kind, String key) {
        return UUID.nameUUIDFromBytes((kind + ":" + key).getBytes(StandardCharsets.UTF_8));
    }

    private static String normalizeKey(String key) {
        Objects.requireNonNull(key, "key");
        String trimmed = key.trim().toLowerCase();
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
        return normalized;
    }

    private record PlotLine(String key, String name, String description, UUID id) {
    }
}

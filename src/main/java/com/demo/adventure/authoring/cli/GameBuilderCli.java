package com.demo.adventure.authoring.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.authoring.lang.gdl.GdlLoader;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterials;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterialsFormatter;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterialsGenerator;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldBuildReportFormatter;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Simple CLI utility to load a GameSave YAML or GDL, optionally validate/build it,
 * and emit a deterministic YAML snapshot.
 *
 * Usage:
 * mvn -q -Dexec.mainClass=com.demo.adventure.authoring.cli.GameBuilderCli -Dexec.args="input.yaml --out output.yaml" exec:java
 */
public final class GameBuilderCli extends BuuiConsole {

    public static void main(String[] args) {
        int exitCode = new GameBuilderCli().run(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    int run(String[] args) {
        if (args == null || args.length == 0) {
            printUsage();
            return 1;
        }

        Path input = null;
        Path output = null;
        boolean toStdout = false;
        boolean skipValidate = false;
        boolean showBom = false;
        boolean showReport = false;
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
                case "-o":
                case "--out":
                    if (i + 1 >= args.length) {
                        System.err.println("Missing value for " + arg);
                        return 1;
                    }
                    output = Path.of(args[++i]);
                    break;
                case "--stdout":
                    toStdout = true;
                    break;
                case "--skip-validate":
                    skipValidate = true;
                    break;
                case "--bom":
                    showBom = true;
                    break;
                case "--report":
                    showReport = true;
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
        if (output != null && toStdout) {
            System.err.println("Choose either --out or --stdout, not both.");
            return 1;
        }

        boolean useGdl = forceGdl || (input.getFileName() != null
                && input.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".gdl"));
        GameSave save;
        try {
            if (useGdl) {
                save = GdlLoader.load(input);
            } else {
                // Prefer structured loader for game.yaml files that use includes; fall back to legacy loader otherwise.
                if (input.getFileName() != null && input.getFileName().toString().equalsIgnoreCase("game.yaml")) {
                    save = StructuredGameSaveLoader.load(input);
                } else {
                    save = GameSaveYamlLoader.load(input);
                }
            }
        } catch (GdlCompileException ex) {
            System.err.println("Failed to load GDL: " + ex.getMessage());
            return 1;
        } catch (Exception ex) {
            String label = useGdl ? "GDL" : "YAML";
            System.err.println("Failed to load " + label + ": " + ex.getMessage());
            return 1;
        }

        WorldBuildResult buildResult = null;
        if (!skipValidate) {
            try {
                buildResult = new GameSaveAssembler().apply(save);
            } catch (GameBuilderException gbe) {
                System.err.println("Build failed: " + gbe.getMessage());
                WorldBuildReport report = gbe.getReport();
                if (report != null) {
                    System.err.println(WorldBuildReportFormatter.format(report));
                }
                return 1;
            } catch (Exception ex) {
                System.err.println("Build failed: " + ex.getMessage());
                return 1;
            }
        }

        if (buildResult != null && showReport) {
            printText(WorldBuildReportFormatter.format(buildResult.report()));
        }
        if (buildResult != null && showBom) {
            WorldBillOfMaterials bom = WorldBillOfMaterialsGenerator.fromRegistry(buildResult.registry());
            printText(WorldBillOfMaterialsFormatter.format(bom));
        }

        try {
            if (output != null) {
                GameSaveYamlWriter.write(save, output);
                print("Wrote YAML to " + output.toAbsolutePath());
            } else {
                // Default to stdout for ease of piping or redirects.
                printText(GameSaveYamlWriter.toYaml(save));
            }
        } catch (Exception ex) {
            System.err.println("Failed to write YAML: " + ex.getMessage());
            return 1;
        }

        return 0;
    }

    private void printUsage() {
        printText("""
                GameBuilder CLI
                Loads a GameSave YAML or GDL, optionally validates/builds it, and writes a deterministic YAML snapshot.

                Usage:
                  gamebuilder <input.yaml|input.gdl> [--gdl] [--out <file> | --stdout] [--skip-validate] [--bom] [--report]

                Options:
                  -h, --help         Show this help.
                  --gdl              Treat input as GDL (required when extension is not .gdl).
                  -o, --out FILE     Write YAML to FILE.
                  --stdout           Write YAML to stdout (default when no --out is given).
                  --skip-validate    Skip world build/validation. Still rewrites YAML deterministically.
                  --bom              Print a bill of materials after a successful build.
                  --report           Print the build/validation report after a successful build.
                """);
    }
}

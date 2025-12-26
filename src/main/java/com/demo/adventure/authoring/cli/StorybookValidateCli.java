package com.demo.adventure.authoring.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldBuildReportFormatter;

import java.nio.file.Path;

/**
 * Validate a storybook GameSave YAML by loading and assembling it.
 */
public final class StorybookValidateCli extends BuuiConsole {

    public static void main(String[] args) {
        int code = new StorybookValidateCli().run(args);
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
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsage();
                return 0;
            }
            if (input == null) {
                input = Path.of(arg);
            } else {
                System.err.println("Unexpected argument: " + arg);
                printUsage();
                return 1;
            }
        }

        if (input == null) {
            printUsage();
            return 1;
        }

        try {
            GameSave save = GameSaveYamlLoader.load(input);
            new GameSaveAssembler().apply(save);
            print("Validation OK");
            return 0;
        } catch (GameBuilderException ex) {
            System.err.println("Validation failed: " + ex.getMessage());
            WorldBuildReport report = ex.getReport();
            if (report != null) {
                System.err.println(WorldBuildReportFormatter.format(report));
            }
            return 1;
        } catch (Exception ex) {
            System.err.println("Failed to load YAML: " + ex.getMessage());
            return 1;
        }
    }

    private static void printUsage() {
        printText("""
                StorybookValidateCli
                Validate a storybook GameSave YAML by loading it and building the world registry.

                Usage:
                  StorybookValidateCli <game.yaml>
                """);
    }
}

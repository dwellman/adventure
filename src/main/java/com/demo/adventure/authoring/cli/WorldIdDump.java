package com.demo.adventure.authoring.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.authoring.lang.gdl.GdlLoader;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Utility to dump plot/fixture/item/actor IDs for a GameSave YAML.
 */
public final class WorldIdDump extends BuuiConsole {
    public static void main(String[] args) {
        Path input = null;
        boolean forceGdl = false;
        for (String arg : args) {
            if ("--gdl".equals(arg)) {
                forceGdl = true;
                continue;
            }
            if ("--help".equals(arg) || "-h".equals(arg)) {
                printUsage();
                return;
            }
            if (input == null) {
                input = Path.of(arg);
            } else {
                System.err.println("Unexpected argument: " + arg);
                printUsage();
                return;
            }
        }
        if (input == null) {
            printUsage();
            return;
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
            return;
        } catch (Exception ex) {
            String label = useGdl ? "GDL" : "YAML";
            System.err.println("Failed to load " + label + ": " + ex.getMessage());
            return;
        }
        printText("Plots:");
        save.plots().forEach(p -> printText(p.plotId() + " | " + p.name() + " | " + p.region()));
        printText("Fixtures:");
        save.fixtures().forEach(f -> printText(f.id() + " | " + f.name()));
        printText("Items:");
        save.items().forEach(i -> printText(i.id() + " | " + i.name()));
        printText("Actors:");
        save.actors().forEach(a -> printText(a.id() + " | " + a.name()));
    }

    private static void printUsage() {
        System.err.println("Usage: WorldIdDump <game.yaml|game.gdl> [--gdl]");
    }
}

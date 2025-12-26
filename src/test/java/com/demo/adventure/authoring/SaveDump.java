package com.demo.adventure.authoring;

import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.authoring.samples.ClueMansion;
import com.demo.adventure.authoring.samples.IslandAdventure;

/**
 * Helper to dump a deterministic GameSave to YAML using the current writer format.
 *
 * Usage:
 * mvn -q -DskipTests -Dexec.classpathScope=test -Dexec.mainClass=com.demo.adventure.authoring.SaveDump exec:java
 * mvn -q -DskipTests -Dexec.classpathScope=test -Dexec.mainClass=com.demo.adventure.authoring.SaveDump exec:java -Dexec.args="adventure src/main/resources/cookbook/gardened-adventure.yaml"
 */
public final class SaveDump {
    private SaveDump() {
    }

    public static void main(String[] args) throws Exception {
        String world = args.length > 0 ? args[0] : "clue";
        String output = args.length > 1 ? args[1] : "logs/" + world + "-save.yaml";

        GameSave save = selectSave(world);
        java.nio.file.Path path = java.nio.file.Path.of(output);
        GameSaveYamlWriter.write(save, path);
        System.out.println("Save dump written to " + path.toAbsolutePath());
    }

    private static GameSave selectSave(String world) {
        return switch (world.toLowerCase()) {
            case "island", "island-adventure" -> IslandAdventure.gameSave();
            case "clue", "mansion", "clue-mansion" -> ClueMansion.gameSave();
            default -> throw new IllegalArgumentException("Unknown world: " + world);
        };
    }
}

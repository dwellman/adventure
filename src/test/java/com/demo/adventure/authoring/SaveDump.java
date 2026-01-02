package com.demo.adventure.authoring;

import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;

import java.nio.file.Path;

/**
 * Helper to dump a deterministic GameSave to YAML using the current writer format.
 *
 * Usage:
 * mvn -q -DskipTests -Dexec.classpathScope=test -Dexec.mainClass=com.demo.adventure.authoring.SaveDump exec:java
 * mvn -q -DskipTests -Dexec.classpathScope=test -Dexec.mainClass=com.demo.adventure.authoring.SaveDump exec:java -Dexec.args="mansion src/main/resources/cookbook/gardened-mansion.yaml"
 */
public final class SaveDump {
    private SaveDump() {
    }

    public static void main(String[] args) throws Exception {
        String world = args.length > 0 ? args[0] : "clue";
        String output = args.length > 1 ? args[1] : "logs/" + world + "-save.yaml";

        GameSave save = selectSave(world);
        Path path = Path.of(output);
        GameSaveYamlWriter.write(save, path);
        System.out.println("Save dump written to " + path.toAbsolutePath());
    }

    private static GameSave selectSave(String world) throws Exception {
        return switch (world.toLowerCase()) {
            case "island", "island-adventure" ->
                    StructuredGameSaveLoader.load(Path.of("src/main/resources/games/island/game.yaml"));
            case "clue", "mansion", "clue-mansion" ->
                    StructuredGameSaveLoader.load(Path.of("src/main/resources/games/mansion/game.yaml"));
            default -> throw new IllegalArgumentException("Unknown world: " + world);
        };
    }
}

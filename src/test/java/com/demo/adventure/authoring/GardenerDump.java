package com.demo.adventure.authoring;

import com.demo.adventure.authoring.save.build.WorldBuildReportFormatter;
import com.demo.adventure.authoring.gardener.FixtureDescriptionExpander;
import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.authoring.gardener.Gardener;
import com.demo.adventure.authoring.gardener.NoopFixtureDescriptionExpander;
import com.demo.adventure.authoring.gardener.ai.OpenAiHttpFixtureDescriptionExpander;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;

import java.nio.file.Path;

/**
 * One-off helper to run the Gardener against a world and dump YAML with description history.
 *
 * Usage (from repo root):
 * mvn -q -DskipTests -Dexec.classpathScope=test -Dexec.mainClass=com.demo.adventure.authoring.GardenerDump exec:java
 * mvn -q -DskipTests -Dexec.classpathScope=test -Dexec.mainClass=com.demo.adventure.authoring.GardenerDump exec:java -Dexec.args=\"adventure logs/gardened-adventure.yaml\"
 */
public final class GardenerDump {
    private GardenerDump() {
    }

    public static void main(String[] args) throws Exception {
        boolean useAi = false;
        String world = "clue";
        String output = null;
        boolean worldSet = false;
        for (String arg : args) {
            if ("--ai".equalsIgnoreCase(arg)) {
                useAi = true;
                continue;
            }
            if (!worldSet) {
                world = arg;
                worldSet = true;
            } else if (output == null) {
                output = arg;
            }
        }
        output = output == null ? "logs/gardened-" + world + ".yaml" : output;
        GameSave save = selectSave(world);

        FixtureDescriptionExpander expander = useAi
                ? new OpenAiHttpFixtureDescriptionExpander(voiceFor(world), overviewFor(world))
                : new NoopFixtureDescriptionExpander();
        Gardener gardener = new Gardener(expander);
        GardenResult result = gardener.garden(save);

        if (!result.report().getProblems().isEmpty()) {
            System.out.println(WorldBuildReportFormatter.format(result.report()));
        }

        Path path = Path.of(output);
        GameSaveYamlWriter.write(result, path);
        System.out.println("Gardener dump written to " + path.toAbsolutePath());
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

    private static String voiceFor(String world) {
        return switch (world.toLowerCase()) {
            case "clue", "mansion", "clue-mansion" -> "A playful Disney Haunted Mansion storyteller with a Clue mystery vibe.";
            case "island", "island-adventure" -> "A pulpy Indiana Jones explorer narrating discoveries.";
            default -> "";
        };
    }

    private static String overviewFor(String world) {
        return switch (world.toLowerCase()) {
            case "clue", "mansion", "clue-mansion" -> "A heightened mansion of secrets; a whimsical yet eerie tour where every room hides clues.";
            case "island", "island-adventure" -> "A stranded explorer journey across a mysterious adventure, hopeful and bold.";
            default -> "";
        };
    }
}

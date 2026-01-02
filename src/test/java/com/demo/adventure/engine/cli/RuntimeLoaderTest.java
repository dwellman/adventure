package com.demo.adventure.engine.cli;

import com.demo.adventure.ai.runtime.smart.SmartActorSpec;
import com.demo.adventure.ai.runtime.smart.SmartActorTagIndex;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.trigger.TriggerDefinition;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.domain.save.GameSave;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuntimeLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadSaveFallsBackForLegacyYaml() throws Exception {
        GameSave save = RuntimeLoader.loadSave("src/test/resources/integrity/mini.yaml");

        assertThat(save.preamble()).contains("A test preamble.");
        assertThat(save.plots()).isNotEmpty();
    }

    @Test
    void loadSaveThrowsForBrokenStructuredGameYaml() throws Exception {
        Path gameFile = tempDir.resolve("game.yaml");
        Files.writeString(gameFile, "seed: 1\nstartPlotKey: start\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> RuntimeLoader.loadSave(gameFile.toString()))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Failed to load structured game");
    }

    @Test
    void loadSaveFromClasspath() throws Exception {
        GameSave save = RuntimeLoader.loadSave("integrity/mini.yaml");

        assertThat(save.preamble()).contains("A test preamble.");
        assertThat(save.plots()).isNotEmpty();
    }

    @Test
    void loadSaveFromGdlFile() throws Exception {
        GameSave save = RuntimeLoader.loadSave("src/test/resources/games/gdl-demo/game.gdl");

        assertThat(save.plots()).isNotEmpty();
    }

    @Test
    void loadSaveFromClasspathGdl() throws Exception {
        GameSave save = RuntimeLoader.loadSave("games/gdl-demo/game.gdl");

        assertThat(save.plots()).isNotEmpty();
    }

    @Test
    void loadBackstoryReadsFilesystemNarrative() {
        String backstory = RuntimeLoader.loadBackstory("src/test/resources/games/island-adventure-test/game.yaml");

        assertThat(backstory).contains("Wreck Beach");
    }

    @Test
    void loadBackstoryFailsWhenMissing() throws Exception {
        Path gameFile = tempDir.resolve("game.yaml");
        Files.writeString(gameFile, "seed: 1\nstartPlotKey: start\n", StandardCharsets.UTF_8);

        assertThatThrownBy(() -> RuntimeLoader.loadBackstory(gameFile.toString()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing backstory file");
    }

    @Test
    void loadCraftingRecipesReadsYaml() {
        Map<String, CraftingRecipe> recipes = RuntimeLoader.loadCraftingRecipes(
                "src/test/resources/games/island-adventure-test/game.yaml"
        );

        assertThat(recipes).containsKey("TORCH");
    }

    @Test
    void loadVerbAliasesReadsYaml() {
        Map<String, TokenType> aliases = RuntimeLoader.loadVerbAliases(
                "src/test/resources/games/island-adventure-test/game.yaml"
        );

        assertThat(aliases).containsKey("EXAMINE");
    }

    @Test
    void loadLoopConfigReadsYaml() {
        LoopConfig config = RuntimeLoader.loadLoopConfig("src/test/resources/games/island-adventure-test/game.yaml");

        assertThat(config.enabled()).isTrue();
        assertThat(config.maxTicks()).isEqualTo(1440);
    }

    @Test
    void loadLoopConfigDefaultsWhenMissing() {
        LoopConfig config = RuntimeLoader.loadLoopConfig("  ");

        assertThat(config.enabled()).isFalse();
        assertThat(config.maxTicks()).isZero();
    }

    @Test
    void loadTriggerDefinitionsReadsYaml() {
        List<TriggerDefinition> triggers = RuntimeLoader.loadTriggerDefinitions(
                "src/test/resources/games/island-adventure-test/game.yaml"
        );

        assertThat(triggers).isNotEmpty();
    }

    @Test
    void loadSmartActorSpecsReadsYaml() {
        List<SmartActorSpec> specs = RuntimeLoader.loadSmartActorSpecs(
                "src/test/resources/minigames/combat-sim-warded/mini.yaml"
        );

        assertThat(specs).isNotEmpty();
        assertThat(specs.get(0).actorKey()).isEqualTo("wraith");
    }

    @Test
    void loadSmartActorTagsReadsYaml() {
        SmartActorTagIndex tags = RuntimeLoader.loadSmartActorTags(
                "src/main/resources/games/mansion/game.yaml"
        );

        assertThat(tags.tagsForQuestKey("coldwater-case")).contains("QUEST_MAIN");
    }

    @Test
    void loadResourcesFromClasspath() {
        String resourcePath = "games/island/game.yaml";

        assertThat(RuntimeLoader.loadCraftingRecipes(resourcePath)).containsKey("TORCH");
        assertThat(RuntimeLoader.loadVerbAliases(resourcePath)).containsKey("EXAMINE");
        assertThat(RuntimeLoader.loadLoopConfig(resourcePath).enabled()).isTrue();
        assertThat(RuntimeLoader.loadTriggerDefinitions(resourcePath)).isNotEmpty();
    }

    @Test
    void loadSmartActorSpecsFromClasspath() {
        List<SmartActorSpec> specs = RuntimeLoader.loadSmartActorSpecs(
                "games/mansion/game.yaml"
        );

        assertThat(specs).isNotEmpty();
    }

    @Test
    void loadSmartActorTagsFromClasspath() {
        SmartActorTagIndex tags = RuntimeLoader.loadSmartActorTags(
                "games/mansion/game.yaml"
        );

        assertThat(tags.tagsForQuestKey("coldwater-case")).contains("QUEST_MAIN");
    }

    @Test
    void loadSaveThrowsWhenResourceMissing() {
        assertThatThrownBy(() -> RuntimeLoader.loadSave("missing-game.yaml"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void loadDefaultsWhenResourceBlank() {
        assertThat(RuntimeLoader.loadCraftingRecipes("")).isEmpty();
        assertThat(RuntimeLoader.loadVerbAliases(null)).isEmpty();
        assertThat(RuntimeLoader.loadTriggerDefinitions("  ")).isEmpty();
        assertThat(RuntimeLoader.loadSmartActorSpecs("")).isEmpty();
        assertThat(RuntimeLoader.loadSmartActorTags(" ").tagsForQuestKey("x")).isEmpty();
    }

    @Test
    void privateHelpersResolveFallbacks() throws Exception {
        Path root = tempDir.resolve("resolve");
        Files.createDirectories(root);
        Path fallback = root.resolve("data.yaml");
        Files.writeString(fallback, "key: value\n", StandardCharsets.UTF_8);

        Path resolved = (Path) invokePrivate(
                "resolveGameFile",
                new Class<?>[]{Path.class, String.class, String.class},
                root,
                "world",
                "data.yaml"
        );
        assertThat(resolved).isEqualTo(fallback);

        Path resolvedRoot = (Path) invokePrivate(
                "resolveGameFile",
                new Class<?>[]{Path.class, String.class, String.class},
                root,
                "",
                "data.yaml"
        );
        assertThat(resolvedRoot).isEqualTo(fallback);

        Object missing = invokePrivate(
                "resolveGameFile",
                new Class<?>[]{Path.class, String.class, String.class},
                null,
                "world",
                "data.yaml"
        );
        assertThat(missing).isNull();

        String[] candidates = (String[]) invokePrivate(
                "classpathCandidates",
                new Class<?>[]{String.class, String.class, String.class},
                "game.yaml",
                "",
                "crafting.yaml"
        );
        assertThat(candidates).isNotEmpty();
        assertThat(Arrays.asList(candidates)).anyMatch(value -> value.endsWith("crafting.yaml"));

        String[] trimmed = (String[]) invokePrivate(
                "classpathCandidates",
                new Class<?>[]{String.class, String.class, String.class},
                "src/main/resources/games/island/game.yaml",
                "world",
                "crafting.yaml"
        );
        assertThat(trimmed).isNotEmpty();

        Object stream = invokePrivate("openClasspathResource", new Class<?>[]{String[].class}, (Object) new String[0]);
        assertThat(stream).isNull();
    }

    private Object invokePrivate(String methodName, Class<?>[] types, Object... args) throws Exception {
        try {
            var method = RuntimeLoader.class.getDeclaredMethod(methodName, types);
            method.setAccessible(true);
            return method.invoke(null, args);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception causeException) {
                throw causeException;
            }
            throw ex;
        }
    }
}

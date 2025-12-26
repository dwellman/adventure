package com.demo.adventure.engine.runtime;

import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.engine.cli.GameCli;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.test.ConsoleCaptureExtension;
import com.demo.adventure.test.PlaybookSupport;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class MiniGamePlaybookIntegrationTest {

    private static final String PROMPT_TOKEN = "\n_ ";

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @ParameterizedTest
    @MethodSource("playbooks")
    void runsMiniGamePlaybook(PlaybookSupport.Playbook playbook) throws Exception {
        GameSave save = loadGame(playbook);
        String output = runPlaybook(playbook, save);

        assertThat(output).doesNotContain("Unknown command");
        assertThat(output).doesNotContain("Invalid command");

        List<String> segments = splitByPrompt(output);
        int available = segments.size();
        int required = playbook.steps().size();
        if (available < required && !playbook.allowEarlyExit()) {
            throw new IllegalStateException("Playbook ended early: " + playbook.name());
        }
        int checkCount = Math.min(available, required);
        for (int i = 0; i < checkCount; i++) {
            PlaybookSupport.Step step = playbook.steps().get(i);
            String segment = segments.get(i);
            for (String expected : step.expectContains()) {
                assertThat(segment).as("step " + (i + 1) + " (" + step.command() + ")").contains(expected);
            }
        }
    }

    private static Stream<PlaybookSupport.Playbook> playbooks() {
        return Stream.of(
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/two-plot/playbook.yaml"),
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/cave-walkthrough/playbook.yaml"),
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/dungeon-adventure/playbook.yaml"),
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/combat-sim/playbook.yaml"),
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/combat-sim-warded/playbook.yaml")
        );
    }

    private static InputStream openResource(String resource) throws Exception {
        Path path = Path.of(resource);
        if (Files.exists(path)) {
            return Files.newInputStream(path);
        }
        return MiniGamePlaybookIntegrationTest.class.getClassLoader().getResourceAsStream(resource);
    }

    private static GameSave loadGame(PlaybookSupport.Playbook playbook) throws Exception {
        Path path = Path.of(playbook.gameResource());
        if (Files.exists(path)) {
            return GameSaveYamlLoader.load(path);
        }
        try (InputStream in = openResource(playbook.gameResource())) {
            if (in == null) {
                throw new IllegalStateException("Missing game resource: " + playbook.gameResource());
            }
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return GameSaveYamlLoader.load(yaml);
        }
    }

    private String runPlaybook(PlaybookSupport.Playbook playbook, GameSave save) throws Exception {
        List<String> commands = playbook.steps().stream()
                .map(PlaybookSupport.Step::command)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        if (!playbook.allowEarlyExit() && commands.stream().noneMatch(MiniGamePlaybookIntegrationTest::isQuitCommand)) {
            throw new IllegalStateException("Playbook must include a quit command: " + playbook.name());
        }
        String input = String.join("\n", commands) + "\n";

        GameCli cli = new GameCli();
        Object option = buildGameOption(playbook.name(), playbook.gameResource());
        Method walkabout = resolveWalkabout(option.getClass());

        KeyExpressionEvaluator.DiceRoller previous = KeyExpressionEvaluator.getDefaultDiceRoller();
        try (Scanner scanner = new Scanner(new StringReader(input))) {
            KeyExpressionEvaluator.setDefaultDiceRoller(sides -> sides);
            console.reset();
            invokeWalkabout(walkabout, cli, option, save, scanner);
        } finally {
            KeyExpressionEvaluator.setDefaultDiceRoller(previous);
        }
        return console.output();
    }

    private static List<String> splitByPrompt(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        String normalized = output.replace("\r\n", "\n");
        String[] parts = normalized.split(java.util.regex.Pattern.quote(PROMPT_TOKEN), -1);
        List<String> segments = new ArrayList<>();
        for (int i = 1; i < parts.length; i++) {
            segments.add(parts[i]);
        }
        return segments;
    }

    private static Object buildGameOption(String name, String resource) throws Exception {
        Class<?> optionClass = Class.forName("com.demo.adventure.engine.cli.GameCli$GameOption");
        Constructor<?> ctor = optionClass.getDeclaredConstructor(int.class, String.class, String.class, String.class, boolean.class);
        ctor.setAccessible(true);
        return ctor.newInstance(0, name, resource, "playbook", false);
    }

    private static Method resolveWalkabout(Class<?> optionClass) throws Exception {
        Method method = GameCli.class.getDeclaredMethod("walkabout", optionClass, GameSave.class, Scanner.class);
        method.setAccessible(true);
        return method;
    }

    private static void invokeWalkabout(Method walkabout, GameCli cli, Object option, GameSave save, Scanner scanner) throws Exception {
        try {
            walkabout.invoke(cli, option, save, scanner);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception causeException) {
                throw causeException;
            }
            throw ex;
        }
    }

    private static boolean isQuitCommand(String command) {
        if (command == null) {
            return false;
        }
        String trimmed = command.trim();
        return trimmed.equalsIgnoreCase("quit") || trimmed.equalsIgnoreCase("q");
    }
}

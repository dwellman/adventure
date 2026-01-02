package com.demo.adventure.test;

import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.engine.cli.GameCli;
import com.demo.adventure.engine.cli.RuntimeLoader;
import com.demo.adventure.test.PlaybookSupport.Playbook;
import com.demo.adventure.test.PlaybookSupport.Step;
import org.assertj.core.api.Assertions;

import java.io.StringReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

public final class AdventurePlaybookSupport {

    private static final String PROMPT_TOKEN = "\n_ ";

    private AdventurePlaybookSupport() {
    }

    public static GameSave loadGame(Playbook playbook) throws Exception {
        return RuntimeLoader.loadSave(playbook.gameResource());
    }

    public static String runPlaybook(Playbook playbook, GameSave save, ConsoleCaptureExtension console) throws Exception {
        List<String> commands = playbook.steps().stream()
                .map(Step::command)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        if (!playbook.allowEarlyExit()
                && requiresQuit(playbook)
                && commands.stream().noneMatch(AdventurePlaybookSupport::isQuitCommand)) {
            throw new IllegalStateException("Playbook must include a quit command: " + playbook.name());
        }
        String input = String.join("\n", commands) + "\n";

        GameCli cli = new GameCli();
        Object option = buildGameOption(playbook.name(), playbook.gameResource());
        Method walkabout = resolveWalkabout(option.getClass());

        try (Scanner scanner = new Scanner(new StringReader(input))) {
            console.reset();
            invokeWalkabout(walkabout, cli, option, save, scanner);
        }
        return console.output();
    }

    public static void assertPlaybookOutput(String output, Playbook playbook) {
        Assertions.assertThat(output).doesNotContain("Unknown command");
        Assertions.assertThat(output).doesNotContain("Invalid command");

        List<String> segments = splitByPrompt(output);
        int available = segments.size();
        int required = playbook.steps().size();
        if (available < required && !playbook.allowEarlyExit()) {
            throw new IllegalStateException("Playbook ended early: " + playbook.name());
        }
        int checkCount = Math.min(available, required);
        for (int i = 0; i < checkCount; i++) {
            Step step = playbook.steps().get(i);
            String segment = segments.get(i);
            for (String expected : step.expectContains()) {
                Assertions.assertThat(segment)
                        .as("step " + (i + 1) + " (" + step.command() + ")")
                        .contains(expected);
            }
        }
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
        Class<?> optionClass = Class.forName("com.demo.adventure.engine.cli.GameCatalogEntry");
        Constructor<?> ctor = optionClass.getDeclaredConstructor(
                String.class,
                int.class,
                String.class,
                String.class,
                String.class,
                boolean.class
        );
        ctor.setAccessible(true);
        return ctor.newInstance("playbook", 0, name, resource, "playbook", false);
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

    private static boolean requiresQuit(Playbook playbook) {
        if (playbook == null) {
            return true;
        }
        String outcome = playbook.outcome();
        if (outcome == null || outcome.isBlank()) {
            return true;
        }
        String normalized = outcome.trim().toLowerCase(Locale.ROOT);
        return !(normalized.equals("win")
                || normalized.equals("loss")
                || normalized.equals("lose")
                || normalized.equals("defeat"));
    }
}

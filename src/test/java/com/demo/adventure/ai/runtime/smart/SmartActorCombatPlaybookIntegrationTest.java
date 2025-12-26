package com.demo.adventure.ai.runtime.smart;

import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandOutput;
import com.demo.adventure.engine.command.handlers.CommandHandlers;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.engine.runtime.SceneNarrator;
import com.demo.adventure.engine.runtime.SmartActorRuntime;
import com.demo.adventure.test.ConsoleCaptureExtension;
import com.demo.adventure.test.PlaybookSupport;
import com.demo.adventure.test.PlaybookSupport.DecisionSpec;
import com.demo.adventure.test.PlaybookSupport.Playbook;
import com.demo.adventure.test.PlaybookSupport.Step;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorCombatPlaybookIntegrationTest {

    private record RunResult(List<String> segments, Map<String, List<DecisionSpec>> actualDecisions) {
    }

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @ParameterizedTest
    @MethodSource("playbooks")
    void runsSmartActorCombatPlaybook(Playbook playbook) throws Exception {
        GameSave save = loadGame(playbook);
        RunResult result = runPlaybook(playbook, save);
        List<String> segments = result.segments();

        if (segments.size() < playbook.steps().size() && !playbook.allowEarlyExit()) {
            throw new IllegalStateException("Playbook ended early: " + playbook.name());
        }
        int checkCount = Math.min(segments.size(), playbook.steps().size());
        for (int i = 0; i < checkCount; i++) {
            Step step = playbook.steps().get(i);
            String segment = segments.get(i);
            for (String expected : step.expectContains()) {
                assertThat(segment).as("step " + (i + 1) + " (" + step.command() + ")").contains(expected);
            }
        }
        if (!playbook.smartActorExpectations().isEmpty()) {
            for (Map.Entry<String, List<DecisionSpec>> entry : playbook.smartActorExpectations().entrySet()) {
                List<DecisionSpec> actual = result.actualDecisions().getOrDefault(entry.getKey(), List.of());
                assertThat(actual)
                        .as("smart actor decisions for " + entry.getKey())
                        .containsExactlyElementsOf(entry.getValue());
            }
        }
    }

    private static Stream<Playbook> playbooks() {
        return Stream.of(
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/combat-sim-smart-actor/playbook.yaml"),
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/combat-sim-smart-actor/playbook-flee.yaml"),
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/combat-sim/playbook-smart-actor.yaml"),
                PlaybookSupport.loadPlaybook("src/test/resources/minigames/combat-sim-warded/playbook-smart-actor.yaml")
        );
    }

    private static GameSave loadGame(Playbook playbook) throws Exception {
        Path path = Path.of(playbook.gameResource());
        if (Files.exists(path)) {
            return GameSaveYamlLoader.load(path);
        }
        try (InputStream in = PlaybookSupport.openResource(playbook.gameResource())) {
            if (in == null) {
                throw new IllegalStateException("Missing game resource: " + playbook.gameResource());
            }
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return GameSaveYamlLoader.load(yaml);
        }
    }

    private RunResult runPlaybook(Playbook playbook, GameSave save) throws Exception {
        List<String> commands = playbook.steps().stream()
                .map(Step::command)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();
        if (!playbook.allowEarlyExit() && commands.stream().noneMatch(SmartActorCombatPlaybookIntegrationTest::isQuitCommand)) {
            throw new IllegalStateException("Playbook must include a quit command: " + playbook.name());
        }

        LoopRuntime loopRuntime = new LoopRuntime(save, LoopConfig.disabled());
        WorldBuildResult world = loopRuntime.buildWorld();
        KernelRegistry registry = world.registry();
        UUID currentPlot = world.startPlotId();

        SceneNarrator narrator = new SceneNarrator(new NarrationService(false, null, false));
        GameRuntime runtime = new GameRuntime(narrator, text -> System.out.println(text), false);

        UUID playerId = findActorIdByLabel(registry, "Ranger");
        if (playerId == null) {
            playerId = runtime.findPlayerActor(registry, currentPlot);
        }
        List<Item> inventory = new ArrayList<>(runtime.startingInventory(registry, playerId));
        Map<UUID, Map<UUID, Rectangle2D>> inventoryPlacements = new HashMap<>();
        runtime.seedInventoryPlacements(inventory, inventoryPlacements);

        runtime.configure(
                registry,
                currentPlot,
                playerId,
                inventory,
                inventoryPlacements,
                loopRuntime,
                new TriggerEngine(List.of()),
                Map.of(),
                Map.of()
        );

        Map<String, List<DecisionSpec>> actualDecisions = new HashMap<>();
        SmartActorRuntime smartActorRuntime = buildSmartActors(
                runtime,
                playbook.gameResource(),
                playbook.smartActorDecisions(),
                actualDecisions
        );
        runtime.configureSmartActors(smartActorRuntime);

        CommandInterpreter interpreter = new CommandInterpreter();
        Map<com.demo.adventure.engine.command.CommandAction, GameCommandHandler> handlers = CommandHandlers.defaultHandlers();
        CommandOutput output = new CommandOutput() {
            @Override
            public void emit(String text) {
                System.out.println(text);
            }

            @Override
            public void printHelp() {
                System.out.println("Help is not available in this test.");
            }
        };
        CommandContext context = new CommandContext(output, runtime);

        KeyExpressionEvaluator.DiceRoller previous = KeyExpressionEvaluator.getDefaultDiceRoller();
        List<String> segments = new ArrayList<>();
        int lastIndex = 0;
        try {
            KeyExpressionEvaluator.setDefaultDiceRoller(sides -> sides);
            console.reset();
            for (String commandText : commands) {
                narrator.setLastUtterance(commandText);
                if (commandText.isEmpty()) {
                    segments.add("");
                    continue;
                }
                Command cmd = interpreter.interpret(commandText);
                narrator.setLastCommand(commandText);
                GameCommandHandler handler = handlers.get(cmd.action());
                if (handler == null) {
                    throw new IllegalStateException("Missing handler for command: " + cmd.action());
                }
                CommandOutcome outcome = handler.handle(context, cmd);
                if (!outcome.skipTurnAdvance() && !outcome.endGame()) {
                    runtime.advanceTurn();
                }
                String full = console.output();
                String segment = full.substring(lastIndex);
                segments.add(segment);
                lastIndex = full.length();
                if (outcome.endGame()) {
                    break;
                }
            }
        } finally {
            KeyExpressionEvaluator.setDefaultDiceRoller(previous);
        }
        return new RunResult(segments, actualDecisions);
    }

    private static SmartActorRuntime buildSmartActors(GameRuntime runtime,
                                                      String gameResource,
                                                      Map<String, List<DecisionSpec>> decisions,
                                                      Map<String, List<DecisionSpec>> actualDecisions) throws Exception {
        Path gamePath = Path.of(gameResource);
        Path smartActorsPath = gamePath.getParent().resolve("world").resolve("smart-actors.yaml");
        List<SmartActorSpec> specs = SmartActorSpecLoader.load(smartActorsPath);
        SmartActorRegistry registry = SmartActorRegistry.create(runtime.registry(), specs);
        SmartActorTagIndex tags = SmartActorTagIndex.empty();

        Map<String, Deque<DecisionSpec>> decisionQueue = new HashMap<>();
        if (decisions != null) {
            for (Map.Entry<String, List<DecisionSpec>> entry : decisions.entrySet()) {
                decisionQueue.put(entry.getKey(), new ArrayDeque<>(entry.getValue()));
            }
        }

        SmartActorPlanner planner = new SmartActorPlanner(true, "test", false, (apiKey, systemPrompt, userPrompt, debug) -> {
            String actorKey = actorKeyFromPrompt(userPrompt);
            Deque<DecisionSpec> queue = decisionQueue.get(actorKey);
            DecisionSpec decision = queue == null ? null : queue.pollFirst();
            if (decision == null) {
                decision = DecisionSpec.fallbackLook();
            }
            if (actorKey != null && !actorKey.isBlank()) {
                actualDecisions.computeIfAbsent(actorKey, key -> new ArrayList<>()).add(decision);
            }
            return decision.toJson();
        });

        return new SmartActorRuntime(
                registry,
                tags,
                planner,
                new com.demo.adventure.ai.runtime.TranslatorService(false, null),
                new CommandInterpreter(),
                CommandHandlers.defaultHandlers(),
                false
        );
    }

    private static boolean isQuitCommand(String command) {
        if (command == null) {
            return false;
        }
        String trimmed = command.trim();
        return trimmed.equalsIgnoreCase("quit") || trimmed.equalsIgnoreCase("q");
    }

    private static UUID findActorIdByLabel(KernelRegistry registry, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        for (Object entry : registry.getEverything().values()) {
            if (entry instanceof Actor actor) {
                if (actor.getLabel() != null && actor.getLabel().equalsIgnoreCase(label.trim())) {
                    return actor.getId();
                }
            }
        }
        return null;
    }

    private static String actorKeyFromPrompt(String userPrompt) {
        if (userPrompt == null || userPrompt.isBlank()) {
            return "";
        }
        String[] lines = userPrompt.split("\\R", -1);
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.startsWith("actorKey:")) {
                return trimmed.substring("actorKey:".length()).trim();
            }
        }
        return "";
    }
}

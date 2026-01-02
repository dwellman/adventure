package com.demo.adventure.engine.cli;

import com.demo.adventure.ai.runtime.TranslationOrchestrator;
import com.demo.adventure.ai.runtime.smart.SmartActorPlanner;
import com.demo.adventure.ai.runtime.smart.SmartActorRegistry;
import com.demo.adventure.ai.runtime.smart.SmartActorSpec;
import com.demo.adventure.ai.runtime.smart.SmartActorTagIndex;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.authoring.save.io.FootprintRule;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandParseError;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.handlers.ClassicCommandFallback;
import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.engine.runtime.InteractionState;
import com.demo.adventure.engine.runtime.InteractionType;
import com.demo.adventure.engine.runtime.MentionResolution;
import com.demo.adventure.engine.runtime.MentionResolutionType;
import com.demo.adventure.engine.runtime.SceneNarrator;
import com.demo.adventure.engine.runtime.SmartActorRuntime;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

final class GameSessionRunner {
    private final GameCli cli;
    private SceneNarrator narrator;
    private GameRuntime runtime;

    GameSessionRunner(GameCli cli) {
        this.cli = cli;
    }

    boolean run(GameCatalogEntry option, GameSave save, Scanner scanner) throws GameBuilderException {
        boolean returnToMenu = false;
        LoopConfig loopConfig = RuntimeLoader.loadLoopConfig(option.resource());
        List<FootprintRule> footprintRules = RuntimeLoader.loadFootprintRules(option.resource());
        LoopRuntime loopRuntime = new LoopRuntime(save, loopConfig, footprintRules);
        WorldBuildResult world = loopRuntime.buildWorld();
        KernelRegistry registry = world.registry();
        UUID currentPlot = world.startPlotId();

        narrator = new SceneNarrator(cli.narrationService());
        runtime = new GameRuntime(narrator, cli::emit, cli.aiEnabled());
        cli.setNarrator(narrator);
        cli.setRuntime(runtime);

        UUID playerId = runtime.findPlayerActor(registry, currentPlot);
        List<Item> inventory = new ArrayList<>(runtime.startingInventory(registry, playerId));
        Map<UUID, Map<UUID, Rectangle2D>> inventoryPlacements = new HashMap<>();
        runtime.seedInventoryPlacements(inventory, inventoryPlacements);

        cli.printBlankLine();
        cli.printlnLine("=== " + option.name() + " ===");
        cli.printBlankLine();
        if (save.preamble() != null && !save.preamble().isBlank()) {
            cli.printNarrationLine(save.preamble());
        }
        String backstory = RuntimeLoader.loadBackstory(option.resource());
        if (backstory != null && !backstory.isBlank()) {
            cli.printNarrationLine(backstory);
        }
        narrator.setBackstory(backstory);

        Map<String, CraftingRecipe> craftingRecipes = RuntimeLoader.loadCraftingRecipes(option.resource());
        Map<String, TokenType> extraAliases = RuntimeLoader.loadVerbAliases(option.resource());
        cli.commandInterpreter().setExtraKeywords(extraAliases);
        cli.setAliasMap(cli.mergeAliasMap(extraAliases));
        TriggerEngine triggerEngine = new TriggerEngine(RuntimeLoader.loadTriggerDefinitions(option.resource()));

        runtime.configure(
                registry,
                currentPlot,
                playerId,
                inventory,
                inventoryPlacements,
                loopRuntime,
                triggerEngine,
                craftingRecipes,
                extraAliases
        );
        runtime.primeScene();
        List<SmartActorSpec> smartActorSpecs = RuntimeLoader.loadSmartActorSpecs(option.resource());
        SmartActorTagIndex smartActorTags = RuntimeLoader.loadSmartActorTags(option.resource());
        if (cli.aiEnabled() && !smartActorSpecs.isEmpty()) {
            SmartActorRegistry smartActorRegistry = SmartActorRegistry.create(registry, smartActorSpecs);
            SmartActorPlanner planner = new SmartActorPlanner(cli.aiEnabled(), cli.apiKey(), cli.smartActorDebug());
            SmartActorRuntime smartActorRuntime = new SmartActorRuntime(
                    smartActorRegistry,
                    smartActorTags,
                    planner,
                    cli.translatorService(),
                    cli.commandInterpreter(),
                    cli.commandHandlers(),
                    cli.smartActorDebug()
            );
            smartActorRuntime.setLocalOnly(cli.smartActorLocalOnly());
            runtime.configureSmartActors(smartActorRuntime);
        }
        CommandContext context = new CommandContext(cli, runtime);

gameLoop:
        while (true) {
            System.out.print(cli.mode() == GameCli.GameMode.Z2025 ? "\n> " : "\n_ ");
            String line = scanner.nextLine();
            if (line == null) {
                return returnToMenu;
            }
            String input = line.trim();
            narrator.setLastUtterance(input);
            if (input.isEmpty()) {
                continue;
            }
            cli.printBlankLine();
            InteractionState interactionState = runtime.interactionState();
            if (interactionState.type() != InteractionType.NONE) {
                if (interactionState.type() == InteractionType.AWAITING_DICE) {
                    Command diceCommand = parseCommand(input);
                    if (diceCommand != null && !diceCommand.hasError() && diceCommand.action() == CommandAction.DICE) {
                        narrator.setLastCommand("dice");
                        runtime.rollDice(diceCommand.argument());
                    } else {
                        String prompt = interactionState.promptLine();
                        if (prompt == null || prompt.isBlank()) {
                            String expected = interactionState.expectedToken();
                            prompt = expected == null || expected.isBlank() ? "Roll dice." : "Roll " + expected + ".";
                        }
                        runtime.narrate(prompt);
                    }
                    continue;
                }
                runtime.narrate("Finish the current prompt before acting.");
                continue;
            }
            MentionParse mention = resolveMention(input);
            if (runtime.isConversationActive()) {
                if (isConversationExit(input)) {
                    narrator.setLastCommand("");
                    runtime.endConversation();
                    continue;
                }
                MentionHandling mentionHandling = handleMention(mention);
                if (mentionHandling == MentionHandling.END_GAME) {
                    return returnToMenu;
                }
                if (mentionHandling != MentionHandling.NOT_HANDLED) {
                    continue;
                }
                String actorLabel = runtime.conversationActorLabel();
                narrator.setLastCommand(actorLabel.isBlank() ? "talk" : "talk " + actorLabel);
                runtime.talkToConversation(input);
                CommandOutcome turnOutcome = runtime.advanceTurn();
                if (turnOutcome.endGame()) {
                    return returnToMenu;
                }
                if (turnOutcome.skipTurnAdvance()) {
                    continue;
                }
                continue;
            }

            MentionHandling mentionHandling = handleMention(mention);
            if (mentionHandling == MentionHandling.END_GAME) {
                return returnToMenu;
            }
            if (mentionHandling != MentionHandling.NOT_HANDLED) {
                continue;
            }

            String commandText = input;
            Command cmd = parseCommand(commandText);
            boolean localValid = isValidCommand(cmd);
            boolean translated = false;
            if (cli.aiEnabled()) {
                if (!localValid) {
                    List<String> fixtures = runtime.visibleFixtureLabels();
                    List<String> items = runtime.visibleItemLabels();
                    List<String> inventoryLabels = runtime.inventoryLabels();
                    TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                            cli.translatorService(),
                            input,
                            fixtures,
                            items,
                            inventoryLabels,
                            narrator.lastState(),
                            cli.translatorDebug(),
                            this::parseCommand,
                            System.out::println
                    );
                    if (outcome.type() == TranslationOrchestrator.OutcomeType.FAILED) {
                        cli.printlnLine("~ translator failed; please rephrase (try HELP or a direction).");
                        continue gameLoop;
                    }
                    if (outcome.type() == TranslationOrchestrator.OutcomeType.EMOTE) {
                        narrator.setLastCommand("");
                        runtime.emote(outcome.commandText());
                        continue gameLoop;
                    }
                    commandText = outcome.commandText();
                    translated = true;
                }
            } else {
                // Pattern: Trust UX
                // - When AI is disabled, allow classic fallback only if the compiler cannot parse the input.
                if (cmd.action() == CommandAction.UNKNOWN || cmd.hasError()) {
                    String fallbackCommandText = ClassicCommandFallback.resolve(input);
                    if (fallbackCommandText != null && !fallbackCommandText.isBlank()) {
                        commandText = fallbackCommandText;
                    }
                }
            }

            cmd = parseCommand(commandText);
            narrator.setLastCommand(commandText);
            if (cmd.hasError()) {
                if (translated) {
                    cli.printlnLine("~ translator failed; please rephrase (try HELP or a direction).");
                    continue gameLoop;
                }
                runtime.narrate(formatCommandError(cmd.error()));
                continue;
            }
            if (cmd.action() == CommandAction.UNKNOWN) {
                if (translated) {
                    cli.printlnLine("~ translator failed; please rephrase (try HELP or a direction).");
                    continue;
                }
                runtime.narrate("Unknown command. Type help for commands.");
                continue;
            }
            GameCommandHandler handler = cli.commandHandlers().get(cmd.action());
            if (handler == null) {
                runtime.narrate("Unknown command. Type help for commands.");
                continue;
            }
            if (cmd.action() == CommandAction.QUIT) {
                returnToMenu = true;
            }
            CommandOutcome outcome = handler.handle(context, cmd);
            if (outcome.endGame()) {
                return returnToMenu;
            }
            if (outcome.skipTurnAdvance()) {
                continue gameLoop;
            }

            CommandOutcome turnOutcome = runtime.advanceTurn();
            if (turnOutcome.endGame()) {
                return returnToMenu;
            }
            if (turnOutcome.skipTurnAdvance()) {
                continue gameLoop;
            }
        }
    }

    Command parseCommand(String input) {
        return cli.commandInterpreter().interpret(input);
    }

    boolean isValidCommand(Command command) {
        return command != null && command.action() != CommandAction.UNKNOWN && !command.hasError();
    }

    String formatCommandError(CommandParseError error) {
        if (error == null) {
            return "Invalid command.";
        }
        String suffix = error.column() >= 0 ? " (col " + (error.column() + 1) + ")" : "";
        return "Invalid command: " + error.message() + suffix;
    }

    boolean isConversationExit(String input) {
        List<Token> tokens = CommandScanner.scan(input);
        List<String> words = new ArrayList<>();
        for (Token token : tokens) {
            if (token == null || token.type == TokenType.EOL || token.type == TokenType.HELP) {
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim();
            if (lexeme.isEmpty()) {
                continue;
            }
            if (token.type == TokenType.STRING) {
                for (String part : lexeme.split("\\s+")) {
                    if (!part.isBlank()) {
                        words.add(part);
                    }
                }
                continue;
            }
            words.add(lexeme);
        }
        if (words.size() != 2) {
            return false;
        }
        return "okay".equalsIgnoreCase(words.get(0)) && "bye".equalsIgnoreCase(words.get(1));
    }

    enum MentionParseType {
        NONE,
        INVALID,
        AMBIGUOUS,
        UNKNOWN,
        MATCH
    }

    record MentionParse(MentionParseType type, String actorLabel, String utterance) {
        static MentionParse none() {
            return new MentionParse(MentionParseType.NONE, "", "");
        }
    }

    private enum MentionHandling {
        NOT_HANDLED,
        CONTINUE,
        END_GAME
    }

    MentionParse resolveMention(String input) {
        if (runtime == null || input == null || input.isBlank()) {
            return MentionParse.none();
        }
        List<Token> tokens = CommandScanner.scan(input);
        int mentionIndex = findMentionToken(tokens);
        if (mentionIndex < 0) {
            return MentionParse.none();
        }
        List<String> beforeWords = collectWords(tokens, 0, mentionIndex);
        List<String> afterWords = collectWords(tokens, mentionIndex + 1, tokens.size());
        if (afterWords.isEmpty()) {
            return new MentionParse(MentionParseType.INVALID, "", "");
        }
        MentionResolution resolution = runtime.resolveMentionActor(afterWords);
        if (resolution.type() == MentionResolutionType.NONE) {
            return new MentionParse(MentionParseType.UNKNOWN, "", "");
        }
        if (resolution.type() == MentionResolutionType.AMBIGUOUS) {
            return new MentionParse(MentionParseType.AMBIGUOUS, "", "");
        }
        String actorLabel = resolution.actorLabel() == null ? "" : resolution.actorLabel().trim();
        List<String> utteranceTokens = new ArrayList<>(beforeWords);
        int consumed = Math.max(0, resolution.tokensMatched());
        if (consumed < afterWords.size()) {
            utteranceTokens.addAll(afterWords.subList(consumed, afterWords.size()));
        }
        String utterance = String.join(" ", utteranceTokens).trim();
        return new MentionParse(MentionParseType.MATCH, actorLabel, utterance);
    }

    private MentionHandling handleMention(MentionParse mention) throws GameBuilderException {
        if (mention == null || mention.type() == MentionParseType.NONE) {
            return MentionHandling.NOT_HANDLED;
        }
        switch (mention.type()) {
            case INVALID -> {
                runtime.narrate("Talk to whom?");
                return MentionHandling.CONTINUE;
            }
            case AMBIGUOUS -> {
                runtime.narrate("Be specific.");
                return MentionHandling.CONTINUE;
            }
            case UNKNOWN -> {
                runtime.narrate("You don't see anyone by that name.");
                return MentionHandling.CONTINUE;
            }
            case MATCH -> {
                String label = mention.actorLabel() == null ? "" : mention.actorLabel().trim();
                if (label.isBlank()) {
                    runtime.narrate("No one answers.");
                    return MentionHandling.CONTINUE;
                }
                narrator.setLastCommand("talk " + label);
                runtime.talk(label);
                if (!mention.utterance().isBlank()) {
                    runtime.talkToConversation(mention.utterance());
                }
                CommandOutcome turnOutcome = runtime.advanceTurn();
                if (turnOutcome.endGame()) {
                    return MentionHandling.END_GAME;
                }
                return MentionHandling.CONTINUE;
            }
            default -> {
                return MentionHandling.NOT_HANDLED;
            }
        }
    }

    int findMentionToken(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token != null && token.type == TokenType.TALK) {
                return i;
            }
        }
        return -1;
    }

    List<String> collectWords(List<Token> tokens, int startIdx, int endIdx) {
        if (tokens == null || tokens.isEmpty() || startIdx >= endIdx) {
            return List.of();
        }
        int safeEnd = Math.min(tokens.size(), endIdx);
        List<String> words = new ArrayList<>();
        for (int i = Math.max(0, startIdx); i < safeEnd; i++) {
            Token token = tokens.get(i);
            if (token == null || token.type == TokenType.EOL || token.type == TokenType.HELP || token.type == TokenType.TALK) {
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim();
            if (lexeme.isEmpty()) {
                continue;
            }
            if (token.type == TokenType.STRING) {
                for (String part : lexeme.split("\\s+")) {
                    if (!part.isBlank()) {
                        words.add(part);
                    }
                }
                continue;
            }
            words.add(lexeme);
        }
        return words;
    }
}

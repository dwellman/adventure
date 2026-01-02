package com.demo.adventure.engine.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.buui.BuuiMenu;
import com.demo.adventure.ai.runtime.AiConfig;
import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.ai.runtime.TranslationOrchestrator;
import com.demo.adventure.ai.runtime.TranslatorService;
import com.demo.adventure.ai.runtime.smart.SmartActorPlanner;
import com.demo.adventure.ai.runtime.smart.SmartActorRegistry;
import com.demo.adventure.ai.runtime.smart.SmartActorSpec;
import com.demo.adventure.ai.runtime.smart.SmartActorTagIndex;
import com.demo.adventure.engine.command.CommandOutput;
import com.demo.adventure.engine.command.handlers.ClassicCommandFallback;
import com.demo.adventure.engine.command.handlers.CommandHandlers;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandParseError;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.VerbAliases;
import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.authoring.save.io.FootprintRule;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.runtime.SceneNarrator;
import com.demo.adventure.engine.runtime.SmartActorRuntime;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

/**
 * Minimal CLI launcher to pick a game and print its preamble.
 */
public final class GameCli extends BuuiConsole implements CommandOutput {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-00000000feed");

    private static final String HELP_TEXT = """
Commands:
- **move (go) <direction>** - Move (N,S,E,W,NE,NW,SE,SW,UP,DOWN)
- **n|s|e|w|u|d|ne|nw|se|sw** - Shortcut movement
- **look (l)** - Describe the current plot
- **look <thing>** - Inspect an item/fixture/actor you can see or carry
- **inspect <thing>** - Look closely at an item/fixture/actor
- **listen** - Quick re-read of the current scene (alias of look)
- **take <item>** - Pick up a visible item here
- **drop <item>** - Drop an item from your inventory
- **put <item>** - Drop an item from your inventory
- **open <thing>** - Try opening a visible item or gate
- **use <thing>** - Try using an item
- **attack <target>** - Attack a visible actor
- **flee/run away** - Try to escape combat
- **talk <actor>** - Start a conversation with a visible actor
- **@<actor>** - Start/switch conversation; say "okay, bye" to end
- **inventory (i)** - Show what you're carrying
- **craft <item>** - Craft an item if you have the ingredients
- **how craft <item>** - Show required skill and ingredients
- **search/explore** - Poke around for hidden items
- **dice(<sides>,<target>)** - Roll a check when prompted (example: dice(20,15))
- **help (h, ?)** - Show this help
- **quit (q)** - Return to the main menu
""";

    private static final String GAME_CATALOG_PATH = "src/main/resources/games/index.yaml";

    public static void main(String[] args) {
        // Pattern: Trust UX
        // - Keep player output clean by suppressing debug noise unless explicitly enabled for tests.
        KeyExpressionEvaluator.setDebugOutput(false);
        GameMode mode = GameMode.fromArgs(args);
        new GameCli(mode).run();
    }

    private enum GameMode { Z1980, Z2025;
        static GameMode fromArgs(String[] args) {
            if (args == null) {
                return Z1980;
            }
            for (String a : args) {
                if ("--mode=2025".equalsIgnoreCase(a) || "--mode=Z2025".equalsIgnoreCase(a)) {
                    return Z2025;
                }
                if ("--mode=1980".equalsIgnoreCase(a) || "--mode=Z1980".equalsIgnoreCase(a)) {
                    return Z1980;
                }
            }
            return Z1980;
        }
    }

    private final GameMode mode;
    private final List<GameCatalogEntry> gameOptions;

    public GameCli() {
        this(GameMode.Z1980);
    }

    private final String apiKey;
    private final boolean aiEnabled;
    private final boolean translatorDebug;
    private final boolean smartActorDebug;
    private final boolean smartActorLocalOnly;
    private final NarrationService narrationService;
    private final TranslatorService translatorService;
    private final CommandInterpreter commandInterpreter = new CommandInterpreter();
    private final Map<CommandAction, GameCommandHandler> commandHandlers = CommandHandlers.defaultHandlers();
    private Map<String, String> aliasMap = VerbAliases.aliasMap();
    private SceneNarrator narrator;
    private GameRuntime runtime;
    private boolean returnToMenu;

    public GameCli(GameMode mode) {
        // Keep player output clean even when GameCli is constructed directly (tests bypass main()).
        KeyExpressionEvaluator.setDebugOutput(false);
        this.mode = mode == null ? GameMode.Z1980 : mode;
        this.gameOptions = loadGameOptions();
        this.apiKey = resolveApiKey();
        AiConfig config = AiConfig.load();
        this.aiEnabled = this.mode == GameMode.Z2025 && this.apiKey != null && !this.apiKey.isBlank();
        this.translatorDebug = config.getBoolean("ai.translator.debug", false);
        this.smartActorDebug = config.getBoolean("ai.smart_actor.debug", false);
        this.smartActorLocalOnly = isSmartActorLocalOnly(config);
        this.narrationService = new NarrationService(
                aiEnabled,
                apiKey,
                config.getBoolean("ai.narrator.debug", false)
        );
        this.translatorService = new TranslatorService(aiEnabled, apiKey);
        if (!aiEnabled) {
            println("~ AI disabled (mode=" + this.mode + ", apiKey=" + (apiKey == null ? "missing" : "present") + ")");
        } else {
            println("~ AI enabled (mode=2025)");
        }
    }

    private List<GameCatalogEntry> loadGameOptions() {
        try {
            List<GameCatalogEntry> options = GameCatalogLoader.load(GAME_CATALOG_PATH);
            if (options == null || options.isEmpty()) {
                throw new IllegalStateException("No games listed in " + GAME_CATALOG_PATH);
            }
            return options;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to load game catalog: " + ex.getMessage(), ex);
        }
    }

    private void run() {
        printMenu();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                long visibleCount = gameOptions.stream().filter(o -> !o.hidden()).count();
                System.out.print(BuuiMenu.prompt("game", (int) visibleCount, "q"));
                String input = scanner.nextLine().trim();
                if (input.isBlank()) {
                    continue;
                }
                if (input.equalsIgnoreCase(
                        "q") ||
                        input.equalsIgnoreCase("quit") ||
                        input.equalsIgnoreCase("exit")
                ) {
                    println("Goodbye.");
                    return;
                }
                GameCatalogEntry selected = parseSelection(input);
                if (selected == null) {
                    println("Unknown selection: " + input);
                    continue;
                }
                try {
                    GameSave save = RuntimeLoader.loadSave(selected.resource());
                    walkabout(selected, save, scanner);
                    if (returnToMenu) {
                        continue;
                    }
                    return;
                } catch (Exception ex) {
                    println("Failed to load game: " + ex.getMessage());
                }
            }
        }
    }

    private void printMenu() {
        String title = mode == GameMode.Z2025 ? "BUUI Adventure (AI)" : "BUUI Adventure (1980)";
        BuuiMenu.Builder builder = BuuiMenu.builder()
                .title(title)
                .itemHeader("Game")
                .descriptionHeader("Description");
        for (GameCatalogEntry option : gameOptions) {
            if (option.hidden()) {
                continue;
            }
            builder.addItem(String.valueOf(option.index()), option.name(), option.tagline());
        }
        builder.addItem("q", "Quit", "");
        printText(builder.build().render());
    }

    private GameCatalogEntry parseSelection(String input) {
        if (input.equalsIgnoreCase("z")) {
            return gameOptions.stream().filter(GameCatalogEntry::hidden).findFirst().orElse(null);
        }
        try {
            int index = Integer.parseInt(input);
            return gameOptions.stream()
                    .filter(opt -> !opt.hidden())
                    .filter(opt -> opt.index() == index)
                    .findFirst()
                    .orElse(null);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    // Pattern: Orchestration
    // - Runs the core translator -> command -> engine -> narrator loop with a single translation pass.
    private void walkabout(GameCatalogEntry option, GameSave save, Scanner scanner) throws GameBuilderException {
        returnToMenu = false;
        LoopConfig loopConfig = RuntimeLoader.loadLoopConfig(option.resource());
        List<FootprintRule> footprintRules = RuntimeLoader.loadFootprintRules(option.resource());
        LoopRuntime loopRuntime = new LoopRuntime(save, loopConfig, footprintRules);
        WorldBuildResult world = loopRuntime.buildWorld();
        KernelRegistry registry = world.registry();
        UUID currentPlot = world.startPlotId();

        narrator = new SceneNarrator(narrationService);
        runtime = new GameRuntime(narrator, this::emit, aiEnabled);

        UUID playerId = runtime.findPlayerActor(registry, currentPlot);
        List<Item> inventory = new ArrayList<>(runtime.startingInventory(registry, playerId));
        Map<UUID, Map<UUID, Rectangle2D>> inventoryPlacements = new HashMap<>();
        runtime.seedInventoryPlacements(inventory, inventoryPlacements);

        printBlank();
        println("=== " + option.name() + " ===");
        printBlank();
        if (save.preamble() != null && !save.preamble().isBlank()) {
            printNarration(save.preamble());
        }
        String backstory = RuntimeLoader.loadBackstory(option.resource());
        if (backstory != null && !backstory.isBlank()) {
            printNarration(backstory);
        }
        narrator.setBackstory(backstory);

        Map<String, CraftingRecipe> craftingRecipes = RuntimeLoader.loadCraftingRecipes(option.resource());
        Map<String, TokenType> extraAliases = RuntimeLoader.loadVerbAliases(option.resource());
        commandInterpreter.setExtraKeywords(extraAliases);
        aliasMap = mergeAliasMap(extraAliases);
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
        if (aiEnabled && !smartActorSpecs.isEmpty()) {
            SmartActorRegistry smartActorRegistry = SmartActorRegistry.create(registry, smartActorSpecs);
            SmartActorPlanner planner = new SmartActorPlanner(aiEnabled, apiKey, smartActorDebug);
            SmartActorRuntime smartActorRuntime = new SmartActorRuntime(
                    smartActorRegistry,
                    smartActorTags,
                    planner,
                    translatorService,
                    commandInterpreter,
                    commandHandlers,
                    smartActorDebug
            );
            smartActorRuntime.setLocalOnly(this.smartActorLocalOnly);
            runtime.configureSmartActors(smartActorRuntime);
        }
        CommandContext context = new CommandContext(this, runtime);

gameLoop:
        while (true) {
            System.out.print(mode == GameMode.Z2025 ? "\n> " : "\n_ ");
            String line = scanner.nextLine();
            if (line == null) {
                return;
            }
            String input = line.trim();
            narrator.setLastUtterance(input);
            if (input.isEmpty()) {
                continue;
            }
            printBlank();
            GameRuntime.InteractionState interactionState = runtime.interactionState();
            if (interactionState.type() != GameRuntime.InteractionType.NONE) {
                if (interactionState.type() == GameRuntime.InteractionType.AWAITING_DICE) {
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
                    return;
                }
                if (mentionHandling != MentionHandling.NOT_HANDLED) {
                    continue;
                }
                String actorLabel = runtime.conversationActorLabel();
                narrator.setLastCommand(actorLabel.isBlank() ? "talk" : "talk " + actorLabel);
                runtime.talkToConversation(input);
                CommandOutcome turnOutcome = runtime.advanceTurn();
                if (turnOutcome.endGame()) {
                    return;
                }
                if (turnOutcome.skipTurnAdvance()) {
                    continue;
                }
                continue;
            }

            MentionHandling mentionHandling = handleMention(mention);
            if (mentionHandling == MentionHandling.END_GAME) {
                return;
            }
            if (mentionHandling != MentionHandling.NOT_HANDLED) {
                continue;
            }

            String commandText = input;
            Command cmd = parseCommand(commandText);
            boolean localValid = isValidCommand(cmd);
            boolean translated = false;
            if (aiEnabled) {
                if (!localValid) {
                    List<String> fixtures = runtime.visibleFixtureLabels();
                    List<String> items = runtime.visibleItemLabels();
                    List<String> inventoryLabels = runtime.inventoryLabels();
                    TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                            translatorService,
                            input,
                            fixtures,
                            items,
                            inventoryLabels,
                            narrator.lastState(),
                            translatorDebug,
                            this::parseCommand,
                            System.out::println
                    );
                    if (outcome.type() == TranslationOrchestrator.OutcomeType.FAILED) {
                        println("~ translator failed; please rephrase (try HELP or a direction).");
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
                    println("~ translator failed; please rephrase (try HELP or a direction).");
                    continue gameLoop;
                }
                runtime.narrate(formatCommandError(cmd.error()));
                continue;
            }
            if (cmd.action() == CommandAction.UNKNOWN) {
                if (translated) {
                    println("~ translator failed; please rephrase (try HELP or a direction).");
                    continue;
                }
                runtime.narrate("Unknown command. Type help for commands.");
                continue;
            }
            GameCommandHandler handler = commandHandlers.get(cmd.action());
            if (handler == null) {
                runtime.narrate("Unknown command. Type help for commands.");
                continue;
            }
            if (cmd.action() == CommandAction.QUIT) {
                returnToMenu = true;
            }
            CommandOutcome outcome = handler.handle(context, cmd);
            if (outcome.endGame()) {
                return;
            }
            if (outcome.skipTurnAdvance()) {
                continue gameLoop;
            }

            CommandOutcome turnOutcome = runtime.advanceTurn();
            if (turnOutcome.endGame()) {
                return;
            }
            if (turnOutcome.skipTurnAdvance()) {
                continue gameLoop;
            }
        }
    }

    private Command parseCommand(String input) {
        return commandInterpreter.interpret(input);
    }

    private boolean isValidCommand(Command command) {
        return command != null && command.action() != CommandAction.UNKNOWN && !command.hasError();
    }

    private String formatCommandError(CommandParseError error) {
        if (error == null) {
            return "Invalid command.";
        }
        String suffix = error.column() >= 0 ? " (col " + (error.column() + 1) + ")" : "";
        return "Invalid command: " + error.message() + suffix;
    }

    private boolean isConversationExit(String input) {
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

    private enum MentionParseType {
        NONE,
        INVALID,
        AMBIGUOUS,
        UNKNOWN,
        MATCH
    }

    private record MentionParse(MentionParseType type, String actorLabel, String utterance) {
        static MentionParse none() {
            return new MentionParse(MentionParseType.NONE, "", "");
        }
    }

    private enum MentionHandling {
        NOT_HANDLED,
        CONTINUE,
        END_GAME
    }

    private MentionParse resolveMention(String input) {
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
        GameRuntime.MentionResolution resolution = runtime.resolveMentionActor(afterWords);
        if (resolution.type() == GameRuntime.MentionResolutionType.NONE) {
            return new MentionParse(MentionParseType.UNKNOWN, "", "");
        }
        if (resolution.type() == GameRuntime.MentionResolutionType.AMBIGUOUS) {
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
                narrate("Talk to whom?");
                return MentionHandling.CONTINUE;
            }
            case AMBIGUOUS -> {
                narrate("Be specific.");
                return MentionHandling.CONTINUE;
            }
            case UNKNOWN -> {
                narrate("You don't see anyone by that name.");
                return MentionHandling.CONTINUE;
            }
            case MATCH -> {
                String label = mention.actorLabel() == null ? "" : mention.actorLabel().trim();
                if (label.isBlank()) {
                    narrate("No one answers.");
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

    private int findMentionToken(List<Token> tokens) {
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

    private List<String> collectWords(List<Token> tokens, int startIdx, int endIdx) {
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

    @Override
    public void emit(String text) {
        print(text);
    }

    private void narrate(String text) {
        if (runtime == null) {
            print(text);
            return;
        }
        runtime.narrate(text);
    }

    @Override
    public void printHelp() {
        String help = HELP_TEXT.trim();
        String aliasSummary = buildAliasSummary();
        if (!aliasSummary.isBlank()) {
            help = help + "\n" + aliasSummary;
        }
        narrate(help);
    }

    private String buildAliasSummary() {
        List<String> entries = new ArrayList<>();
        for (var entry : aliasMap.entrySet()) {
            String alias = entry.getKey();
            if (!isWordAlias(alias)) {
                continue;
            }
            String canonical = entry.getValue();
            entries.add(alias.toLowerCase(Locale.ROOT) + " -> " + canonical.toLowerCase(Locale.ROOT));
        }
        if (entries.isEmpty()) {
            return "";
        }
        return "Aliases: " + String.join(", ", entries);
    }

    private Map<String, String> mergeAliasMap(Map<String, TokenType> extraAliases) {
        Map<String, String> merged = new LinkedHashMap<>(VerbAliases.aliasMap());
        if (extraAliases == null || extraAliases.isEmpty()) {
            return merged;
        }
        for (Map.Entry<String, TokenType> entry : extraAliases.entrySet()) {
            String alias = entry.getKey();
            TokenType type = entry.getValue();
            if (alias == null || alias.isBlank() || type == null) {
                continue;
            }
            merged.put(alias.trim().toUpperCase(Locale.ROOT), type.name());
        }
        return merged;
    }

    private boolean isWordAlias(String alias) {
        if (alias == null || alias.length() < 2) {
            return false;
        }
        for (int i = 0; i < alias.length(); i++) {
            if (!Character.isLetter(alias.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private String resolveApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("OPENAI_API_KEY");
        }
        return key;
    }

    private boolean isSmartActorLocalOnly(AiConfig config) {
        String scope = config == null ? "" : config.getString("ai.smart_actor.scope", "local");
        if (scope == null) {
            return true;
        }
        String normalized = scope.trim().toLowerCase(Locale.ROOT);
        return !(normalized.equals("global") || normalized.equals("all"));
    }

}

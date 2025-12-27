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
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.VerbAliases;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
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
import java.util.Objects;

/**
 * Minimal CLI launcher to pick a game and print its preamble.
 */
public final class GameCli extends BuuiConsole implements CommandOutput {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-00000000feed");

    private static final String HELP_TEXT = """
Commands:
  move (go) <direction> Move (N,S,E,W,NE,NW,SE,SW,UP,DOWN)
  n|s|e|w|u|d|ne|nw|se|sw Shortcut movement
  look (l)       Describe the current plot
  look <thing>   Inspect an item/fixture/actor you can see or carry
  inspect <thing> Look closely at an item/fixture/actor
  listen         Quick re-read of the current scene (alias of look)
  take <item>    Pick up a visible item here
  drop <item>    Drop an item from your inventory
  put <item>     Drop an item from your inventory
  open <thing>   Try opening a visible item or gate
  use <thing>    Try using an item
  attack <target> Attack a visible actor
  flee/run away  Try to escape combat
  inventory (i)  Show what you're carrying
  craft <item>   Craft an item if you have the ingredients
  how craft <item> Show required skill and ingredients
  search/explore Poke around for hidden items
  help (h, ?)    Show this help
  quit (q)       Exit the game
""";

    private record GameOption(int index, String name, String resource, String tagline, boolean hidden) { }

    private static final List<GameOption> OPTIONS = List.of(
            new GameOption(1, "Island Adventure", "src/main/resources/games/island/game.yaml", "CASTAWAY THRILLS. Jungle traps, cliffside caves, and a desperate raft-for-freedom dash before the adventure claims you for good.", false),
            new GameOption(2, "Mansion Adventure", "src/main/resources/games/mansion/game.yaml", "THE MYSTERY HOUSE. Secret doors. Creaking halls. One wrong turn and the mansion keeps you forever.", false),
            new GameOption(3, "Western Adventure", "src/main/resources/games/western/game.yaml", "TERROR AT DEAD-MAN’S GULCH. TNT on the rails. The Iron Rattler thundering in. One last chance at Rattlesnake Bridge.", false),
            new GameOption(4, "Spy Adventure", "src/main/resources/games/spy/game.yaml", "TRAUMA-BOND, INTERNATIONAL MAN OF MAYHEM. Glamour, gadgets, and a race to disarm the Jade Capsule before it lights the world up.", false),
            new GameOption(6, "Zone Builder Demo", "logs/zone-game.yaml", "Generated from src/test/resources/zone-demo/zone-input.sample.yaml (run ZoneBuilderCli to refresh).", true)
    );

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

    public GameCli() {
        this(GameMode.Z1980);
    }

    private final String apiKey;
    private final boolean aiEnabled;
    private final boolean translatorDebug;
    private final boolean smartActorDebug;
    private final NarrationService narrationService;
    private final TranslatorService translatorService;
    private final CommandInterpreter commandInterpreter = new CommandInterpreter();
    private final Map<CommandAction, GameCommandHandler> commandHandlers = CommandHandlers.defaultHandlers();
    private Map<String, String> aliasMap = VerbAliases.aliasMap();
    private SceneNarrator narrator;
    private GameRuntime runtime;

    public GameCli(GameMode mode) {
        // Keep player output clean even when GameCli is constructed directly (tests bypass main()).
        KeyExpressionEvaluator.setDebugOutput(false);
        this.mode = mode == null ? GameMode.Z1980 : mode;
        this.apiKey = resolveApiKey();
        AiConfig config = AiConfig.load();
        this.aiEnabled = this.mode == GameMode.Z2025 && this.apiKey != null && !this.apiKey.isBlank();
        this.translatorDebug = config.getBoolean("ai.translator.debug", false);
        this.smartActorDebug = config.getBoolean("ai.smart_actor.debug", false);
        this.narrationService = new NarrationService(
                aiEnabled,
                apiKey,
                config.getBoolean("ai.narrator.debug", false)
        );
        this.translatorService = new TranslatorService(aiEnabled, apiKey);
        if (!aiEnabled) {
            print("~ AI disabled (mode=" + this.mode + ", apiKey=" + (apiKey == null ? "missing" : "present") + ")");
        } else {
            print("~ AI enabled (mode=2025)");
        }
    }

    private void run() {
        printMenu();
        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                long visibleCount = OPTIONS.stream().filter(o -> !o.hidden()).count();
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
                    print("Goodbye.");
                    return;
                }
                GameOption selected = parseSelection(input);
                if (selected == null) {
                    print("Unknown selection: " + input);
                    continue;
                }
                try {
                    GameSave save = RuntimeLoader.loadSave(selected.resource());
                    walkabout(selected, save, scanner);
                    return;
                } catch (Exception ex) {
                    print("Failed to load game: " + ex.getMessage());
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
        for (GameOption option : OPTIONS) {
            if (option.hidden()) {
                continue;
            }
            builder.addItem(String.valueOf(option.index()), option.name(), option.tagline());
        }
        builder.addItem("q", "Quit", "");
        printText(builder.build().render());
    }

    private GameOption parseSelection(String input) {
        if (input.equalsIgnoreCase("z")) {
            return OPTIONS.stream().filter(GameOption::hidden).findFirst().orElse(null);
        }
        try {
            int index = Integer.parseInt(input);
            return OPTIONS.stream()
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
    private void walkabout(GameOption option, GameSave save, Scanner scanner) throws GameBuilderException {
        LoopConfig loopConfig = RuntimeLoader.loadLoopConfig(option.resource);
        LoopRuntime loopRuntime = new LoopRuntime(save, loopConfig);
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
        print("=== " + option.name() + " ===");
        printBlank();
        if (save.preamble() != null && !save.preamble().isBlank()) {
            print(save.preamble());
        }
        String backstory = RuntimeLoader.loadBackstory(option.resource);
        if (backstory != null && !backstory.isBlank()) {
            print(backstory);
        }
        narrator.setBackstory(backstory);

        Map<String, CraftingRecipe> craftingRecipes = RuntimeLoader.loadCraftingRecipes(option.resource);
        Map<String, TokenType> extraAliases = RuntimeLoader.loadVerbAliases(option.resource);
        commandInterpreter.setExtraKeywords(extraAliases);
        aliasMap = mergeAliasMap(extraAliases);
        TriggerEngine triggerEngine = new TriggerEngine(RuntimeLoader.loadTriggerDefinitions(option.resource));

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
        List<SmartActorSpec> smartActorSpecs = RuntimeLoader.loadSmartActorSpecs(option.resource);
        SmartActorTagIndex smartActorTags = RuntimeLoader.loadSmartActorTags(option.resource);
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
                        print("~ translator failed; please rephrase (try HELP or a direction).");
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
                    print("~ translator failed; please rephrase (try HELP or a direction).");
                    continue gameLoop;
                }
                runtime.narrate(formatCommandError(cmd.error()));
                continue;
            }
            if (cmd.action() == CommandAction.UNKNOWN) {
                if (translated) {
                    print("~ translator failed; please rephrase (try HELP or a direction).");
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

}

package com.demo.adventure.engine.cli;

import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.buui.BuuiMenu;
import com.demo.adventure.ai.runtime.AiConfig;
import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.ai.runtime.TranslatorService;
import com.demo.adventure.engine.command.CommandOutput;
import com.demo.adventure.engine.command.handlers.CommandHandlers;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandParseError;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.VerbAliases;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.engine.runtime.SceneNarrator;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.ArrayList;
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

    enum GameMode { Z1980, Z2025;
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
    private final GameSessionRunner sessionRunner;

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
        this.sessionRunner = new GameSessionRunner(this);
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
                    boolean returnToMenu = sessionRunner.run(selected, save, scanner);
                    if (!returnToMenu) {
                        return;
                    }
                } catch (Exception ex) {
                    println("Failed to load game: " + ex.getMessage());
                }
            }
        }
    }

    private void walkabout(GameCatalogEntry option, GameSave save, Scanner scanner) throws GameBuilderException {
        sessionRunner.run(option, save, scanner);
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

    GameMode mode() {
        return mode;
    }

    boolean aiEnabled() {
        return aiEnabled;
    }

    String apiKey() {
        return apiKey;
    }

    boolean translatorDebug() {
        return translatorDebug;
    }

    boolean smartActorDebug() {
        return smartActorDebug;
    }

    boolean smartActorLocalOnly() {
        return smartActorLocalOnly;
    }

    NarrationService narrationService() {
        return narrationService;
    }

    TranslatorService translatorService() {
        return translatorService;
    }

    CommandInterpreter commandInterpreter() {
        return commandInterpreter;
    }

    Map<CommandAction, GameCommandHandler> commandHandlers() {
        return commandHandlers;
    }

    void setNarrator(SceneNarrator narrator) {
        this.narrator = narrator;
    }

    void setRuntime(GameRuntime runtime) {
        this.runtime = runtime;
    }

    void setAliasMap(Map<String, String> aliasMap) {
        this.aliasMap = aliasMap == null ? Map.of() : aliasMap;
    }

    void printBlankLine() {
        printBlank();
    }

    void printlnLine(String text) {
        println(text);
    }

    void printNarrationLine(String text) {
        printNarration(text);
    }

    private Command parseCommand(String input) {
        return sessionRunner.parseCommand(input);
    }

    private boolean isValidCommand(Command command) {
        return sessionRunner.isValidCommand(command);
    }

    private String formatCommandError(CommandParseError error) {
        return sessionRunner.formatCommandError(error);
    }

    private boolean isConversationExit(String input) {
        return sessionRunner.isConversationExit(input);
    }

    private Object resolveMention(String input) {
        return sessionRunner.resolveMention(input);
    }

    private int findMentionToken(List<Token> tokens) {
        return sessionRunner.findMentionToken(tokens);
    }

    private List<String> collectWords(List<Token> tokens, int startIdx, int endIdx) {
        return sessionRunner.collectWords(tokens, startIdx, endIdx);
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

    Map<String, String> mergeAliasMap(Map<String, TokenType> extraAliases) {
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

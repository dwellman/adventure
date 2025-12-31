package com.demo.adventure.engine.cli;

import com.demo.adventure.ai.runtime.AiConfig;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandParseError;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GameCliHelperTest {

    @RegisterExtension
    static ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void parseSelectionHandlesHiddenAndInvalid() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        Object hidden = invoke(cli, "parseSelection", new Class<?>[]{String.class}, "z");
        Object first = invoke(cli, "parseSelection", new Class<?>[]{String.class}, "1");
        Object invalid = invoke(cli, "parseSelection", new Class<?>[]{String.class}, "nope");

        assertThat(hidden).isNotNull();
        assertThat(first).isNotNull();
        assertThat(invalid).isNull();
    }

    @Test
    void formatCommandErrorIncludesColumn() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        CommandParseError error = new CommandParseError("Bad verb", 2, "bad");
        String formatted = (String) invoke(cli, "formatCommandError", new Class<?>[]{CommandParseError.class}, error);
        String fallback = (String) invoke(cli, "formatCommandError", new Class<?>[]{CommandParseError.class}, new Object[]{null});

        assertThat(formatted).contains("Bad verb").contains("col 3");
        assertThat(fallback).isEqualTo("Invalid command.");
    }

    @Test
    void aliasHelpersMergeAndSummarizeWordAliases() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        Map<String, TokenType> extras = new HashMap<>();
        extras.put("sneak", TokenType.MOVE);
        extras.put("", TokenType.LOOK);
        extras.put("jump", null);

        @SuppressWarnings("unchecked")
        Map<String, String> merged = (Map<String, String>) invoke(cli, "mergeAliasMap",
                new Class<?>[]{Map.class},
                extras
        );
        assertThat(merged).containsEntry("SNEAK", "MOVE");

        setField(cli, "aliasMap", merged);
        String summary = (String) invoke(cli, "buildAliasSummary", new Class<?>[0]);

        assertThat(summary).contains("sneak -> move");
        assertThat((boolean) invoke(cli, "isWordAlias", new Class<?>[]{String.class}, "run")).isTrue();
        assertThat((boolean) invoke(cli, "isWordAlias", new Class<?>[]{String.class}, "n")).isFalse();
        assertThat((boolean) invoke(cli, "isWordAlias", new Class<?>[]{String.class}, "r2")).isFalse();
    }

    @Test
    void aliasSummarySkipsNonWordAliases() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        Map<String, String> aliases = new HashMap<>();
        aliases.put("Q", "QUIT");
        aliases.put("1", "LOOK");
        setField(cli, "aliasMap", aliases);

        String summary = (String) invoke(cli, "buildAliasSummary", new Class<?>[0]);

        assertThat(summary).isEmpty();

        @SuppressWarnings("unchecked")
        Map<String, String> merged = (Map<String, String>) invoke(cli, "mergeAliasMap",
                new Class<?>[]{Map.class},
                new HashMap<>()
        );
        assertThat(merged).containsKey("Q");
    }

    @Test
    void printHelpIncludesAliases() {
        GameCli cli = new GameCli();
        console.reset();

        cli.printHelp();
        String output = console.output();

        assertThat(output).contains("Commands:");
        assertThat(output).contains("Aliases:");
    }

    @Test
    void printHelpSkipsEmptyAliasSummary() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        Map<String, String> aliases = new HashMap<>();
        aliases.put("Q", "QUIT");
        setField(cli, "aliasMap", aliases);

        cli.printHelp();

        assertThat(console.output()).contains("Commands:");
        assertThat(console.output()).doesNotContain("Aliases:");
    }

    @Test
    void parsesConversationExitAndMentions() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        assertThat((boolean) invoke(cli, "isConversationExit", new Class<?>[]{String.class}, "okay, bye")).isTrue();
        assertThat((boolean) invoke(cli, "isConversationExit", new Class<?>[]{String.class}, "okay bye now")).isFalse();

        List<Token> tokens = CommandScanner.scan("say \"hello there\" @Elias hi");
        int mentionIndex = (int) invoke(cli, "findMentionToken", new Class<?>[]{List.class}, tokens);
        assertThat(mentionIndex).isGreaterThanOrEqualTo(0);

        @SuppressWarnings("unchecked")
        List<String> before = (List<String>) invoke(cli, "collectWords",
                new Class<?>[]{List.class, int.class, int.class},
                tokens,
                0,
                mentionIndex
        );
        @SuppressWarnings("unchecked")
        List<String> after = (List<String>) invoke(cli, "collectWords",
                new Class<?>[]{List.class, int.class, int.class},
                tokens,
                mentionIndex + 1,
                tokens.size()
        );

        assertThat(before).containsExactly("say", "hello", "there");
        assertThat(after).containsExactly("Elias", "hi");

        int missing = (int) invoke(cli, "findMentionToken", new Class<?>[]{List.class}, List.of());
        assertThat(missing).isEqualTo(-1);
    }

    @Test
    void parseCommandAndValidationWork() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        Command command = (Command) invoke(cli, "parseCommand", new Class<?>[]{String.class}, "look");
        boolean valid = (boolean) invoke(cli, "isValidCommand", new Class<?>[]{Command.class}, command);

        assertThat(valid).isTrue();
    }

    @Test
    void resolvesApiKeyAndSmartActorScope() throws Exception {
        String originalKey = System.getProperty("OPENAI_API_KEY");
        String originalScope = System.getProperty("ai.smart_actor.scope");
        GameCli cli = new GameCli();
        console.reset();
        try {
            System.setProperty("OPENAI_API_KEY", "test-key");
            System.setProperty("ai.smart_actor.scope", "global");

            String key = (String) invoke(cli, "resolveApiKey", new Class<?>[0]);
            boolean localOnly = (boolean) invoke(cli, "isSmartActorLocalOnly",
                    new Class<?>[]{AiConfig.class},
                    AiConfig.load()
            );
            boolean localOnlyDefault = (boolean) invoke(cli, "isSmartActorLocalOnly",
                    new Class<?>[]{AiConfig.class},
                    new Object[]{null}
            );

            assertThat(key).isEqualTo("test-key");
            assertThat(localOnly).isFalse();
            assertThat(localOnlyDefault).isTrue();
        } finally {
            restoreProperty("OPENAI_API_KEY", originalKey);
            restoreProperty("ai.smart_actor.scope", originalScope);
        }
    }

    @Test
    void gameModeFromArgsResolvesFlags() throws Exception {
        Class<?> modeClass = Class.forName("com.demo.adventure.engine.cli.GameCli$GameMode");
        Method fromArgs = modeClass.getDeclaredMethod("fromArgs", String[].class);
        fromArgs.setAccessible(true);

        Object z2025 = fromArgs.invoke(null, (Object) new String[]{"--mode=2025"});
        Object z1980 = fromArgs.invoke(null, (Object) new String[]{"--mode=Z1980"});
        Object fallback = fromArgs.invoke(null, (Object) null);
        Object unknown = fromArgs.invoke(null, (Object) new String[]{"--mode=other"});

        assertThat(z2025.toString()).isEqualTo("Z2025");
        assertThat(z1980.toString()).isEqualTo("Z1980");
        assertThat(fallback.toString()).isEqualTo("Z1980");
        assertThat(unknown.toString()).isEqualTo("Z1980");
    }

    @Test
    void helperEarlyReturnsAvoidNulls() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        Object mention = invoke(cli, "resolveMention", new Class<?>[]{String.class}, "@Elias");
        Method typeMethod = mention.getClass().getDeclaredMethod("type");
        typeMethod.setAccessible(true);
        Object type = typeMethod.invoke(mention);
        assertThat(type.toString()).isEqualTo("NONE");

        @SuppressWarnings("unchecked")
        Map<String, String> merged = (Map<String, String>) invoke(cli, "mergeAliasMap",
                new Class<?>[]{Map.class},
                new Object[]{null}
        );
        assertThat(merged).containsKey("Q");

        @SuppressWarnings("unchecked")
        List<String> words = (List<String>) invoke(cli, "collectWords",
                new Class<?>[]{List.class, int.class, int.class},
                List.of(),
                1,
                0
        );
        assertThat(words).isEmpty();
    }

    @Test
    void conversationExitHandlesQuotedInputAndEmit() throws Exception {
        GameCli cli = new GameCli();
        console.reset();

        boolean quotedExit = (boolean) invoke(cli, "isConversationExit", new Class<?>[]{String.class}, "\"okay bye\"");
        cli.emit("ping");

        assertThat(quotedExit).isTrue();
        assertThat(console.output()).contains("ping");
    }

    private static Object invoke(GameCli cli, String methodName, Class<?>[] types, Object... args) throws Exception {
        try {
            Method method = GameCli.class.getDeclaredMethod(methodName, types);
            method.setAccessible(true);
            return method.invoke(cli, args);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception causeException) {
                throw causeException;
            }
            throw ex;
        }
    }

    private static void setField(GameCli cli, String fieldName, Object value) throws Exception {
        Field field = GameCli.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(cli, value);
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}

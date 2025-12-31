package com.demo.adventure.ai.runtime;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.interpreter.CommandScanner;

import java.util.List;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

public final class TranslationOrchestrator {
    private static final String EMOTE_PREFIX = "EMOTE:";

    private TranslationOrchestrator() {}

    public enum OutcomeType {
        COMMAND,
        EMOTE,
        FAILED
    }

    public record Outcome(
            OutcomeType type,
            String commandText,
            String failureMessage
    ) {
    }

    @FunctionalInterface
    public interface CommandParser {
        Command parse(String input);
    }

    public static Outcome resolve(
            TranslatorService translatorService,
            String input,
            List<String> fixtures,
            List<String> items,
            List<String> inventory,
            String sceneContext,
            boolean translatorDebug,
            CommandParser commandParser,
            Consumer<String> logger
    ) {
        if (translatorDebug) {
            logger.accept("~ translator input='" + input + "'");
        }
        TranslatorService.TranslationResult translated = translatorService.translate(
                input, fixtures, items, inventory, sceneContext
        );
        if (translated.type() == TranslatorService.TranslationResult.Type.EMOTE) {
            String emoteText = stripEmotePrefix(translated.command());
            if (violatesGrounding(input, emoteText, fixtures, items, inventory)) {
                if (translatorDebug) {
                    logger.accept("~ translator rejected ungrounded emote: " + translated.command());
                }
                return new Outcome(
                        OutcomeType.FAILED,
                        null,
                        "translator produced ungrounded emote"
                );
            }
            if (translatorDebug) {
                logger.accept("~ translator emote: " + emoteText);
            }
            return new Outcome(
                    OutcomeType.EMOTE,
                    normalizeEmoteLine(emoteText),
                    null
            );
        }
        if (translated.type() != TranslatorService.TranslationResult.Type.COMMAND) {
            if (translatorDebug) {
                logger.accept("~ translator output error: " + translated.error());
            }
            return new Outcome(
                    OutcomeType.FAILED,
                    null,
                    "translator output error: " + translated.error()
            );
        }
        String commandText = translated.command();
        if (violatesGrounding(input, commandText, fixtures, items, inventory)) {
            if (translatorDebug) {
                logger.accept("~ translator rejected ungrounded command: " + commandText);
            }
            return new Outcome(
                    OutcomeType.FAILED,
                    null,
                    "translator produced ungrounded command"
            );
        }
        Command command = commandParser.parse(commandText);
        boolean invalid = command.action() == CommandAction.UNKNOWN || command.hasError();
        if (invalid) {
            if (translatorDebug) {
                logger.accept("~ translator invalid command: " + commandText);
            }
            return new Outcome(
                    OutcomeType.FAILED,
                    null,
                    "translator produced invalid command"
            );
        }
        if (translatorDebug) {
            logger.accept("~ translator command: " + commandText);
        }
        return new Outcome(OutcomeType.COMMAND, commandText, null);
    }

    private static String normalizeEmoteLine(String emoteText) {
        if (emoteText == null) {
            return EMOTE_PREFIX;
        }
        String trimmed = emoteText.trim();
        if (trimmed.isEmpty()) {
            return EMOTE_PREFIX;
        }
        return EMOTE_PREFIX + " " + trimmed;
    }

    private static String stripEmotePrefix(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.trim();
        if (trimmed.length() < EMOTE_PREFIX.length()) {
            return trimmed;
        }
        if (!trimmed.regionMatches(true, 0, EMOTE_PREFIX, 0, EMOTE_PREFIX.length())) {
            return trimmed;
        }
        return trimmed.substring(EMOTE_PREFIX.length()).trim();
    }

    private static boolean violatesGrounding(String input,
                                             String output,
                                             List<String> fixtures,
                                             List<String> items,
                                             List<String> inventory) {
        if (output == null || output.isBlank()) {
            return false;
        }
        Set<String> outputTokens = extractGroundingTokens(output);
        if (outputTokens.isEmpty()) {
            return false;
        }
        Set<String> inputTokens = extractGroundingTokens(input);
        Set<String> allowedTokens = new java.util.LinkedHashSet<>(inputTokens);
        allowedTokens.addAll(extractOverlappingLabelTokens(inputTokens, fixtures));
        allowedTokens.addAll(extractOverlappingLabelTokens(inputTokens, items));
        allowedTokens.addAll(extractOverlappingLabelTokens(inputTokens, inventory));
        return allowedTokens.isEmpty() || !allowedTokens.containsAll(outputTokens);
    }

    private static Set<String> extractGroundingTokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        List<Token> tokens = CommandScanner.scan(text);
        Set<String> grounded = new java.util.LinkedHashSet<>();
        for (Token token : tokens) {
            if (!GROUNDING_TOKENS.contains(token.type)) {
                continue;
            }
            if (DIRECTION_TOKENS.contains(token.type)) {
                grounded.add(token.type.name());
                continue;
            }
            String lexeme = normalizeLexeme(token.lexeme);
            if (!lexeme.isBlank()) {
                grounded.add(lexeme);
            }
            if (token.type == TokenType.STRING) {
                for (String part : lexeme.split("\\s+")) {
                    if (!part.isBlank()) {
                        grounded.add(part);
                    }
                }
            }
        }
        return grounded;
    }

    private static Set<String> extractOverlappingLabelTokens(Set<String> inputTokens, List<String> labels) {
        if (labels == null || labels.isEmpty() || inputTokens == null || inputTokens.isEmpty()) {
            return Set.of();
        }
        Set<String> grounded = new java.util.LinkedHashSet<>();
        for (String label : labels) {
            Set<String> labelTokens = extractGroundingTokens(label);
            if (labelTokens.isEmpty()) {
                continue;
            }
            boolean overlap = false;
            for (String token : labelTokens) {
                if (inputTokens.contains(token)) {
                    overlap = true;
                    break;
                }
            }
            if (overlap) {
                grounded.addAll(labelTokens);
            }
        }
        return grounded;
    }

    private static final Set<TokenType> DIRECTION_TOKENS = EnumSet.of(
            TokenType.NORTH,
            TokenType.NORTH_EAST,
            TokenType.EAST,
            TokenType.SOUTH_EAST,
            TokenType.SOUTH,
            TokenType.SOUTH_WEST,
            TokenType.WEST,
            TokenType.NORTH_WEST,
            TokenType.UP,
            TokenType.DOWN
    );

    private static final Set<TokenType> GROUNDING_TOKENS = EnumSet.of(
            TokenType.IDENTIFIER,
            TokenType.STRING,
            TokenType.NORTH,
            TokenType.NORTH_EAST,
            TokenType.EAST,
            TokenType.SOUTH_EAST,
            TokenType.SOUTH,
            TokenType.SOUTH_WEST,
            TokenType.WEST,
            TokenType.NORTH_WEST,
            TokenType.UP,
            TokenType.DOWN
    );

    private static String normalizeLexeme(String lexeme) {
        return lexeme == null ? "" : lexeme.trim().toLowerCase(java.util.Locale.ROOT);
    }
}

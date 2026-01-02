package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class RuntimeEmoteDice {
    private static final String EMOTE_PREFIX = "EMOTE:";
    private static final String CHECK_REQUEST_PREFIX = "CHECK_REQUEST:";
    private static final String CHECK_RESULT_PREFIX = "CHECK_RESULT:";
    private static final int EMOTE_CHECK_SIDES = 20;
    private static final int EMOTE_CHECK_TARGET = 15;
    private static final Set<String> EMOTE_CHECK_KEYWORDS = Set.of(
            "distract",
            "convince",
            "persuade",
            "intimidate",
            "deceive",
            "lie",
            "sneak",
            "steal",
            "pick",
            "unlock",
            "hide",
            "escape",
            "bluff",
            "charm"
    );

    private final GameRuntime runtime;
    private PendingEmoteCheck pendingEmoteCheck;
    private GameRuntime.InteractionState interactionState = GameRuntime.InteractionState.none();

    RuntimeEmoteDice(GameRuntime runtime) {
        this.runtime = runtime;
    }

    void reset() {
        pendingEmoteCheck = null;
        interactionState = GameRuntime.InteractionState.none();
    }

    GameRuntime.InteractionState interactionState() {
        return interactionState == null ? GameRuntime.InteractionState.none() : interactionState;
    }

    void emote(String rawEmote) {
        String emoteText = normalizeEmoteText(rawEmote);
        if (emoteText.isBlank()) {
            return;
        }
        if (emoteNeedsCheck(emoteText)) {
            pendingEmoteCheck = new PendingEmoteCheck(emoteText, EMOTE_CHECK_SIDES, EMOTE_CHECK_TARGET);
            interactionState = GameRuntime.InteractionState.awaitingDice(
                    formatDiceCall(pendingEmoteCheck.sides(), pendingEmoteCheck.target())
            );
            runtime.narrate(formatCheckRequest(pendingEmoteCheck));
            return;
        }
        pendingEmoteCheck = null;
        interactionState = GameRuntime.InteractionState.none();
        runtime.narrate(formatEmote(emoteText));
    }

    void rollDice(String argument) {
        if (runtime.isOutputSuppressed()) {
            return;
        }
        if (pendingEmoteCheck == null || interactionState.type() != GameRuntime.InteractionType.AWAITING_DICE) {
            interactionState = GameRuntime.InteractionState.none();
            runtime.narrate("No check to roll.");
            return;
        }
        DiceSpec spec = parseDiceSpec(argument);
        if (spec == null) {
            spec = new DiceSpec(pendingEmoteCheck.sides(), pendingEmoteCheck.target());
        }
        if (!matchesPending(spec, pendingEmoteCheck)) {
            String expected = interactionState.expectedToken();
            if (expected.isBlank()) {
                expected = formatDiceCall(pendingEmoteCheck.sides(), pendingEmoteCheck.target());
            }
            runtime.narrate("Roll " + expected + ".");
            return;
        }
        DiceCheckResult result;
        try {
            result = evaluateDiceCheck(spec.sides(), spec.target());
        } catch (RuntimeException ex) {
            runtime.narrate(ex.getMessage());
            return;
        }
        String outcome = result.success() ? "SUCCESS" : "FAIL";
        String resolved = formatCheckResult(result.roll(), spec.target(), outcome, pendingEmoteCheck.emoteText());
        pendingEmoteCheck = null;
        interactionState = GameRuntime.InteractionState.none();
        runtime.narrate(resolved);
    }

    private String normalizeEmoteText(String rawEmote) {
        if (rawEmote == null) {
            return "";
        }
        String trimmed = rawEmote.trim();
        if (trimmed.length() >= EMOTE_PREFIX.length()
                && trimmed.regionMatches(true, 0, EMOTE_PREFIX, 0, EMOTE_PREFIX.length())) {
            trimmed = trimmed.substring(EMOTE_PREFIX.length()).trim();
        }
        return trimmed;
    }

    private boolean emoteNeedsCheck(String emoteText) {
        if (emoteText == null || emoteText.isBlank()) {
            return false;
        }
        List<Token> tokens = CommandScanner.scan(emoteText);
        for (Token token : tokens) {
            if (token == null || token.type == TokenType.EOL) {
                continue;
            }
            if (token.type == TokenType.STRING) {
                for (String part : token.lexeme.split("\\s+")) {
                    if (EMOTE_CHECK_KEYWORDS.contains(part.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
                continue;
            }
            if (token.type != TokenType.IDENTIFIER) {
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim().toLowerCase(Locale.ROOT);
            if (EMOTE_CHECK_KEYWORDS.contains(lexeme)) {
                return true;
            }
        }
        return false;
    }

    private DiceSpec parseDiceSpec(String argument) {
        if (argument == null || argument.isBlank()) {
            return null;
        }
        List<Token> tokens = CommandScanner.scan(argument);
        List<Integer> values = new ArrayList<>();
        for (Token token : tokens) {
            if (token == null || token.type == TokenType.EOL) {
                continue;
            }
            if (token.type != TokenType.IDENTIFIER && token.type != TokenType.NUMBER && token.type != TokenType.STRING) {
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim();
            if (lexeme.isEmpty()) {
                continue;
            }
            int parsed = parseDiceNumber(lexeme);
            if (parsed > 0) {
                values.add(parsed);
            }
            if (values.size() >= 2) {
                break;
            }
        }
        if (values.size() < 2) {
            return null;
        }
        return new DiceSpec(values.get(0), values.get(1));
    }

    private int parseDiceNumber(String lexeme) {
        String trimmed = lexeme == null ? "" : lexeme.trim();
        if (trimmed.isEmpty()) {
            return -1;
        }
        if (trimmed.startsWith("d") || trimmed.startsWith("D")) {
            trimmed = trimmed.substring(1);
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private DiceCheckResult evaluateDiceCheck(int sides, int target) {
        KeyExpressionEvaluator.DiceRoller previous = KeyExpressionEvaluator.getDefaultDiceRoller();
        List<Integer> rolls = new ArrayList<>();
        KeyExpressionEvaluator.setDefaultDiceRoller(s -> {
            int roll = previous.roll(s);
            rolls.add(roll);
            return roll;
        });
        boolean success;
        try {
            success = KeyExpressionEvaluator.evaluate("DICE(" + sides + ") >= " + target);
        } finally {
            KeyExpressionEvaluator.setDefaultDiceRoller(previous);
        }
        int roll = rolls.isEmpty() ? 0 : rolls.get(rolls.size() - 1);
        return new DiceCheckResult(roll, success);
    }

    private String formatDiceCall(int sides, int target) {
        return "dice(" + sides + ", " + target + ")";
    }

    private String formatEmote(String emoteText) {
        return EMOTE_PREFIX + " " + emoteText;
    }

    private String formatCheckRequest(PendingEmoteCheck check) {
        return CHECK_REQUEST_PREFIX + " " + formatDiceCall(check.sides(), check.target())
                + " | " + EMOTE_PREFIX + " " + check.emoteText();
    }

    private String formatCheckResult(int roll, int target, String outcome, String emoteText) {
        return CHECK_RESULT_PREFIX + " roll=" + roll + " target=" + target + " outcome=" + outcome
                + " | " + EMOTE_PREFIX + " " + emoteText;
    }

    private boolean matchesPending(DiceSpec spec, PendingEmoteCheck pending) {
        if (spec == null || pending == null) {
            return false;
        }
        return spec.sides() == pending.sides() && spec.target() == pending.target();
    }

    private record PendingEmoteCheck(String emoteText, int sides, int target) {
    }

    private record DiceSpec(int sides, int target) {
    }

    private record DiceCheckResult(int roll, boolean success) {
    }
}

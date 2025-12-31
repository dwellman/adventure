package com.demo.adventure.ai.runtime;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.interpreter.CommandScanner;

import java.util.List;

public final class NarrationPromptSelector {
    private static final String EMOTE_PREFIX = "EMOTE:";
    private static final String CHECK_REQUEST_PREFIX = "CHECK_REQUEST:";
    private static final String CHECK_RESULT_PREFIX = "CHECK_RESULT:";

    private NarrationPromptSelector() {
    }

    public static NarrationPromptMode select(String canonicalCommand, String rawEngineOutput, String colorEvent) {
        String raw = rawEngineOutput == null ? "" : rawEngineOutput.trim();
        String actionResult = containsExitsLine(raw) ? "" : raw;
        if (isCheckResult(actionResult)) {
            return NarrationPromptMode.CHECK_RESULT;
        }
        if (isCheckRequest(actionResult)) {
            return NarrationPromptMode.CHECK_REQUEST;
        }
        if (isEmoteActionResult(actionResult)) {
            return NarrationPromptMode.EMOTE;
        }
        if (colorEvent != null && !colorEvent.isBlank()) {
            return NarrationPromptMode.COLOR_EVENT;
        }
        if (!actionResult.isBlank()) {
            if (isLookDirectionCommand(canonicalCommand)) {
                return NarrationPromptMode.LOOK_DIRECTION;
            }
            if (isLookTargetCommand(canonicalCommand)) {
                return NarrationPromptMode.LOOK_TARGET;
            }
            return NarrationPromptMode.ACTION_RESULT;
        }
        return NarrationPromptMode.SCENE;
    }

    private static boolean isEmoteActionResult(String actionResult) {
        if (actionResult == null || actionResult.isBlank()) {
            return false;
        }
        String trimmed = actionResult.trim();
        if (trimmed.length() < EMOTE_PREFIX.length()) {
            return false;
        }
        return trimmed.regionMatches(true, 0, EMOTE_PREFIX, 0, EMOTE_PREFIX.length());
    }

    private static boolean isCheckRequest(String actionResult) {
        if (actionResult == null || actionResult.isBlank()) {
            return false;
        }
        String trimmed = actionResult.trim();
        if (trimmed.length() < CHECK_REQUEST_PREFIX.length()) {
            return false;
        }
        return trimmed.regionMatches(true, 0, CHECK_REQUEST_PREFIX, 0, CHECK_REQUEST_PREFIX.length());
    }

    private static boolean isCheckResult(String actionResult) {
        if (actionResult == null || actionResult.isBlank()) {
            return false;
        }
        String trimmed = actionResult.trim();
        if (trimmed.length() < CHECK_RESULT_PREFIX.length()) {
            return false;
        }
        return trimmed.regionMatches(true, 0, CHECK_RESULT_PREFIX, 0, CHECK_RESULT_PREFIX.length());
    }

    private static boolean containsExitsLine(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String[] lines = text.split("\\R", -1);
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("exits:")) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLookDirectionCommand(String canonicalCommand) {
        if (canonicalCommand == null || canonicalCommand.isBlank()) {
            return false;
        }
        List<Token> tokens = trimEolTokens(CommandScanner.scan(canonicalCommand));
        if (tokens.size() != 2) {
            return false;
        }
        Token first = tokens.get(0);
        Token second = tokens.get(1);
        if (first.type != TokenType.LOOK) {
            return false;
        }
        return isDirectionToken(second.type);
    }

    private static boolean isLookTargetCommand(String canonicalCommand) {
        if (canonicalCommand == null || canonicalCommand.isBlank()) {
            return false;
        }
        List<Token> tokens = trimEolTokens(CommandScanner.scan(canonicalCommand));
        if (tokens.isEmpty()) {
            return false;
        }
        if (tokens.get(0).type != TokenType.LOOK) {
            return false;
        }
        if (tokens.size() <= 1) {
            return false;
        }
        Token second = tokens.get(1);
        return !isDirectionToken(second.type);
    }

    private static boolean isDirectionToken(TokenType type) {
        return switch (type) {
            case NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST, UP, DOWN -> true;
            default -> false;
        };
    }

    private static List<Token> trimEolTokens(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<Token> trimmed = new java.util.ArrayList<>(tokens);
        if (trimmed.get(trimmed.size() - 1).type == TokenType.EOL) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }
}

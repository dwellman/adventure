package com.demo.adventure.engine.command.handlers;

import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Deterministic classic-mode fallback that only consults scanner tokens.
 * Intended for 1980 mode when the compiler does not recognize the input.
 */
public final class ClassicCommandFallback {
    private ClassicCommandFallback() {
    }

    public static String resolve(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        List<Token> tokens = trimEol(CommandScanner.scan(input));
        if (tokens.isEmpty()) {
            return null;
        }
        if (isLookAround(tokens) || isWhereAmI(tokens)) {
            return "look";
        }
        Token directionToken = firstDirectionToken(tokens);
        if (directionToken != null) {
            return "go " + directionToken.lexeme;
        }
        return null;
    }

    private static boolean isLookAround(List<Token> tokens) {
        if (tokens.size() == 1 && tokens.get(0).type == TokenType.LOOK) {
            return true;
        }
        if (tokens.size() == 2 && tokens.get(0).type == TokenType.LOOK) {
            return "around".equalsIgnoreCase(tokens.get(1).lexeme);
        }
        return false;
    }

    private static boolean isWhereAmI(List<Token> tokens) {
        List<Token> candidate = dropTrailingHelp(tokens);
        if (candidate.size() != 3) {
            return false;
        }
        String first = candidate.get(0).lexeme.toLowerCase(Locale.ROOT);
        String second = candidate.get(1).lexeme.toLowerCase(Locale.ROOT);
        String third = candidate.get(2).lexeme.toLowerCase(Locale.ROOT);
        return first.equals("where") && second.equals("am") && third.equals("i");
    }

    private static List<Token> dropTrailingHelp(List<Token> tokens) {
        if (tokens.isEmpty()) {
            return tokens;
        }
        Token last = tokens.get(tokens.size() - 1);
        if (last.type == TokenType.HELP && "?".equals(last.lexeme)) {
            return tokens.subList(0, tokens.size() - 1);
        }
        return tokens;
    }

    private static Token firstDirectionToken(List<Token> tokens) {
        for (Token token : tokens) {
            if (isDirection(token.type)) {
                return token;
            }
        }
        return null;
    }

    private static boolean isDirection(TokenType tokenType) {
        return switch (tokenType) {
            case NORTH,
                 NORTH_EAST,
                 EAST,
                 SOUTH_EAST,
                 SOUTH,
                 SOUTH_WEST,
                 WEST,
                 NORTH_WEST,
                 UP,
                 DOWN -> true;
            default -> false;
        };
    }

    private static List<Token> trimEol(List<Token> tokens) {
        if (tokens.isEmpty()) {
            return tokens;
        }
        List<Token> trimmed = new ArrayList<>(tokens);
        if (trimmed.get(trimmed.size() - 1).type == TokenType.EOL) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }
}

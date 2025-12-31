package com.demo.adventure.engine.command.interpreter;

import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandNode;
import com.demo.adventure.engine.command.CommandParseError;
import com.demo.adventure.engine.command.CommandPhrase;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class CommandCompiler {

    private static final EnumSet<TokenType> PREPOSITIONS = EnumSet.of(
            TokenType.IN,
            TokenType.INTO,
            TokenType.ON,
            TokenType.FROM,
            TokenType.WITH,
            TokenType.USING
    );

    private Map<String, TokenType> extraKeywords = Map.of();

    public void setExtraKeywords(Map<String, TokenType> extraKeywords) {
        this.extraKeywords = extraKeywords == null ? Map.of() : Map.copyOf(extraKeywords);
    }

    public CommandNode compile(String input) {
        String safeInput = input == null ? "" : input;
        List<Token> tokens = CommandScanner.scan(safeInput, extraKeywords);
        if (tokens.isEmpty()) {
            return new CommandNode.Unknown();
        }

        Token first = tokens.get(0);
        if (first.type == TokenType.EOL) {
            return new CommandNode.Unknown();
        }

        if (isDirection(first.type)) {
            CommandPhrase phrase = new CommandPhrase(first.lexeme, first.lexeme, null, null);
            return new CommandNode.Verb(CommandAction.GO, phrase);
        }

        CommandAction action = actionFor(first.type);
        if (action == null) {
            return new CommandNode.Unknown();
        }

        if (action == CommandAction.FLEE) {
            if (hasNonEol(tokens, 1) && !isOnlyAway(tokens, 1)) {
                return error("Unexpected input after " + first.lexeme, tokens.get(1), safeInput);
            }
            return new CommandNode.Verb(action, CommandPhrase.empty());
        }

        if (isNoArgVerb(action) && hasNonEol(tokens, 1)) {
            return error("Unexpected input after " + first.lexeme, tokens.get(1), safeInput);
        }

        if (action == CommandAction.GO) {
            if (isRunAway(tokens, first)) {
                return new CommandNode.Verb(CommandAction.FLEE, CommandPhrase.empty());
            }
            PhraseParseResult result = parsePhrase(tokens, 1, safeInput, true);
            if (result.error != null) {
                return new CommandNode.Error(result.error);
            }
            CommandPhrase phrase = result.phrase;
            String direction = findDirection(tokens, 1);
            if (direction != null && !direction.isBlank()) {
                phrase = new CommandPhrase(phrase.raw(), direction, phrase.preposition(), phrase.object());
            }
            if (phrase.raw().isBlank()) {
                return error("Go where?", first, safeInput);
            }
            return new CommandNode.Verb(action, phrase);
        }

        if (action == CommandAction.LOOK) {
            PhraseParseResult result = parsePhrase(tokens, 1, safeInput, false);
            if (result.error != null) {
                return new CommandNode.Error(result.error);
            }
            CommandPhrase phrase = normalizeLookPhrase(tokens, 1, result.phrase);
            return new CommandNode.Verb(action, phrase);
        }

        if (action == CommandAction.TALK) {
            PhraseParseResult result = parsePhrase(tokens, 1, safeInput, true);
            if (result.error != null) {
                return new CommandNode.Error(result.error);
            }
            if (result.phrase.raw().isBlank()) {
                return error("Talk to whom?", first, safeInput);
            }
            return new CommandNode.Verb(action, result.phrase);
        }

        PhraseParseResult result = parsePhrase(tokens, 1, safeInput, false);
        if (result.error != null) {
            return new CommandNode.Error(result.error);
        }
        return new CommandNode.Verb(action, result.phrase);
    }

    private boolean isDirection(TokenType type) {
        return switch (type) {
            case NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST, UP, DOWN -> true;
            default -> false;
        };
    }

    private boolean hasNonEol(List<Token> tokens, int startIdx) {
        for (int i = startIdx; i < tokens.size(); i++) {
            if (tokens.get(i).type == TokenType.EOL) {
                return false;
            }
            return true;
        }
        return false;
    }

    private CommandAction actionFor(TokenType type) {
        return switch (type) {
            case HELP -> CommandAction.HELP;
            case LOOK -> CommandAction.LOOK;
            case LISTEN -> CommandAction.LISTEN;
            case INVENTORY -> CommandAction.INVENTORY;
            case INSPECT -> CommandAction.INSPECT;
            case QUIT -> CommandAction.QUIT;
            case SEARCH -> CommandAction.EXPLORE;
            case HOW -> CommandAction.HOW;
            case MAKE -> CommandAction.CRAFT;
            case DICE -> CommandAction.DICE;
            case TAKE -> CommandAction.TAKE;
            case DROP -> CommandAction.DROP;
            case MOVE, GO -> CommandAction.GO;
            case OPEN -> CommandAction.OPEN;
            case USE -> CommandAction.USE;
            case STRIKE -> CommandAction.ATTACK;
            case FLEE -> CommandAction.FLEE;
            case PUT -> CommandAction.PUT;
            case TALK -> CommandAction.TALK;
            default -> null;
        };
    }

    private boolean isNoArgVerb(CommandAction action) {
        return switch (action) {
            case HELP, LISTEN, INVENTORY, QUIT -> true;
            default -> false;
        };
    }

    private boolean isOnlyAway(List<Token> tokens, int startIdx) {
        for (int i = startIdx; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type == TokenType.EOL) {
                return true;
            }
            if (token.type != TokenType.AWAY) {
                return false;
            }
        }
        return true;
    }

    private boolean isRunAway(List<Token> tokens, Token first) {
        if (first == null || !"run".equalsIgnoreCase(first.lexeme)) {
            return false;
        }
        if (!hasNonEol(tokens, 1)) {
            return true;
        }
        return isOnlyAway(tokens, 1);
    }

    private String findDirection(List<Token> tokens, int startIdx) {
        for (int i = startIdx; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type == TokenType.EOL) {
                break;
            }
            if (isDirection(token.type)) {
                return token.lexeme;
            }
        }
        return null;
    }

    private CommandNode.Error error(String message, Token token, String input) {
        int column = token == null ? 0 : token.column;
        return new CommandNode.Error(new CommandParseError(message, column, input));
    }

    private record PhraseParseResult(CommandPhrase phrase, CommandParseError error) {
    }

    private PhraseParseResult parsePhrase(
            List<Token> tokens,
            int startIdx,
            String input,
            boolean allowLeadingTo
    ) {
        List<Token> args = collectArgs(tokens, startIdx);
        String raw = join(args);
        if (args.isEmpty()) {
            return new PhraseParseResult(CommandPhrase.empty(), null);
        }

        int parseStart = 0;
        if (allowLeadingTo) {
            while (parseStart < args.size() && args.get(parseStart).type == TokenType.TO) {
                parseStart++;
            }
            if (parseStart >= args.size()) {
                Token token = args.get(args.size() - 1);
                return new PhraseParseResult(null, new CommandParseError(
                        "Expected destination after '" + token.lexeme + "'",
                        token.column,
                        input
                ));
            }
        }

        List<Token> parseTokens = args.subList(parseStart, args.size());
        int prepIndex = indexOfPreposition(parseTokens);
        if (prepIndex == -1) {
            String target = join(parseTokens);
            CommandPhrase phrase = new CommandPhrase(raw, target, null, null);
            return new PhraseParseResult(phrase, null);
        }

        if (prepIndex == 0) {
            Token token = parseTokens.get(0);
            return new PhraseParseResult(null, new CommandParseError(
                    "Expected target before '" + token.lexeme + "'",
                    token.column,
                    input
            ));
        }
        if (prepIndex == parseTokens.size() - 1) {
            Token token = parseTokens.get(prepIndex);
            return new PhraseParseResult(null, new CommandParseError(
                    "Expected object after '" + token.lexeme + "'",
                    token.column,
                    input
            ));
        }

        Token prepToken = parseTokens.get(prepIndex);
        String target = join(parseTokens.subList(0, prepIndex));
        String object = join(parseTokens.subList(prepIndex + 1, parseTokens.size()));
        CommandPhrase phrase = new CommandPhrase(raw, target, prepToken.lexeme.toLowerCase(Locale.ROOT), object);
        return new PhraseParseResult(phrase, null);
    }

    private List<Token> collectArgs(List<Token> tokens, int startIdx) {
        List<Token> args = new ArrayList<>();
        for (int i = startIdx; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.type == TokenType.EOL) {
                break;
            }
            args.add(token);
        }
        return args;
    }

    private int indexOfPreposition(List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            if (PREPOSITIONS.contains(tokens.get(i).type)) {
                return i;
            }
        }
        return -1;
    }

    private String join(List<Token> tokens) {
        StringBuilder sb = new StringBuilder();
        for (Token token : tokens) {
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(token.lexeme);
        }
        return sb.toString().trim();
    }

    private CommandPhrase normalizeLookPhrase(List<Token> tokens, int startIdx, CommandPhrase phrase) {
        List<Token> args = collectArgs(tokens, startIdx);
        if (args.isEmpty()) {
            return phrase;
        }
        Token first = args.get(0);
        if (first.type == TokenType.IDENTIFIER && isAroundAlias(first.lexeme)) {
            return CommandPhrase.empty();
        }
        return phrase;
    }

    private boolean isAroundAlias(String lexeme) {
        if (lexeme == null || lexeme.isBlank()) {
            return false;
        }
        String normalized = lexeme.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("around") || normalized.equals("arround");
    }
}

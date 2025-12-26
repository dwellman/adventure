package com.demo.adventure.engine.command.interpreter;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Lightweight scanner for player input strings. Tokenizes words, quoted strings,
 * and a small set of punctuation/keywords so downstream interpreters can extract
 * verb/object phrases (e.g., "PUT SHATTERED IPAD IN CANVAS BACKPACK").
 */
public final class CommandScanner {
    private final Map<String, TokenType> keywords = new HashMap<>();

    private final String source;
    private int current = 0;
    private final List<Token> tokens;

    public CommandScanner(String source) {
        this(source, Map.of());
    }

    public CommandScanner(String source, Map<String, TokenType> extraKeywords) {
        // IMPORTANT: Keep all verb/direction aliases centralized here so parsers don’t re-check lexemes.
        // Future additions (shortcuts, synonyms) belong here, not in downstream command parsing.
        loadDefaultKeywords();
        mergeKeywords(extraKeywords);
        this.source = source == null ? "" : source;
        this.tokens = scanTokens();
    }

    public static List<Token> scan(String commandString) {
        return new CommandScanner(commandString).getTokens();
    }

    public static List<Token> scan(String commandString, Map<String, TokenType> extraKeywords) {
        return new CommandScanner(commandString, extraKeywords).getTokens();
    }

    public List<Token> getTokens() {
        return this.tokens;
    }

    private List<Token> scanTokens() {
        List<Token> tokens = new ArrayList<>();
        while (!isAtEnd()) {
            int start = current;
            char c = advance();
            switch (c) {
                case ' ', '\t', '\r', '\n', ',', '.', '(', ')' -> {
                    // skip
                }
                case '"' -> tokens.add(readString(start));
                case '?' -> tokens.add(new Token(TokenType.HELP, String.valueOf(c), start));
                default -> {
                    if (isAlphaNumeric(c)) {
                        tokens.add(identifier(start));
                    }
                }
            }
        }
        tokens.add(new Token(TokenType.EOL, "", current));
        return tokens;
    }

    private Token readString(int start) {
        StringBuilder sb = new StringBuilder();
        while (!isAtEnd()) {
            char c = advance();
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        return new Token(TokenType.STRING, sb.toString(), start);
    }

    private Token identifier(int start) {
        int end = current;
        while (!isAtEnd() && isAlphaNumeric(peek())) {
            advance();
            end = current;
        }

        String lexeme = source.substring(start, end).trim();
        TokenType tokenType = keywords.get(lexeme.toUpperCase(Locale.ROOT));

        if (tokenType != null) {
            return new Token(tokenType, lexeme, start);
        }

        return new Token(TokenType.IDENTIFIER, lexeme, start);
    }

    private boolean isAlphaNumeric(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '-';
    }

    private char advance() {
        return source.charAt(current++);
    }

    private char peek() {
        if (isAtEnd()) return '\0';
        return source.charAt(current);
    }

    private boolean isAtEnd() {
        return current >= this.source.length();
    }

    private void loadDefaultKeywords() {
        // Actions
        keywords.put("EAT", TokenType.EAT);
        keywords.put("CLIMB", TokenType.MOVE);
        keywords.put("DROP", TokenType.DROP);
        keywords.put("GO", TokenType.MOVE);
        keywords.put("MOVE", TokenType.MOVE);
        keywords.put("GRAB", TokenType.TAKE);
        keywords.put("INSPECT", TokenType.INSPECT);
        keywords.put("INVENTORY", TokenType.INVENTORY);
        keywords.put("I", TokenType.INVENTORY);
        keywords.put("JUMP", TokenType.JUMP);
        keywords.put("L", TokenType.LOOK);
        keywords.put("LOOK", TokenType.LOOK);
        keywords.put("LISTEN", TokenType.LISTEN);
        keywords.put("MAKE", TokenType.MAKE);
        // Keep player verbs normalized: CRAFT and MAKE collapse to the same token so
        // downstream command handlers don't need to special-case synonyms.
        keywords.put("CRAFT", TokenType.MAKE);
        keywords.put("ATTACK", TokenType.STRIKE);
        keywords.put("OPEN", TokenType.OPEN);
        keywords.put("PUT", TokenType.PUT);
        keywords.put("SEARCH", TokenType.SEARCH);
        keywords.put("EXPLORE", TokenType.SEARCH);
        keywords.put("SOAK", TokenType.SOAK);
        keywords.put("STRIKE", TokenType.STRIKE);
        keywords.put("SWIM", TokenType.SWIM);
        keywords.put("TAKE", TokenType.TAKE);
        keywords.put("USE", TokenType.USE);
        keywords.put("HOW", TokenType.HOW);
        keywords.put("QUIT", TokenType.QUIT);
        keywords.put("FLEE", TokenType.FLEE);
        keywords.put("RUN", TokenType.MOVE);
        keywords.put("EXIT", TokenType.QUIT);
        keywords.put("Q", TokenType.QUIT);

        // Directions
        keywords.put("E", TokenType.EAST);
        keywords.put("EAST", TokenType.EAST);
        keywords.put("N", TokenType.NORTH);
        keywords.put("NE", TokenType.NORTH_EAST);
        keywords.put("NORTH", TokenType.NORTH);
        keywords.put("NORTH_EAST", TokenType.NORTH_EAST);
        keywords.put("NORTHEAST", TokenType.NORTH_EAST);
        keywords.put("NW", TokenType.NORTH_WEST);
        keywords.put("NORTH_WEST", TokenType.NORTH_WEST);
        keywords.put("NORTHWEST", TokenType.NORTH_WEST);
        keywords.put("S", TokenType.SOUTH);
        keywords.put("SE", TokenType.SOUTH_EAST);
        keywords.put("SOUTH", TokenType.SOUTH);
        keywords.put("SOUTH_EAST", TokenType.SOUTH_EAST);
        keywords.put("SOUTHEAST", TokenType.SOUTH_EAST);
        keywords.put("SOUTH_WEST", TokenType.SOUTH_WEST);
        keywords.put("SOUTHWEST", TokenType.SOUTH_WEST);
        keywords.put("SW", TokenType.SOUTH_WEST);
        keywords.put("W", TokenType.WEST);
        keywords.put("WEST", TokenType.WEST);
        keywords.put("U", TokenType.UP);
        keywords.put("UP", TokenType.UP);
        keywords.put("D", TokenType.DOWN);
        keywords.put("DOWN", TokenType.DOWN);

        // Keywords
        keywords.put("FROM", TokenType.FROM);
        keywords.put("HELP", TokenType.HELP);
        keywords.put("H", TokenType.HELP);
        keywords.put("?", TokenType.HELP);
        keywords.put("IN", TokenType.IN);
        keywords.put("INTO", TokenType.INTO);
        keywords.put("ON", TokenType.ON);
        keywords.put("TO", TokenType.TO);
        keywords.put("WITH", TokenType.WITH);
        keywords.put("USING", TokenType.USING);
        keywords.put("AWAY", TokenType.AWAY);
    }

    private void mergeKeywords(Map<String, TokenType> extraKeywords) {
        if (extraKeywords == null || extraKeywords.isEmpty()) {
            return;
        }
        for (Map.Entry<String, TokenType> entry : extraKeywords.entrySet()) {
            String key = entry.getKey();
            TokenType type = entry.getValue();
            if (key == null || key.isBlank() || type == null) {
                continue;
            }
            keywords.put(key.trim().toUpperCase(Locale.ROOT), type);
        }
    }
}

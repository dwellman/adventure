package com.demo.adventure.ai.runtime;

import java.util.List;

/**
 * Orchestrates translation of free-form input into game commands.
 * Pattern hooks: Grounding (scene context), Orchestration (deterministic parse), Verification (single-line output).
 */
public final class TranslatorService {
    private static final String EMOTE_PREFIX = "EMOTE:";

    @FunctionalInterface
    public interface CommandTranslationClient {
        String translate(String apiKey, String prompt) throws Exception;
    }

    private final boolean aiEnabled;
    private final String apiKey;
    private final String translatorTemplate;
    private final CommandTranslationClient translator;

    public TranslatorService(boolean aiEnabled, String apiKey) {
        this(aiEnabled, apiKey, CommandTranslator::translate);
    }

    TranslatorService(boolean aiEnabled, String apiKey, CommandTranslationClient translator) {
        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        String loaded = PromptTemplates.load("agents/translator.md");
        if (loaded == null || loaded.isBlank()) {
            throw new IllegalStateException("Missing translator prompt template: agents/translator.md");
        }
        this.translatorTemplate = loaded;
        this.translator = translator == null ? CommandTranslator::translate : translator;
    }

    // Pattern: Grounding
    // - Builds prompts from visible fixtures/items/inventory plus the last scene context to bound the model.
    // Pattern: Orchestration
    // - Coordinates prompt -> LLM call -> structured parse, with an AI-disabled bypass.
    // Pattern: Verification
    // - Returns explicit error rules when output violates the JSON contract.
    public TranslationResult translate(String playerText,
                                       List<String> fixtures,
                                       List<String> visibleItems,
                                       List<String> inventoryItems,
                                       String sceneContext) {
        if (!aiEnabled) {
            return new TranslationResult(TranslationResult.Type.COMMAND, playerText, null);
        }
        TranslationResult locationResult = resolveLocationQuestion(playerText);
        if (locationResult != null) {
            return locationResult;
        }
        TranslationResult identityResult = resolveIdentityQuestion(playerText);
        if (identityResult != null) {
            return identityResult;
        }
        try {
            String prompt = buildTranslatorPrompt(playerText, fixtures, visibleItems, inventoryItems, sceneContext);
            String result = translator.translate(apiKey, prompt);
            if (result == null || result.isBlank()) {
                return new TranslationResult(TranslationResult.Type.ERROR, null, "translator returned empty");
            }
            return parseTranslationOutput(result);
        } catch (Exception ex) {
            return new TranslationResult(TranslationResult.Type.ERROR, null, ex.getMessage());
        }
    }

    // Pattern: Verification
    // - Enforces a single-line command string output.
    private TranslationResult parseTranslationOutput(String raw) {
        if (raw == null) {
            return new TranslationResult(TranslationResult.Type.ERROR, null, "translator returned null");
        }
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0) {
            return new TranslationResult(TranslationResult.Type.ERROR, null, "Translator emitted multiple lines");
        }
        String line = raw.strip();
        if (line.isBlank()) {
            return new TranslationResult(TranslationResult.Type.ERROR, null, "Missing command");
        }
        if (isEmoteLine(line)) {
            String emoteText = stripEmotePrefix(line);
            if (emoteText.isBlank()) {
                return new TranslationResult(TranslationResult.Type.ERROR, null, "Missing emote");
            }
            return new TranslationResult(TranslationResult.Type.EMOTE, normalizeEmoteLine(emoteText), null);
        }
        return new TranslationResult(TranslationResult.Type.COMMAND, line, null);
    }

    // Pattern: Grounding
    // - Sanitizes player text, prior error, and last scene to keep prompt inputs bounded and consistent.
    private String buildTranslatorPrompt(String playerText,
                                         List<String> fixtures,
                                         List<String> visibleItems,
                                         List<String> inventoryItems,
                                         String sceneContext) {
        String safePlayer = playerText == null ? "" : playerText.replace("\n", " ").replace("\r", " ").trim();
        String scene = sceneContext == null ? "(none)" : sceneContext.replace("\r", " ").trim();
        if (scene.isBlank()) {
            scene = "(none)";
        }
        return translatorTemplate.formatted(
                joinOrNone(fixtures),
                joinOrNone(visibleItems),
                joinOrNone(inventoryItems),
                safePlayer,
                scene
        );
    }

    private String joinOrNone(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "(none)";
        }
        return String.join(", ", values);
    }

    public record TranslationResult(Type type, String command, String error) {
        public enum Type {COMMAND, EMOTE, ERROR}
    }

    private boolean isEmoteLine(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String trimmed = line.trim();
        if (trimmed.length() < EMOTE_PREFIX.length()) {
            return false;
        }
        return trimmed.regionMatches(true, 0, EMOTE_PREFIX, 0, EMOTE_PREFIX.length());
    }

    private String stripEmotePrefix(String line) {
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

    private String normalizeEmoteLine(String emoteText) {
        if (emoteText == null) {
            return EMOTE_PREFIX;
        }
        String trimmed = emoteText.trim();
        if (trimmed.isEmpty()) {
            return EMOTE_PREFIX;
        }
        return EMOTE_PREFIX + " " + trimmed;
    }

    private TranslationResult resolveLocationQuestion(String playerText) {
        if (playerText == null || playerText.isBlank()) {
            return null;
        }
        List<com.demo.adventure.engine.command.Token> tokens =
                trimEolTokens(com.demo.adventure.engine.command.interpreter.CommandScanner.scan(playerText));
        if (tokens.isEmpty()) {
            return null;
        }
        List<com.demo.adventure.engine.command.Token> candidate = dropTrailingHelp(tokens);
        if (candidate.size() != 3) {
            return null;
        }
        String first = normalizeLexeme(candidate.get(0).lexeme);
        String second = normalizeLexeme(candidate.get(1).lexeme);
        String third = normalizeLexeme(candidate.get(2).lexeme);
        if (!"where".equals(first)) {
            return null;
        }
        if (!("am".equals(second) || "are".equals(second))) {
            return null;
        }
        if (!("i".equals(third) || "we".equals(third))) {
            return null;
        }
        return new TranslationResult(TranslationResult.Type.COMMAND, "look", null);
    }

    private TranslationResult resolveIdentityQuestion(String playerText) {
        if (playerText == null || playerText.isBlank()) {
            return null;
        }
        List<com.demo.adventure.engine.command.Token> tokens =
                trimEolTokens(com.demo.adventure.engine.command.interpreter.CommandScanner.scan(playerText));
        if (tokens.isEmpty()) {
            return null;
        }
        List<com.demo.adventure.engine.command.Token> candidate = dropTrailingHelp(tokens);
        List<String> words = tokensToWords(candidate);
        if (words.size() < 3) {
            return null;
        }
        String first = normalizeLexeme(words.get(0));
        String second = normalizeLexeme(words.get(1));
        if (!"who".equals(first)) {
            return null;
        }
        if (!("is".equals(second) || "s".equals(second))) {
            return null;
        }
        String target = String.join(" ", words.subList(2, words.size())).trim();
        if (target.isBlank()) {
            return null;
        }
        return new TranslationResult(TranslationResult.Type.COMMAND, "look " + target, null);
    }

    private List<com.demo.adventure.engine.command.Token> dropTrailingHelp(List<com.demo.adventure.engine.command.Token> tokens) {
        if (tokens.isEmpty()) {
            return tokens;
        }
        com.demo.adventure.engine.command.Token last = tokens.get(tokens.size() - 1);
        if (last.type == com.demo.adventure.engine.command.TokenType.HELP && "?".equals(last.lexeme)) {
            return tokens.subList(0, tokens.size() - 1);
        }
        return tokens;
    }

    private List<com.demo.adventure.engine.command.Token> trimEolTokens(List<com.demo.adventure.engine.command.Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<com.demo.adventure.engine.command.Token> trimmed = new java.util.ArrayList<>(tokens);
        if (trimmed.get(trimmed.size() - 1).type == com.demo.adventure.engine.command.TokenType.EOL) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }

    private List<String> tokensToWords(List<com.demo.adventure.engine.command.Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<String> words = new java.util.ArrayList<>();
        for (com.demo.adventure.engine.command.Token token : tokens) {
            if (token == null || token.type == com.demo.adventure.engine.command.TokenType.EOL
                    || token.type == com.demo.adventure.engine.command.TokenType.HELP) {
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim();
            if (lexeme.isEmpty()) {
                continue;
            }
            if (token.type == com.demo.adventure.engine.command.TokenType.STRING) {
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

    private String normalizeLexeme(String lexeme) {
        return lexeme == null ? "" : lexeme.trim().toLowerCase(java.util.Locale.ROOT);
    }
}

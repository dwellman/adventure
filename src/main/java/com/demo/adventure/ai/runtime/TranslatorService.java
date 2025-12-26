package com.demo.adventure.ai.runtime;

import java.util.List;

/**
 * Orchestrates translation of free-form input into game commands.
 * Pattern hooks: Grounding (scene context), Orchestration (deterministic parse), Verification (single-line output).
 */
public final class TranslatorService {
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
        public enum Type {COMMAND, ERROR}
    }
}

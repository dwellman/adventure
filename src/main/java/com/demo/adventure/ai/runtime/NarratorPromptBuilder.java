package com.demo.adventure.ai.runtime;

import java.util.List;

public final class NarratorPromptBuilder {

    private NarratorPromptBuilder() {}

    public static String buildEngine(String playerUtterance,
                                     String canonicalCommand,
                                     String rawEngineOutput,
                                     String sceneSnapshot,
                                     String backstory,
                                     List<RecentAction> recentActions,
                                     List<RecentNarration> recentNarrations) {
        return buildPrompt(
                playerUtterance,
                canonicalCommand,
                rawEngineOutput,
                sceneSnapshot,
                "",
                backstory,
                recentActions,
                recentNarrations
        );
    }

    public static String buildSnapshot(String playerUtterance,
                                       String canonicalCommand,
                                       String sceneSnapshot,
                                       String colorEvent,
                                       String backstory,
                                       List<RecentAction> recentActions,
                                       List<RecentNarration> recentNarrations) {
        return buildPrompt(
                playerUtterance,
                canonicalCommand,
                "",
                sceneSnapshot,
                colorEvent,
                backstory,
                recentActions,
                recentNarrations
        );
    }

    private static String buildPrompt(String playerUtterance,
                                      String canonicalCommand,
                                      String rawEngineOutput,
                                      String sceneSnapshot,
                                      String colorEvent,
                                      String backstory,
                                      List<RecentAction> recentActions,
                                      List<RecentNarration> recentNarrations) {
        String raw = nullToEmpty(rawEngineOutput).trim();
        String snapshot = nullToEmpty(sceneSnapshot).trim();
        boolean rawHasExits = containsExitsLine(raw);
        String sceneSource = rawHasExits ? raw : snapshot;
        String actionResult = rawHasExits ? "" : raw;
        String color = nullToEmpty(colorEvent).trim();
        NarrationPromptMode mode = NarrationPromptSelector.select(canonicalCommand, raw, color);
        String sceneDetailLevel = mode == NarrationPromptMode.SCENE ? sceneDetailLevel(raw, snapshot) : "";

        StringBuilder sb = new StringBuilder();
        sb.append("Narrator Prompt v0.32 (rewrite)").append("\n\n");
        sb.append("MODE: ").append(mode.label()).append("\n");
        if (!sceneDetailLevel.isBlank()) {
            sb.append("SCENE_DETAIL_LEVEL: ").append(sceneDetailLevel).append("\n");
        }
        String voice = nullToEmpty(backstory).isBlank() ? "VOICE: neutral" : "VOICE: use BACKSTORY for tone only";
        sb.append(voice).append("\n");
        if (!nullToEmpty(playerUtterance).isBlank()) {
            sb.append("PLAYER_TEXT: ").append(playerUtterance.trim()).append("\n");
        }
        if (!nullToEmpty(canonicalCommand).isBlank()) {
            sb.append("LAST_COMMAND: ").append(canonicalCommand.trim()).append("\n");
        }
        boolean locationQuestion = mode == NarrationPromptMode.SCENE && isLocationQuestion(playerUtterance);
        if (locationQuestion) {
            sb.append("INTENT: LOCATION_QUESTION").append("\n");
        }
        sb.append("\n");
        boolean includeRecentActions = hasRecentActions(mode, recentActions);
        boolean includeRecentNarrations = hasRecentNarrations(mode, recentNarrations);
        sb.append("RULES").append("\n");
        sb.append("- Rewrite the provided source for readability and flow.").append("\n");
        sb.append("- Keep facts identical; do not add new entities, exits, locations, or mechanics.").append("\n");
        sb.append("- Paraphrase is allowed.").append("\n");
        sb.append("- If you mention a destination name or direction, keep wording exact.").append("\n");
        if (!nullToEmpty(playerUtterance).isBlank()) {
            sb.append("- PLAYER_TEXT is intent context only; do not add facts from it.").append("\n");
        }
        if (locationQuestion) {
            sb.append("- For a location question, name the location from SCENE_SOURCE and include 1-2 concrete details from it.").append("\n");
        }
        if (includeRecentActions) {
            sb.append("- RECENT_ACTIONS are continuity context only; do not introduce new facts.").append("\n");
        }
        if (includeRecentNarrations) {
            sb.append("- RECENT_NARRATION is continuity context only; do not introduce new facts.").append("\n");
        }
        sb.append("\n");
        sb.append("OUTPUT").append("\n");
        if (mode == NarrationPromptMode.EMOTE) {
            appendEmoteOutputRules(sb);
        } else if (mode == NarrationPromptMode.CHECK_REQUEST) {
            appendCheckRequestOutputRules(sb);
        } else if (mode == NarrationPromptMode.CHECK_RESULT) {
            appendCheckResultOutputRules(sb);
        } else {
            appendModeOutputRules(sb, mode);
            if (mode == NarrationPromptMode.SCENE) {
                sb.append("- After the paragraph, copy the Exits line exactly if present.").append("\n");
            }
        }
        sb.append("\n");
        sb.append("MARKDOWN").append("\n");
        sb.append("- Inline emphasis only: **bold** and _italic_.").append("\n");
        sb.append("- Do not add markdown to the exits footer line.").append("\n\n");
        if (!nullToEmpty(backstory).isBlank()) {
            sb.append("BEGIN_BACKSTORY").append("\n");
            sb.append(backstory.trim()).append("\n");
            sb.append("END_BACKSTORY").append("\n\n");
        }
        appendRecentActions(sb, mode, recentActions);
        appendRecentNarrations(sb, mode, recentNarrations);
        if (mode == NarrationPromptMode.SCENE && !sceneSource.isBlank()) {
            sb.append("BEGIN_SCENE_SOURCE").append("\n");
            sb.append(sceneSource).append("\n");
            sb.append("END_SCENE_SOURCE").append("\n");
        }
        if (!actionResult.isBlank()) {
            sb.append("\nBEGIN_ACTION_RESULT").append("\n");
            sb.append(actionResult).append("\n");
            sb.append("END_ACTION_RESULT").append("\n");
        }
        if (!color.isBlank()) {
            sb.append("\nBEGIN_COLOR_EVENT").append("\n");
            sb.append(color).append("\n");
            sb.append("END_COLOR_EVENT").append("\n");
        }
        return sb.toString();
    }

    private static void appendModeOutputRules(StringBuilder sb, NarrationPromptMode mode) {
        switch (mode) {
            case SCENE -> {
                sb.append("- 1 paragraph, 1-2 sentences.").append("\n");
                sb.append("- If SCENE_DETAIL_LEVEL=HEADER_ONLY, keep to 1 short sentence.").append("\n");
                sb.append("- Rewrite SCENE_SOURCE only.").append("\n");
                sb.append("- Do not list exits in the body.").append("\n");
            }
            case LOOK_TARGET -> {
                sb.append("- 1 paragraph, 1-2 sentences.").append("\n");
                sb.append("- Rewrite ACTION_RESULT only.").append("\n");
                sb.append("- Do not add scene-wide details or exits.").append("\n");
            }
            case LOOK_DIRECTION -> {
                sb.append("- 1 paragraph, 1-2 sentences.").append("\n");
                sb.append("- Rewrite ACTION_RESULT only (direction/gate answer).").append("\n");
                sb.append("- Keep direction words and destination names exact if used.").append("\n");
                sb.append("- Do not list other exits.").append("\n");
            }
            case ACTION_RESULT -> {
                sb.append("- 1 paragraph, 1-2 sentences.").append("\n");
                sb.append("- Rewrite ACTION_RESULT only.").append("\n");
                sb.append("- Do not add scene-wide details or exits.").append("\n");
            }
            case COLOR_EVENT -> {
                sb.append("- 1 paragraph, 1-2 sentences.").append("\n");
                sb.append("- Rewrite COLOR_EVENT only.").append("\n");
                sb.append("- Do not add scene-wide details or exits.").append("\n");
            }
            case EMOTE, CHECK_REQUEST, CHECK_RESULT -> {
                // Handled separately.
            }
        }
    }

    private static void appendEmoteOutputRules(StringBuilder sb) {
        sb.append("- Output exactly one paragraph, 1 sentence.").append("\n");
        sb.append("- Do not print the location sentence.").append("\n");
        sb.append("- Do not print the Exits line.").append("\n");
        sb.append("- Sentence 1: rewrite the EMOTE into DM narration; do not restate it verbatim or include \"EMOTE:\".").append("\n");
        sb.append("- Do not add scene details, new actors, or props beyond the EMOTE text.").append("\n");
        sb.append("- Do not mention weather, lighting, or atmosphere unless it appears in RECENT_ACTIONS.").append("\n");
        sb.append("- Do not use BACKSTORY unless the specific detail appears in RECENT_ACTIONS.").append("\n");
        sb.append("- Keep it concise.").append("\n");
    }

    private static void appendCheckRequestOutputRules(StringBuilder sb) {
        sb.append("- Output exactly one paragraph, 1-3 sentences.").append("\n");
        sb.append("- Do not print the location sentence.").append("\n");
        sb.append("- Do not print the Exits line.").append("\n");
        sb.append("- Sentence 1: rewrite the EMOTE into DM narration; do not restate it verbatim or include \"EMOTE:\".").append("\n");
        sb.append("- Sentence 2: \"Roll <dice call>.\" using the dice call exactly as written in CHECK_REQUEST.").append("\n");
        sb.append("- Optional: add one short color beat from BACKSTORY or RECENT_ACTIONS if it does not repeat a recent beat.").append("\n");
        sb.append("- Do not add scene details, new actors, or props beyond EMOTE/BACKSTORY/RECENT_ACTIONS.").append("\n");
        sb.append("- Keep it concise.").append("\n");
    }

    private static void appendCheckResultOutputRules(StringBuilder sb) {
        sb.append("- Output exactly one paragraph, 1-3 sentences.").append("\n");
        sb.append("- Do not print the location sentence.").append("\n");
        sb.append("- Do not print the Exits line.").append("\n");
        sb.append("- Sentence 1: narrate the EMOTE result based on CHECK_RESULT outcome (SUCCESS or FAIL).").append("\n");
        sb.append("- Do not restate roll numbers or dice calls.").append("\n");
        sb.append("- Optional: add one short color beat from BACKSTORY or RECENT_ACTIONS if it does not repeat a recent beat.").append("\n");
        sb.append("- Do not add scene details, new actors, or props beyond EMOTE/BACKSTORY/RECENT_ACTIONS.").append("\n");
        sb.append("- Keep it concise.").append("\n");
    }

    private static void appendRecentActions(StringBuilder sb, NarrationPromptMode mode, List<RecentAction> recentActions) {
        if (!hasRecentActions(mode, recentActions)) {
            return;
        }
        sb.append("RECENT_ACTIONS (engine-only, newest last)").append("\n");
        int index = 1;
        for (RecentAction action : recentActions) {
            if (action == null) {
                continue;
            }
            String command = nullToEmpty(action.command()).trim();
            String result = nullToEmpty(action.result()).trim();
            if (command.isEmpty() || result.isEmpty()) {
                continue;
            }
            sb.append(index).append(") COMMAND: ").append(command).append("\n");
            sb.append("   RESULT: ").append(result).append("\n");
            index++;
        }
        if (index > 1) {
            sb.append("\n");
        }
    }

    private static void appendRecentNarrations(StringBuilder sb, NarrationPromptMode mode, List<RecentNarration> recentNarrations) {
        if (!hasRecentNarrations(mode, recentNarrations)) {
            return;
        }
        sb.append("RECENT_NARRATION (newest last)").append("\n");
        int index = 1;
        for (RecentNarration narration : recentNarrations) {
            if (!isValidRecentNarration(narration)) {
                continue;
            }
            sb.append(index).append(") ").append(narration.text().trim()).append("\n");
            index++;
        }
        if (index > 1) {
            sb.append("\n");
        }
    }

    private static boolean hasRecentActions(NarrationPromptMode mode, List<RecentAction> recentActions) {
        if (mode != NarrationPromptMode.EMOTE
                && mode != NarrationPromptMode.CHECK_REQUEST
                && mode != NarrationPromptMode.CHECK_RESULT) {
            return false;
        }
        if (recentActions == null || recentActions.isEmpty()) {
            return false;
        }
        for (RecentAction action : recentActions) {
            if (isValidRecentAction(action)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasRecentNarrations(NarrationPromptMode mode, List<RecentNarration> recentNarrations) {
        if (mode != NarrationPromptMode.SCENE) {
            return false;
        }
        if (recentNarrations == null || recentNarrations.isEmpty()) {
            return false;
        }
        for (RecentNarration narration : recentNarrations) {
            if (isValidRecentNarration(narration)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidRecentAction(RecentAction action) {
        if (action == null) {
            return false;
        }
        String command = nullToEmpty(action.command()).trim();
        String result = nullToEmpty(action.result()).trim();
        return !command.isEmpty() && !result.isEmpty();
    }

    private static boolean isValidRecentNarration(RecentNarration narration) {
        if (narration == null) {
            return false;
        }
        String text = nullToEmpty(narration.text()).trim();
        return !text.isEmpty();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static boolean isLocationQuestion(String playerUtterance) {
        if (playerUtterance == null || playerUtterance.isBlank()) {
            return false;
        }
        List<com.demo.adventure.engine.command.Token> tokens =
                trimEolTokens(com.demo.adventure.engine.command.interpreter.CommandScanner.scan(playerUtterance));
        if (tokens.isEmpty()) {
            return false;
        }
        List<com.demo.adventure.engine.command.Token> candidate = dropTrailingHelp(tokens);
        if (candidate.size() != 3) {
            return false;
        }
        String first = normalizeLexeme(candidate.get(0).lexeme);
        String second = normalizeLexeme(candidate.get(1).lexeme);
        String third = normalizeLexeme(candidate.get(2).lexeme);
        if (!"where".equals(first)) {
            return false;
        }
        if (!("am".equals(second) || "are".equals(second))) {
            return false;
        }
        return "i".equals(third) || "we".equals(third);
    }

    private static List<com.demo.adventure.engine.command.Token> dropTrailingHelp(List<com.demo.adventure.engine.command.Token> tokens) {
        if (tokens.isEmpty()) {
            return tokens;
        }
        com.demo.adventure.engine.command.Token last = tokens.get(tokens.size() - 1);
        if (last.type == com.demo.adventure.engine.command.TokenType.HELP && "?".equals(last.lexeme)) {
            return tokens.subList(0, tokens.size() - 1);
        }
        return tokens;
    }

    private static List<com.demo.adventure.engine.command.Token> trimEolTokens(List<com.demo.adventure.engine.command.Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<com.demo.adventure.engine.command.Token> trimmed = new java.util.ArrayList<>(tokens);
        if (trimmed.get(trimmed.size() - 1).type == com.demo.adventure.engine.command.TokenType.EOL) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }

    private static String normalizeLexeme(String lexeme) {
        return lexeme == null ? "" : lexeme.trim().toLowerCase(java.util.Locale.ROOT);
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

    private static String sceneDetailLevel(String rawEngineOutput, String sceneSnapshot) {
        String source = rawEngineOutput != null && rawEngineOutput.contains("Exits:")
                ? rawEngineOutput
                : sceneSnapshot;
        if (source == null || source.isBlank()) {
            return "HEADER_ONLY";
        }
        String[] lines = source.split("\\R", -1);
        boolean foundLocation = false;
        for (String line : lines) {
            if (line == null) {
                continue;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (!foundLocation) {
                foundLocation = true;
                continue;
            }
            if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("exits:")) {
                continue;
            }
            return "FULL";
        }
        return "HEADER_ONLY";
    }
}

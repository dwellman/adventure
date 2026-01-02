package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.smart.SmartActorDecision;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

final class RuntimeConversation {
    private final GameRuntime runtime;
    private UUID conversationActorId;
    private String conversationActorLabel = "";

    RuntimeConversation(GameRuntime runtime) {
        this.runtime = runtime;
    }

    void reset() {
        conversationActorId = null;
        conversationActorLabel = "";
    }

    boolean isConversationActive() {
        return conversationActorId != null;
    }

    String conversationActorLabel() {
        return conversationActorLabel == null ? "" : conversationActorLabel;
    }

    GameRuntime.MentionResolution resolveMentionActor(List<String> tokens) {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        if (registry == null || plotId == null || tokens == null || tokens.isEmpty()) {
            return GameRuntime.MentionResolution.none();
        }
        List<String> mentionTokens = normalizeTokens(tokens);
        if (mentionTokens.isEmpty()) {
            return GameRuntime.MentionResolution.none();
        }
        List<Actor> actors = visibleActorsAtPlot(registry, plotId);
        if (actors.isEmpty()) {
            return GameRuntime.MentionResolution.none();
        }
        List<MentionCandidate> candidates = new ArrayList<>();
        for (Actor actor : actors) {
            int matchLength = mentionMatchLength(actor, mentionTokens);
            if (matchLength > 0) {
                candidates.add(new MentionCandidate(actor, matchLength));
            }
        }
        if (candidates.isEmpty()) {
            return GameRuntime.MentionResolution.none();
        }
        int best = candidates.stream().mapToInt(MentionCandidate::tokensMatched).max().orElse(0);
        List<MentionCandidate> top = candidates.stream()
                .filter(candidate -> candidate.tokensMatched() == best)
                .toList();
        if (top.size() != 1) {
            return GameRuntime.MentionResolution.ambiguous();
        }
        Actor actor = top.get(0).actor();
        String label = actor.getLabel();
        String safeLabel = label == null ? "" : label.trim();
        return new GameRuntime.MentionResolution(GameRuntime.MentionResolutionType.MATCH, actor.getId(), safeLabel, best);
    }

    void endConversation() {
        if (conversationActorId == null) {
            return;
        }
        conversationActorId = null;
        conversationActorLabel = "";
        runtime.narrate("You end the conversation.");
    }

    void talk(String target) {
        if (target == null || target.isBlank()) {
            runtime.narrate("Talk to whom?");
            return;
        }
        Actor actor = findVisibleActorByKeyOrLabel(runtime.registry(), runtime.currentPlotId(), target);
        if (actor == null) {
            runtime.narrate("You don't see " + target + " here.");
            return;
        }
        conversationActorId = actor.getId();
        String label = actor.getLabel();
        String safeLabel = label == null || label.isBlank() ? "someone" : label;
        conversationActorLabel = safeLabel;
        runtime.narrate("You turn to " + safeLabel + ".");
    }

    void talkToConversation(String playerUtterance) {
        if (conversationActorId == null) {
            runtime.narrate("No one is listening.");
            return;
        }
        KernelRegistry registry = runtime.registry();
        Actor actor = registry == null ? null : registry.get(conversationActorId) instanceof Actor found ? found : null;
        UUID plotId = runtime.currentPlotId();
        if (actor == null || !actor.isVisible() || plotId == null || !plotId.equals(actor.getOwnerId())) {
            conversationActorId = null;
            conversationActorLabel = "";
            runtime.narrate("No one answers.");
            return;
        }
        talkToActor(actor, playerUtterance);
    }

    Actor findVisibleActorByLabel(KernelRegistry registry, UUID plotId, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        String targetLower = label.trim().toLowerCase(Locale.ROOT);
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(a -> plotId != null && plotId.equals(a.getOwnerId()))
                .filter(a -> a.getLabel() != null && a.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
    }

    Actor findVisibleActorByKeyOrLabel(KernelRegistry registry, UUID plotId, String labelOrKey) {
        Actor actor = findVisibleActorByLabel(registry, plotId, labelOrKey);
        if (actor != null) {
            return actor;
        }
        UUID actorId = actorIdForKey(labelOrKey);
        if (actorId == null || registry == null || plotId == null) {
            return null;
        }
        Actor byId = registry.get(actorId) instanceof Actor found ? found : null;
        if (byId == null || !byId.isVisible() || !plotId.equals(byId.getOwnerId())) {
            return null;
        }
        return byId;
    }

    private void talkToActor(Actor actor, String playerUtterance) {
        if (actor == null) {
            runtime.narrate("No one answers.");
            return;
        }
        String label = actor.getLabel() == null || actor.getLabel().isBlank() ? "Someone" : actor.getLabel();
        if (!runtime.aiEnabled() || runtime.smartActorRuntime() == null || !runtime.smartActorRuntime().handlesActor(actor.getId())) {
            runtime.narrate(label + " has nothing to say.");
            return;
        }
        SmartActorDecision decision;
        try {
            decision = runtime.smartActorRuntime().respondToPlayer(runtime, actor.getId(), playerUtterance);
        } catch (GameBuilderException ex) {
            runtime.narrate(label + " has nothing to say.");
            return;
        }
        if (decision == null) {
            runtime.narrate(label + " has nothing to say.");
            return;
        }
        String reply = switch (decision.type()) {
            case COLOR -> decision.color();
            case UTTERANCE -> decision.utterance();
            case NONE -> "";
        };
        String cleaned = reply == null ? "" : reply.replace("\r", " ").replace("\n", " ").trim();
        if (cleaned.isBlank()) {
            runtime.narrate(label + " has nothing to say.");
            return;
        }
        runtime.narrate(label + ": " + cleaned);
    }

    private int mentionMatchLength(Actor actor, List<String> mentionTokens) {
        if (actor == null || mentionTokens == null || mentionTokens.isEmpty()) {
            return 0;
        }
        int best = 0;
        List<String> labelTokens = tokenizeLabel(actor.getLabel());
        if (!labelTokens.isEmpty()) {
            best = Math.max(best, longestPrefixMatch(mentionTokens, labelTokens));
            if (labelTokens.contains(mentionTokens.get(0))) {
                best = Math.max(best, 1);
            }
        }
        best = Math.max(best, keyMatchLength(actor.getId(), mentionTokens));
        return best;
    }

    private int longestPrefixMatch(List<String> left, List<String> right) {
        if (left == null || right == null) {
            return 0;
        }
        int max = Math.min(left.size(), right.size());
        int matched = 0;
        for (int i = 0; i < max; i++) {
            if (!left.get(i).equals(right.get(i))) {
                break;
            }
            matched = i + 1;
        }
        return matched;
    }

    private int keyMatchLength(UUID actorId, List<String> mentionTokens) {
        if (actorId == null || mentionTokens == null || mentionTokens.isEmpty()) {
            return 0;
        }
        int best = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mentionTokens.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(mentionTokens.get(i));
            UUID candidate = actorIdForKey(sb.toString());
            if (actorId.equals(candidate)) {
                best = i + 1;
            }
        }
        return best;
    }

    private List<String> normalizeTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String token : tokens) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalized.add(trimmed.toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    private List<String> tokenizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return List.of();
        }
        List<Token> tokens = CommandScanner.scan(label);
        List<String> words = new ArrayList<>();
        for (Token token : tokens) {
            if (token == null || token.type == TokenType.EOL || token.type == TokenType.HELP) {
                continue;
            }
            if (token.type == TokenType.STRING) {
                splitIntoWords(token.lexeme, words);
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim();
            if (!lexeme.isEmpty()) {
                words.add(lexeme.toLowerCase(Locale.ROOT));
            }
        }
        return words;
    }

    private void splitIntoWords(String lexeme, List<String> words) {
        if (lexeme == null || words == null) {
            return;
        }
        for (String part : lexeme.split("\\s+")) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                words.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
    }

    private UUID actorIdForKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(("actor:" + normalized).getBytes(StandardCharsets.UTF_8));
    }

    List<Actor> visibleActorsAtPlot(KernelRegistry registry, UUID plotId) {
        if (registry == null || plotId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(actor -> plotId.equals(actor.getOwnerId()))
                .toList();
    }

    private record MentionCandidate(Actor actor, int tokensMatched) {
    }
}

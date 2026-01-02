package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.smart.SmartActorContext;
import com.demo.adventure.ai.runtime.smart.SmartActorDecision;
import com.demo.adventure.ai.runtime.smart.SmartActorHistoryScope;
import com.demo.adventure.ai.runtime.smart.SmartActorHistoryStore;
import com.demo.adventure.ai.runtime.smart.SmartActorSpec;

final class SmartActorHistoryRecorder {
    private final SmartActorHistoryStore historyStore;
    private long dialogueSequence;

    SmartActorHistoryRecorder(SmartActorHistoryStore historyStore) {
        this.historyStore = historyStore;
    }

    void recordHistory(SmartActorSpec spec, SmartActorContext context, String text, int turnIndex) {
        if (spec == null || context == null || text == null) {
            return;
        }
        if (spec.history() == null || spec.history().storeKey().isBlank()) {
            return;
        }
        String id = spec.actorKey() + ":" + turnIndex;
        historyStore.append(
                spec.history().storeKey(),
                id,
                text,
                context.contextTags(),
                SmartActorHistoryScope.ACTOR,
                turnIndex,
                "smart-actor"
        );
    }

    void recordConversationHistory(
            SmartActorSpec spec,
            SmartActorContext context,
            String playerUtterance,
            SmartActorDecision decision,
            int turnIndex
    ) {
        if (spec == null || context == null || decision == null) {
            return;
        }
        if (spec.history() == null || spec.history().storeKey().isBlank()) {
            return;
        }
        long timestamp = nextDialogueTimestamp(turnIndex);
        String storeKey = spec.history().storeKey();
        String actorKey = spec.actorKey() == null ? "actor" : spec.actorKey();
        String utterance = playerUtterance == null ? "" : playerUtterance.trim();
        if (!utterance.isBlank()) {
            historyStore.append(
                    storeKey,
                    actorKey + ":player:" + timestamp,
                    "PLAYER: " + utterance,
                    context.contextTags(),
                    SmartActorHistoryScope.ACTOR,
                    timestamp,
                    "player"
            );
        }
        String reply = switch (decision.type()) {
            case COLOR -> decision.color();
            case UTTERANCE -> decision.utterance();
            case NONE -> "";
        };
        String cleaned = reply == null ? "" : reply.trim();
        if (!cleaned.isBlank()) {
            historyStore.append(
                    storeKey,
                    actorKey + ":reply:" + timestamp,
                    "REPLY: " + cleaned,
                    context.contextTags(),
                    SmartActorHistoryScope.ACTOR,
                    timestamp,
                    "smart-actor"
            );
        }
    }

    private long nextDialogueTimestamp(int turnIndex) {
        dialogueSequence++;
        return (turnIndex * 1000L) + dialogueSequence;
    }
}

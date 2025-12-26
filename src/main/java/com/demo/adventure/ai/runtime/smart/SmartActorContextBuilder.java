package com.demo.adventure.ai.runtime.smart;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class SmartActorContextBuilder {
    private static final int DEFAULT_HISTORY_LIMIT = 4;
    private static final Set<SmartActorHistoryScope> DEFAULT_SCOPES =
            EnumSet.of(SmartActorHistoryScope.ACTOR, SmartActorHistoryScope.PLOT, SmartActorHistoryScope.GLOBAL);

    private final SmartActorHistoryStore historyStore;
    private final int historyLimit;

    public SmartActorContextBuilder(SmartActorHistoryStore historyStore) {
        this(historyStore, DEFAULT_HISTORY_LIMIT);
    }

    public SmartActorContextBuilder(SmartActorHistoryStore historyStore, int historyLimit) {
        if (historyStore == null) {
            throw new IllegalArgumentException("historyStore is required");
        }
        this.historyStore = historyStore;
        this.historyLimit = Math.max(0, historyLimit);
    }

    public SmartActorContext build(SmartActorSpec spec, SmartActorContextInput input) {
        if (spec == null) {
            throw new IllegalArgumentException("spec is required");
        }
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }
        Set<String> contextTags = mergeTags(input.plotTags(), input.itemTags(), input.questTags());
        List<SmartActorHistoryEntry> historySnippets = retrieveHistory(spec, contextTags);
        return new SmartActorContext(spec, input.actorId(), input.plotId(), contextTags, historySnippets);
    }

    private List<SmartActorHistoryEntry> retrieveHistory(SmartActorSpec spec, Set<String> contextTags) {
        SmartActorHistorySpec historySpec = spec.history();
        if (historySpec == null || historySpec.storeKey().isBlank()) {
            return List.of();
        }
        return historyStore.retrieve(historySpec.storeKey(), DEFAULT_SCOPES, contextTags, historyLimit);
    }

    private static Set<String> mergeTags(Set<String> plotTags, Set<String> itemTags, Set<String> questTags) {
        Set<String> merged = new LinkedHashSet<>();
        if (plotTags != null) {
            merged.addAll(plotTags);
        }
        if (itemTags != null) {
            merged.addAll(itemTags);
        }
        if (questTags != null) {
            merged.addAll(questTags);
        }
        return Set.copyOf(merged);
    }
}

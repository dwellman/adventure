package com.demo.adventure.ai.runtime.smart;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class SmartActorHistoryStore {
    private static final String SEED_SOURCE = "seed";

    private final Map<String, List<SmartActorHistoryEntry>> entriesByStore = new LinkedHashMap<>();

    public void seedFromSpec(SmartActorSpec spec) {
        if (spec == null || spec.history() == null) {
            return;
        }
        seed(spec.history().storeKey(), spec.history().seeds());
    }

    public void seed(String storeKey, List<SmartActorHistorySeed> seeds) {
        String normalizedKey = normalizeStoreKey(storeKey);
        if (normalizedKey.isBlank() || seeds == null || seeds.isEmpty()) {
            return;
        }
        List<SmartActorHistoryEntry> entries = entriesByStore.computeIfAbsent(normalizedKey, key -> new ArrayList<>());
        for (SmartActorHistorySeed seed : seeds) {
            if (seed == null) {
                continue;
            }
            entries.add(new SmartActorHistoryEntry(
                    seed.id(),
                    seed.text(),
                    seed.tags(),
                    seed.scope(),
                    0L,
                    SEED_SOURCE,
                    true
            ));
        }
    }

    public void append(String storeKey, SmartActorHistoryEntry entry) {
        if (entry == null) {
            return;
        }
        String normalizedKey = normalizeStoreKey(storeKey);
        if (normalizedKey.isBlank()) {
            return;
        }
        entriesByStore.computeIfAbsent(normalizedKey, key -> new ArrayList<>()).add(entry);
    }

    public void append(String storeKey,
                       String id,
                       String text,
                       Set<String> tags,
                       SmartActorHistoryScope scope,
                       long timestamp,
                       String source) {
        append(storeKey, new SmartActorHistoryEntry(id, text, tags, scope, timestamp, source, false));
    }

    public List<SmartActorHistoryEntry> retrieve(String storeKey,
                                                 Set<SmartActorHistoryScope> scopes,
                                                 Set<String> contextTags,
                                                 int limit) {
        String normalizedKey = normalizeStoreKey(storeKey);
        if (normalizedKey.isBlank()) {
            return List.of();
        }
        List<SmartActorHistoryEntry> entries = entriesByStore.get(normalizedKey);
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        Set<SmartActorHistoryScope> scopeFilter = normalizeScopes(scopes);
        Set<String> normalizedTags = SmartActorTags.normalize(contextTags);
        boolean filterByTags = !normalizedTags.isEmpty();

        List<SmartActorHistoryEntry> pinned = new ArrayList<>();
        List<SmartActorHistoryEntry> candidates = new ArrayList<>();
        Map<SmartActorHistoryEntry, Integer> tagMatches = new IdentityHashMap<>();
        for (SmartActorHistoryEntry entry : entries) {
            if (entry == null || !scopeFilter.contains(entry.scope())) {
                continue;
            }
            if (entry.pinned()) {
                pinned.add(entry);
                continue;
            }
            int matches = countTagMatches(entry.tags(), normalizedTags);
            if (filterByTags && matches == 0) {
                continue;
            }
            candidates.add(entry);
            tagMatches.put(entry, matches);
        }

        List<SmartActorHistoryEntry> ranked = rankCandidates(candidates, tagMatches);
        List<SmartActorHistoryEntry> limited = limitCandidates(ranked, limit);
        return mergePinnedAndRanked(pinned, limited);
    }

    private static List<SmartActorHistoryEntry> rankCandidates(List<SmartActorHistoryEntry> candidates,
                                                               Map<SmartActorHistoryEntry, Integer> tagMatches) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<SmartActorHistoryEntry> recencySorted = new ArrayList<>(candidates);
        recencySorted.sort(Comparator
                .comparingLong(SmartActorHistoryEntry::timestamp)
                .reversed()
                .thenComparing(SmartActorHistoryEntry::id));
        Map<SmartActorHistoryEntry, Integer> recencyRanks = new IdentityHashMap<>();
        int size = recencySorted.size();
        for (int i = 0; i < size; i++) {
            recencyRanks.put(recencySorted.get(i), size - i);
        }

        List<ScoredEntry> scored = new ArrayList<>();
        for (SmartActorHistoryEntry entry : candidates) {
            int matchCount = Objects.requireNonNullElse(tagMatches.get(entry), 0);
            int rank = Objects.requireNonNullElse(recencyRanks.get(entry), 0);
            int score = matchCount * 10 + rank;
            scored.add(new ScoredEntry(entry, score));
        }

        scored.sort(Comparator
                .comparingInt(ScoredEntry::score)
                .reversed()
                .thenComparing(e -> e.entry().timestamp(), Comparator.reverseOrder())
                .thenComparing(e -> e.entry().id()));

        List<SmartActorHistoryEntry> ranked = new ArrayList<>();
        for (ScoredEntry entry : scored) {
            ranked.add(entry.entry());
        }
        return ranked;
    }

    private static List<SmartActorHistoryEntry> limitCandidates(List<SmartActorHistoryEntry> ranked, int limit) {
        if (limit <= 0 || ranked.isEmpty()) {
            return List.of();
        }
        if (ranked.size() <= limit) {
            return ranked;
        }
        return ranked.subList(0, limit);
    }

    private static List<SmartActorHistoryEntry> mergePinnedAndRanked(List<SmartActorHistoryEntry> pinned,
                                                                     List<SmartActorHistoryEntry> ranked) {
        if (pinned.isEmpty() && ranked.isEmpty()) {
            return List.of();
        }
        List<SmartActorHistoryEntry> combined = new ArrayList<>(pinned.size() + ranked.size());
        Map<String, SmartActorHistoryEntry> seen = new LinkedHashMap<>();
        for (SmartActorHistoryEntry entry : pinned) {
            if (entry != null) {
                seen.putIfAbsent(entry.id(), entry);
            }
        }
        for (SmartActorHistoryEntry entry : ranked) {
            if (entry != null) {
                seen.putIfAbsent(entry.id(), entry);
            }
        }
        combined.addAll(seen.values());
        return List.copyOf(combined);
    }

    private static int countTagMatches(Set<String> entryTags, Set<String> contextTags) {
        if (entryTags == null || entryTags.isEmpty() || contextTags == null || contextTags.isEmpty()) {
            return 0;
        }
        int matches = 0;
        for (String tag : entryTags) {
            if (contextTags.contains(tag)) {
                matches++;
            }
        }
        return matches;
    }

    private static Set<SmartActorHistoryScope> normalizeScopes(Set<SmartActorHistoryScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            return EnumSet.allOf(SmartActorHistoryScope.class);
        }
        return EnumSet.copyOf(scopes);
    }

    private static String normalizeStoreKey(String storeKey) {
        if (storeKey == null) {
            return "";
        }
        return storeKey.trim().toLowerCase(Locale.ROOT);
    }

    private record ScoredEntry(SmartActorHistoryEntry entry, int score) {
    }
}

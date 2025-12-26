package com.demo.adventure.ai.runtime.smart;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorHistoryStoreTest {

    @Test
    void includesPinnedSeedsFirstAndRanksCandidates() {
        SmartActorHistoryStore store = new SmartActorHistoryStore();
        store.seed("mansion:butler", List.of(
                new SmartActorHistorySeed("pinned-1", "Pinned A", SmartActorHistoryScope.ACTOR, Set.of("CLUE")),
                new SmartActorHistorySeed("pinned-2", "Pinned B", SmartActorHistoryScope.PLOT, Set.of("ALIBI"))
        ));
        store.append("mansion:butler", entry("e1", "Clue note", Set.of("CLUE"), SmartActorHistoryScope.PLOT, 10));
        store.append("mansion:butler", entry("e2", "Older clue", Set.of("CLUE", "MOTIVE"), SmartActorHistoryScope.PLOT, 5));
        store.append("mansion:butler", entry("e3", "Other", Set.of("OTHER"), SmartActorHistoryScope.PLOT, 20));

        List<SmartActorHistoryEntry> result = store.retrieve(
                "mansion:butler",
                Set.of(SmartActorHistoryScope.ACTOR, SmartActorHistoryScope.PLOT, SmartActorHistoryScope.GLOBAL),
                Set.of("clue"),
                2
        );

        assertThat(result).extracting(SmartActorHistoryEntry::id)
                .containsExactly("pinned-1", "pinned-2", "e1", "e2");
    }

    @Test
    void fallsBackToRecencyWhenNoTags() {
        SmartActorHistoryStore store = new SmartActorHistoryStore();
        store.append("mansion:heiress", entry("e1", "First", Set.of("A"), SmartActorHistoryScope.ACTOR, 1));
        store.append("mansion:heiress", entry("e2", "Second", Set.of("B"), SmartActorHistoryScope.ACTOR, 3));
        store.append("mansion:heiress", entry("e3", "Third", Set.of("C"), SmartActorHistoryScope.ACTOR, 2));

        List<SmartActorHistoryEntry> result = store.retrieve(
                "mansion:heiress",
                Set.of(SmartActorHistoryScope.ACTOR),
                Set.of(),
                2
        );

        assertThat(result).extracting(SmartActorHistoryEntry::id)
                .containsExactly("e2", "e3");
    }

    @Test
    void filtersByScope() {
        SmartActorHistoryStore store = new SmartActorHistoryStore();
        store.append("mansion:chef", entry("e1", "Global", Set.of("CLUE"), SmartActorHistoryScope.GLOBAL, 2));
        store.append("mansion:chef", entry("e2", "Actor", Set.of("CLUE"), SmartActorHistoryScope.ACTOR, 3));

        List<SmartActorHistoryEntry> result = store.retrieve(
                "mansion:chef",
                Set.of(SmartActorHistoryScope.GLOBAL),
                Set.of("clue"),
                5
        );

        assertThat(result).extracting(SmartActorHistoryEntry::id)
                .containsExactly("e1");
    }

    private static SmartActorHistoryEntry entry(String id,
                                                String text,
                                                Set<String> tags,
                                                SmartActorHistoryScope scope,
                                                long timestamp) {
        return new SmartActorHistoryEntry(id, text, tags, scope, timestamp, "test", false);
    }
}

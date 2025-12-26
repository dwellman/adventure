package com.demo.adventure.ai.runtime.smart;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorContextBuilderTest {

    @Test
    void mergesTagsAndRetrievesHistory() {
        SmartActorHistoryStore store = new SmartActorHistoryStore();
        store.seed("mansion:butler", List.of(
                new SmartActorHistorySeed("seed-1", "Pinned", SmartActorHistoryScope.PLOT, Set.of("SEED"))
        ));
        store.append("mansion:butler", entry("e1", "Clue note", Set.of("CLUE"), SmartActorHistoryScope.PLOT, 10));
        store.append("mansion:butler", entry("e2", "Other", Set.of("OTHER"), SmartActorHistoryScope.PLOT, 11));

        SmartActorSpec spec = new SmartActorSpec(
                "butler",
                "smart-actor-system",
                "",
                Map.of(),
                Map.of(),
                List.of(),
                new SmartActorHistorySpec("mansion:butler", List.of()),
                SmartActorPolicy.empty()
        );

        SmartActorContextInput input = new SmartActorContextInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Set.of("plot"),
                Set.of("clue"),
                Set.of()
        );

        SmartActorContextBuilder builder = new SmartActorContextBuilder(store, 3);
        SmartActorContext context = builder.build(spec, input);

        assertThat(context.contextTags()).containsExactlyInAnyOrder("PLOT", "CLUE");
        assertThat(context.historySnippets()).extracting(SmartActorHistoryEntry::id)
                .containsExactly("seed-1", "e1");
    }

    @Test
    void handlesMissingHistorySpec() {
        SmartActorHistoryStore store = new SmartActorHistoryStore();
        SmartActorSpec spec = new SmartActorSpec(
                "butler",
                "smart-actor-system",
                "",
                Map.of(),
                Map.of(),
                List.of(),
                null,
                SmartActorPolicy.empty()
        );
        SmartActorContextInput input = new SmartActorContextInput(
                UUID.randomUUID(),
                UUID.randomUUID(),
                Set.of(),
                Set.of(),
                Set.of()
        );

        SmartActorContextBuilder builder = new SmartActorContextBuilder(store, 3);
        SmartActorContext context = builder.build(spec, input);

        assertThat(context.historySnippets()).isEmpty();
    }

    private static SmartActorHistoryEntry entry(String id,
                                                String text,
                                                Set<String> tags,
                                                SmartActorHistoryScope scope,
                                                long timestamp) {
        return new SmartActorHistoryEntry(id, text, tags, scope, timestamp, "test", false);
    }
}

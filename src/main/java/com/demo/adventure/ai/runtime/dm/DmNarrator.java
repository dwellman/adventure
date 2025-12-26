package com.demo.adventure.ai.runtime.dm;

import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.engine.mechanics.ops.Describe;
import com.demo.adventure.engine.mechanics.ops.Look;

import java.util.List;
import java.util.Objects;

/**
 * Narration helper that wraps deterministic Describe output with an optional DM agent rewrite.
 */
public final class DmNarrator {
    private final DmAgent agent;

    public DmNarrator() {
        this(null);
    }

    public DmNarrator(DmAgent agent) {
        this.agent = agent;
    }

    /**
     * Describe a target deterministically, then optionally rewrite the line via a DM agent.
     *
     * @param target           thing to describe
     * @param fixtures         fixtures owned by the target
     * @param containerEntries contents when the target is open
     * @return narration text
     */
    // Pattern: Grounding + Trust UX
    // - Always start from the deterministic Describe output and fall back to it on any AI failure.
    public String describe(Thing target, List<Thing> fixtures, List<Look.ContainerEntry> containerEntries) {
        String base = Describe.describe(target, fixtures, containerEntries);
        if (agent == null) {
            return base;
        }
        DmAgentContext context = buildContext(base, target, fixtures, containerEntries);
        try {
            String rewritten = agent.narrate(context);
            if (rewritten == null || rewritten.isBlank()) {
                return base;
            }
            return rewritten;
        } catch (Exception ex) {
            return base;
        }
    }

    private static DmAgentContext buildContext(
            String base,
            Thing target,
            List<Thing> fixtures,
            List<Look.ContainerEntry> containerEntries
    ) {
        String targetLabel = target == null ? "" : Objects.toString(target.getLabel(), "");
        String targetDescription = target == null ? "" : Objects.toString(target.getDescription(), "");
        List<String> fixtureSummaries = safeStream(fixtures)
                .filter(Thing::isVisible)
                .map(f -> Objects.toString(f.getLabel(), "") + ": " + Objects.toString(f.getDescription(), ""))
                .toList();
        List<String> inventorySummaries = safeStream(containerEntries)
                .filter(e -> e.thing() != null && e.thing().isVisible())
                .map(e -> Objects.toString(e.thing().getLabel(), "") + ": " + Objects.toString(e.thing().getDescription(), ""))
                .toList();
        return new DmAgentContext(base, targetLabel, targetDescription, fixtureSummaries, inventorySummaries);
    }

    private static <T> java.util.stream.Stream<T> safeStream(List<T> list) {
        if (list == null) {
            return java.util.stream.Stream.empty();
        }
        return list.stream().filter(Objects::nonNull);
    }
}

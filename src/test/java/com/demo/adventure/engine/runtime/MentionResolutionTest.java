package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotKind;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MentionResolutionTest {

    private GameRuntime runtime;
    private KernelRegistry registry;
    private UUID plotId;

    @BeforeEach
    void setUp() {
        registry = new KernelRegistry();
        plotId = UUID.randomUUID();
        Plot plot = new Plot(plotId, "Hall", "", PlotKind.LAND, null, null, KernelRegistry.MILIARIUM);
        registry.register(plot);

        Actor butler = new Actor(actorIdForKey("butler"), "Elias Crane", "", plotId);
        registry.register(butler);

        runtime = new GameRuntime(null, text -> {}, false);
        runtime.configure(
                registry,
                plotId,
                UUID.randomUUID(),
                new ArrayList<>(),
                new HashMap<>(),
                null,
                null,
                Map.<String, CraftingRecipe>of(),
                Map.of()
        );
    }

    @Test
    void resolvesSingleTokenMention() {
        MentionResolution result = runtime.resolveMentionActor(List.of("Elias"));

        assertThat(result.type()).isEqualTo(MentionResolutionType.MATCH);
        assertThat(result.actorLabel()).isEqualTo("Elias Crane");
        assertThat(result.tokensMatched()).isEqualTo(1);
    }

    @Test
    void resolvesFullLabelPrefixMention() {
        MentionResolution result = runtime.resolveMentionActor(List.of("Elias", "Crane", "what"));

        assertThat(result.type()).isEqualTo(MentionResolutionType.MATCH);
        assertThat(result.actorLabel()).isEqualTo("Elias Crane");
        assertThat(result.tokensMatched()).isEqualTo(2);
    }

    @Test
    void flagsAmbiguousSingleTokenMention() {
        Actor second = new Actor(actorIdForKey("assistant"), "Elias Reed", "", plotId);
        registry.register(second);

        MentionResolution result = runtime.resolveMentionActor(List.of("Elias"));

        assertThat(result.type()).isEqualTo(MentionResolutionType.AMBIGUOUS);
    }

    @Test
    void resolvesActorKeyMention() {
        MentionResolution result = runtime.resolveMentionActor(List.of("butler"));

        assertThat(result.type()).isEqualTo(MentionResolutionType.MATCH);
        assertThat(result.actorLabel()).isEqualTo("Elias Crane");
        assertThat(result.tokensMatched()).isEqualTo(1);
    }

    private UUID actorIdForKey(String key) {
        String normalized = key == null ? "" : key.trim().toLowerCase();
        return UUID.nameUUIDFromBytes(("actor:" + normalized).getBytes(StandardCharsets.UTF_8));
    }
}

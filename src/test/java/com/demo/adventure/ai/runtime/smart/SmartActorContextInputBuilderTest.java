package com.demo.adventure.ai.runtime.smart;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorContextInputBuilderTest {

    @Test
    void collectsPlotAndItemTagsFromRegistry() throws Exception {
        SmartActorTagIndex index = SmartActorTagLoader.load(Path.of("src/test/resources/smart/actor-tags.yaml"));
        KernelRegistry registry = new KernelRegistry();

        UUID plotId = SmartActorIdCodec.uuid("plot", "ballroom");
        UUID actorId = SmartActorIdCodec.uuid("actor", "butler");
        UUID itemId = SmartActorIdCodec.uuid("item", "candlestick");
        UUID fixtureId = SmartActorIdCodec.uuid("fixture", "study-desk");

        Item item = new ItemBuilder()
                .withId(itemId)
                .withLabel("Candlestick")
                .withDescription("Test")
                .withOwnerId(plotId)
                .withVisible(true)
                .build();
        Item fixture = new ItemBuilder()
                .withId(fixtureId)
                .withLabel("Study Desk")
                .withDescription("Test")
                .withOwnerId(plotId)
                .withVisible(true)
                .withFixture(true)
                .build();
        registry.register(item);
        registry.register(fixture);

        SmartActorContextInputBuilder builder = new SmartActorContextInputBuilder(index);
        SmartActorContextInput input = builder.build(registry, actorId, plotId, Set.of("QUEST_MAIN"));

        assertThat(input.plotTags()).containsExactlyInAnyOrder("BALLROOM", "CLUE_SCENE");
        assertThat(input.itemTags()).containsExactlyInAnyOrder("WEAPON", "SEARCHABLE");
        assertThat(input.questTags()).containsExactlyInAnyOrder("QUEST_MAIN");
    }
}

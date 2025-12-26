package com.demo.adventure.ai.runtime.smart;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorTagLoaderTest {

    @Test
    void loadsTagsByKind() throws Exception {
        Path path = Path.of("src/test/resources/smart/actor-tags.yaml");
        SmartActorTagIndex index = SmartActorTagLoader.load(path);

        UUID plotId = SmartActorIdCodec.uuid("plot", "ballroom");
        UUID itemId = SmartActorIdCodec.uuid("item", "candlestick");
        UUID fixtureId = SmartActorIdCodec.uuid("fixture", "study-desk");
        UUID actorId = SmartActorIdCodec.uuid("actor", "butler");

        assertThat(index.tagsForPlot(plotId)).containsExactlyInAnyOrder("BALLROOM", "CLUE_SCENE");
        assertThat(index.tagsForItem(itemId)).containsExactlyInAnyOrder("WEAPON");
        assertThat(index.tagsForFixture(fixtureId)).containsExactlyInAnyOrder("SEARCHABLE");
        assertThat(index.tagsForActor(actorId)).containsExactlyInAnyOrder("SUSPECT");
        assertThat(index.tagsForQuestKey("main-case")).containsExactlyInAnyOrder("QUEST_MAIN");
    }
}

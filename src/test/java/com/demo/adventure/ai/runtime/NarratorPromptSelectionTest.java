package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NarratorPromptSelectionTest {

    @Test
    void buildEngineUsesSceneSourceOnlyForSceneMode() {
        String raw = "Hallway\nExits: north";
        String snapshot = "Garden\nExits: south";
        String prompt = NarratorPromptBuilder.buildEngine(
                "look",
                "look",
                raw,
                snapshot,
                "Backstory",
                List.of(new RecentAction("look", "You scan the hall.")),
                List.of(new RecentNarration("A hush settles over the corridor."))
        );

        assertThat(prompt).contains(raw);
        assertThat(prompt).contains("MODE: SCENE");
        assertThat(prompt).contains("BEGIN_SCENE_SOURCE");
        assertThat(prompt).contains("SCENE_DETAIL_LEVEL");
        assertThat(prompt).contains("RECENT_NARRATION");
        assertThat(prompt).doesNotContain(snapshot);
        assertThat(prompt).doesNotContain("RECENT_ACTIONS");
    }

    @Test
    void buildSnapshotUsesColorOnly() {
        String snapshot = "Cave\nExits: east";
        String color = "A torch sputters.";
        String prompt = NarratorPromptBuilder.buildSnapshot(
                "look",
                "look",
                snapshot,
                color,
                "Backstory",
                List.of(new RecentAction("look", "You scan the cave.")),
                List.of(new RecentNarration("Water drips in the dark."))
        );

        assertThat(prompt).contains(color);
        assertThat(prompt).contains("MODE: COLOR_EVENT");
        assertThat(prompt).contains("BEGIN_COLOR_EVENT");
        assertThat(prompt).doesNotContain("BEGIN_SCENE_SOURCE");
        assertThat(prompt).doesNotContain("RECENT_ACTIONS");
        assertThat(prompt).doesNotContain("RECENT_NARRATION");
        assertThat(prompt).doesNotContain("SCENE_DETAIL_LEVEL");
        assertThat(prompt).doesNotContain(snapshot);
    }

    @Test
    void buildEngineEmoteModeOmitsLocationAndExitsRules() {
        String raw = "EMOTE: Do a little dance.";
        String snapshot = "Hall\nExits: north";
        String prompt = NarratorPromptBuilder.buildEngine(
                "Do a little dance.",
                "",
                raw,
                snapshot,
                "Backstory",
                List.of(new RecentAction("look", "You scan the hall.")),
                List.of(new RecentNarration("A hush settles over the corridor."))
        );

        assertThat(prompt).contains("MODE: EMOTE");
        assertThat(prompt).contains("Sentence 1: rewrite the EMOTE");
        assertThat(prompt).contains("RECENT_ACTIONS");
        assertThat(prompt).doesNotContain("RECENT_NARRATION");
    }

    @Test
    void buildEngineCheckRequestModeOmitsLocationAndExitsRules() {
        String raw = "CHECK_REQUEST: dice(20,15) | EMOTE: Do a little dance.";
        String snapshot = "Hall\nExits: north";
        String prompt = NarratorPromptBuilder.buildEngine(
                "Do a little dance.",
                "",
                raw,
                snapshot,
                "Backstory",
                List.of(new RecentAction("look", "You scan the hall.")),
                List.of(new RecentNarration("A hush settles over the corridor."))
        );

        assertThat(prompt).contains("MODE: CHECK_REQUEST");
        assertThat(prompt).contains("Sentence 2: \"Roll <dice call>.\"");
        assertThat(prompt).contains("RECENT_ACTIONS");
        assertThat(prompt).doesNotContain("RECENT_NARRATION");
    }

    @Test
    void buildEngineLookDirectionSelectsDirectionMode() {
        String raw = "To the east: A narrow corridor.";
        String prompt = NarratorPromptBuilder.buildEngine(
                "look east",
                "look east",
                raw,
                "Hall\nExits: east",
                "Backstory",
                List.of(),
                List.of()
        );

        assertThat(prompt).contains("MODE: LOOK_DIRECTION");
        assertThat(prompt).contains("BEGIN_ACTION_RESULT");
        assertThat(prompt).doesNotContain("BEGIN_SCENE_SOURCE");
    }

    @Test
    void buildEngineAddsLocationIntentForWhereAmI() {
        String raw = "Hall\nExits: east";
        String prompt = NarratorPromptBuilder.buildEngine(
                "where am i?",
                "look",
                raw,
                raw,
                "Backstory",
                List.of(),
                List.of()
        );

        assertThat(prompt).contains("INTENT: LOCATION_QUESTION");
        assertThat(prompt).contains("location question");
    }
}

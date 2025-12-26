package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NarratorPromptSelectionTest {

    @Test
    void buildEngineIncludesRawOutputAndSnapshot() {
        String raw = "Hallway\nExits: north";
        String snapshot = "Garden\nExits: south";
        String prompt = NarratorPromptBuilder.buildEngine(
                "look",
                "look",
                raw,
                snapshot,
                "Backstory"
        );

        assertThat(prompt).contains(raw);
        assertThat(prompt).contains(snapshot);
        assertThat(prompt).contains("If RAW_ENGINE_OUTPUT contains an \"Exits:\" line");
        assertThat(prompt).contains("SCENE_DETAIL_LEVEL");
    }

    @Test
    void buildSnapshotIncludesSceneAndColor() {
        String snapshot = "Cave\nExits: east";
        String color = "A torch sputters.";
        String prompt = NarratorPromptBuilder.buildSnapshot(
                "look",
                "look",
                snapshot,
                color,
                "Backstory"
        );

        assertThat(prompt).contains(snapshot);
        assertThat(prompt).contains(color);
        assertThat(prompt).contains("BEGIN_SCENE_SNAPSHOT");
        assertThat(prompt).contains("COLOR_EVENT");
        assertThat(prompt).contains("SCENE_DETAIL_LEVEL");
    }
}

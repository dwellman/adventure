package com.demo.adventure.ai.runtime;

import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class NarratorFallbackTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void narrateSnapshotUsesRawOutputWhenAiDisabled() {
        NarrationService service = new NarrationService(false, null, false);
        String snapshot = "Clearing\nExits: east";
        String color = "A breeze stirs the leaves.";

        String output = captureOut(() -> service.narrateSnapshot(snapshot, color, "", "", ""));
        assertThat(output.trim()).isEqualTo("Clearing\n  Exits: east\n  A breeze stirs the leaves.");
    }

    @Test
    void narrateEnginePrintsRawOutputWhenAiDisabled() {
        NarrationService service = new NarrationService(false, null, false);
        String raw = "You take the key.";

        String output = captureOut(() -> service.narrateEngine(raw, "Scene\nExits: west", "", "", ""));
        assertThat(output.trim()).isEqualTo(raw);
    }

    @Test
    void snapshotFallbackAnchorsLocationAndExitsOnMultiSceneError() {
        NarrationService service = new NarrationService(
                true,
                "test",
                false,
                (apiKey, prompt, debug) -> "ERROR: MULTI_SCENE_INPUT"
        );
        String snapshot = "Atrium\nExits: north, south";
        String color = "A bell rings";

        String output = captureOut(() -> service.narrateSnapshot(snapshot, color, "", "", ""));
        assertThat(output).contains("Atrium.");
        assertThat(output).contains("A bell rings.");
        assertThat(output).contains("Exits lead north, south.");
    }

    private String captureOut(Runnable action) {
        console.reset();
        action.run();
        return console.output();
    }
}

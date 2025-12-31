package com.demo.adventure.ai.runtime;

import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NarratorFallbackTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    private String previousNoColor;

    @BeforeEach
    void disableAnsi() {
        previousNoColor = System.getProperty("NO_COLOR");
        System.setProperty("NO_COLOR", "1");
    }

    @AfterEach
    void restoreAnsi() {
        if (previousNoColor == null) {
            System.clearProperty("NO_COLOR");
        } else {
            System.setProperty("NO_COLOR", previousNoColor);
        }
    }

    @Test
    void narrateSnapshotUsesRawOutputWhenAiDisabled() {
        NarrationService service = new NarrationService(false, null, false);
        String snapshot = "Clearing\nExits: east";
        String color = "A breeze stirs the leaves.";

        String output = captureOut(() -> service.narrateSnapshot(snapshot, color, "", "", "", List.of(), List.of()));
        assertThat(output.trim()).isEqualTo("Clearing. A breeze stirs the leaves.");
    }

    @Test
    void narrateEnginePrintsRawOutputWhenAiDisabled() {
        NarrationService service = new NarrationService(false, null, false);
        String raw = "You take the key.";

        String output = captureOut(() -> service.narrateEngine(raw, "Scene\nExits: west", "", "", "", List.of(), List.of()));
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

        String output = captureOut(() -> service.narrateSnapshot(snapshot, color, "", "", "", List.of(), List.of()));
        assertThat(output).contains("Atrium.");
        assertThat(output).contains("A bell rings.");
        assertThat(output).doesNotContain("Exits: north, south");
    }

    private String captureOut(Runnable action) {
        console.reset();
        action.run();
        return console.output();
    }
}

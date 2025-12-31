package com.demo.adventure.ai.runtime;

import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NarrationServiceTest {

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
    void narrateEngineFallsBackToDeterministicOutputOnNonLiteral() {
        NarrationService service = new NarrationService(
                true,
                "test",
                false,
                (apiKey, prompt, debug) -> "You are in Hall. The air is thick with tension."
        );
        String raw = """
                # Corridor Hall-Lounge
                The corridor waits.
                Exits: EAST, SOUTH, WEST
                """;
        String snapshot = "Hall\nExits: EAST, SOUTH, WEST";

        String output = captureOut(() -> service.narrateEngine(raw, snapshot, "", "go east", "", List.of(), List.of()));
        assertThat(output).contains("Corridor Hall-Lounge");
        assertThat(output).contains("The corridor waits.");
        assertThat(output).doesNotContain("air is thick");
        assertThat(output).contains("Exits:");
    }

    @Test
    void narrateEngineSkipsExitsForActionResult() {
        NarrationService service = new NarrationService(
                true,
                "test",
                false,
                (apiKey, prompt, debug) -> "A narrow path leads to the signal shack."
        );
        String raw = "A narrow path leads to the signal shack.";
        String snapshot = "Train Platform\nExits: EAST";

        String output = captureOut(() -> service.narrateEngine(raw, snapshot, "", "look east", "", List.of(), List.of()));
        assertThat(output).contains("A narrow path leads to the signal shack.");
        assertThat(output).doesNotContain("Exits:");
    }

    @Test
    void narrateEngineLookTargetStaysFocusedOnActionResult() {
        NarrationService service = new NarrationService(
                true,
                "test",
                false,
                (apiKey, prompt, debug) -> "Nothing special to the north."
        );
        String raw = "Nothing special to the north.";
        String snapshot = "Hall\nExits: EAST, SOUTH, WEST";

        String output = captureOut(() -> service.narrateEngine(raw, snapshot, "", "look north", "", List.of(), List.of()));
        assertThat(output).contains("Nothing special to the north.");
        assertThat(output).doesNotContain("You are in Hall.");
        assertThat(output).doesNotContain("Exits:");
    }

    @Test
    void narrateEngineAppendsFixturesAndItemsFromScene() {
        NarrationService service = new NarrationService(
                true,
                "test",
                false,
                (apiKey, prompt, debug) -> "The Hall stretches ahead."
        );
        String raw = """
                Hall
                The Hall stretches ahead.
                Fixtures:
                - Case Board
                Items:
                - Pocket Watch
                Exits: EAST, SOUTH
                """;

        String output = captureOut(() -> service.narrateEngine(raw, "", "", "look", "", List.of(), List.of()));
        assertThat(output).contains("The Hall stretches ahead.");
        assertThat(output).contains("Fixtures:");
        assertThat(output).contains("Case Board");
        assertThat(output).contains("Items:");
        assertThat(output).contains("Pocket Watch");
        assertThat(output).contains("Exits:");
    }

    @Test
    void narrateEngineEmoteSkipsLocationAndExits() {
        NarrationService service = new NarrationService(
                true,
                "test",
                false,
                (apiKey, prompt, debug) -> "You are in Hall. You dance."
        );
        String raw = "EMOTE: Do a little dance.";
        String snapshot = "Hall\nExits: EAST";

        String output = captureOut(() -> service.narrateEngine(raw, snapshot, "", "", "", List.of(), List.of()));
        assertThat(output).doesNotContain("You are in Hall.");
        assertThat(output).doesNotContain("Exits:");
        assertThat(output).contains("You dance.");
    }

    private String captureOut(Runnable action) {
        console.reset();
        action.run();
        return console.output();
    }
}

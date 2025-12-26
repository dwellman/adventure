package com.demo.adventure.authoring.cli;

import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ZoneBuilderCliIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void buildsGameAndStructuredOutputWithRolePlacement() throws Exception {
        Path input = tempDir.resolve("input.yaml");
        Files.writeString(input, """
                game:
                  seed: 4242
                  preamble: "You wash ashore."
                zones:
                  - id: coast
                    region: COAST
                    targetPlotCount: 8
                    difficulty: EASY
                    pacing: BALANCED
                    topology: BRANCHY
                    anchors:
                      - key: entry
                        name: Wreck Beach
                        role: ENTRY
                        description: "Start."
                      - key: lookout
                        name: Lookout
                        role: VISTA
                      - key: exit
                        name: Treehouse
                        role: EXIT
                items:
                  - name: Rope Ladder
                    description: "Knotted rope."
                    ownerRole: EXIT
                puzzles:
                  - from: entry
                    direction: UP
                    keyString: HAS("Rope Ladder")
                    description: "You need the rope ladder."
                """, StandardCharsets.UTF_8);

        Path output = tempDir.resolve("game.yaml");
        Path structured = tempDir.resolve("structured");

        ZoneBuilderCli.main(new String[]{
                "--in", input.toString(),
                "--out", output.toString(),
                "--structured-out", structured.toString(),
                "--id", "demo",
                "--title", "Demo Game"
        });

        assertThat(output).exists();
        assertThat(structured.resolve("game.yaml")).exists();

        GameSave save = GameSaveYamlLoader.load(output);

        UUID exitPlot = save.plots().stream()
                .filter(p -> "Treehouse".equalsIgnoreCase(p.name()))
                .map(p -> p.plotId())
                .findFirst()
                .orElseThrow();
        Optional<GameSave.ItemRecipe> rope = save.items().stream()
                .filter(i -> i.name().equalsIgnoreCase("Rope Ladder"))
                .findFirst();
        assertThat(rope).isPresent();
        assertThat(rope.get().ownerId()).isEqualTo(exitPlot);

        assertThat(save.gates()).anySatisfy(g -> assertThat(g.keyString()).isEqualTo("HAS(\"Rope Ladder\")"));
    }
}

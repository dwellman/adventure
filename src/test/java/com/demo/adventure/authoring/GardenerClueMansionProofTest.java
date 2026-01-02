package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.authoring.gardener.FixtureDescriptionExpander;
import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.authoring.gardener.Gardener;
import com.demo.adventure.authoring.gardener.GardenerDescriptionPatch;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.save.GameSave;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GardenerClueMansionProofTest {

    @Test
    void runsGardenerAsOneOffAndPreservesDescriptionHistory() throws GameBuilderException {
        FixtureDescriptionExpander expander = registry -> {
            Item desk = findItemByLabel(registry, "Study Desk");
            if (desk == null) {
                return List.of();
            }
            String original = desk.getDescription();
            String updated = original + " (expanded)";
            desk.recordDescription(original, 0);
            desk.recordDescription(updated, 1);
            return List.of(new GardenerDescriptionPatch(desk.getId(), original, updated, "test"));
        };
        Gardener gardener = new Gardener(expander);

        GardenResult result = gardener.garden(loadMansionSave());

        assertThat(result.report().getProblems()).isEmpty();
        Item desk = findItemByLabel(result.registry(), "Study Desk");
        assertThat(desk.getDescriptionHistory()).hasSize(2);
        String yaml = GameSaveYamlWriter.toYaml(result);
        assertThat(yaml).doesNotContain("descriptionOriginal", "descriptionExpanded", "descriptionHistory");
        assertThat(yaml).contains("description:");
        assertThat(yaml).contains("worldClock: 0", "worldClock: 1");
    }

    private static Item findItemByLabel(KernelRegistry registry, String label) {
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> item.getLabel() != null && item.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

    private static GameSave loadMansionSave() throws GameBuilderException {
        try {
            return StructuredGameSaveLoader.load(Path.of("src/main/resources/games/mansion/game.yaml"));
        } catch (Exception ex) {
            throw new GameBuilderException("Failed to load mansion game.yaml: " + ex.getMessage(), null);
        }
    }
}

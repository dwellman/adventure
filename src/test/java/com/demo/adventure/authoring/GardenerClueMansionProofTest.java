package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.authoring.gardener.FixtureDescriptionExpander;
import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.authoring.gardener.Gardener;
import com.demo.adventure.authoring.gardener.GardenerDescriptionPatch;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.authoring.samples.ClueMansion;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.demo.adventure.authoring.samples.ClueMansion.STUDY_DESK;
import static org.assertj.core.api.Assertions.assertThat;

class GardenerClueMansionProofTest {

    @Test
    void runsGardenerAsOneOffAndPreservesDescriptionHistory() throws GameBuilderException {
        FixtureDescriptionExpander expander = registry -> {
            Item desk = (Item) registry.get(STUDY_DESK);
            if (desk == null) {
                return List.of();
            }
            String original = desk.getDescription();
            String updated = original + " (expanded)";
            desk.recordDescription(original, 0);
            desk.recordDescription(updated, 1);
            return List.of(new GardenerDescriptionPatch(STUDY_DESK, original, updated, "test"));
        };
        Gardener gardener = new Gardener(expander);

        GardenResult result = gardener.garden(ClueMansion.gameSave());

        assertThat(result.report().getProblems()).isEmpty();
        Item desk = (Item) result.registry().get(STUDY_DESK);
        assertThat(desk.getDescriptionHistory()).hasSize(2);
        String yaml = GameSaveYamlWriter.toYaml(result);
        assertThat(yaml).doesNotContain("descriptionOriginal", "descriptionExpanded", "descriptionHistory");
        assertThat(yaml).contains("description:");
        assertThat(yaml).contains("worldClock: 0", "worldClock: 1");
    }
}

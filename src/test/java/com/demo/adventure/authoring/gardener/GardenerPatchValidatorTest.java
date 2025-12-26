package com.demo.adventure.authoring.gardener;

import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GardenerPatchValidatorTest {

    @Test
    void detectsUnknownIdsAndDuplicateTitles() {
        GameSave save = sampleSave();
        UUID plotA = save.plots().get(0).plotId();
        UUID plotB = save.plots().get(1).plotId();

        Map<UUID, GardenerPatch.PlotPatch> plots = new HashMap<>();
        plots.put(plotA, new GardenerPatch.PlotPatch("Hall!", "plot description"));
        plots.put(plotB, new GardenerPatch.PlotPatch("Hall", "plot description"));
        plots.put(UUID.randomUUID(), new GardenerPatch.PlotPatch("", ""));

        Map<UUID, GardenerPatch.ThingPatch> things = new HashMap<>();
        things.put(UUID.randomUUID(), new GardenerPatch.ThingPatch("", ""));

        GardenerPatch patch = new GardenerPatch(new GardenerPatch.Metadata(1L, "", "", ""), plots, things);
        GardenerPatchValidator.ValidationResult result = GardenerPatchValidator.validate(save, patch, 4, 10, 4, 10);

        assertThat(result.ok()).isFalse();
        assertThat(result.problems()).anyMatch(p -> p.contains("Unknown plotId"));
        assertThat(result.problems()).anyMatch(p -> p.contains("Duplicate plot title"));
        assertThat(result.problems()).anyMatch(p -> p.contains("Thing name too long") || p.contains("Empty thing name"));
    }

    @Test
    void returnsWarningsForBannedTokens() {
        GameSave save = sampleSave();
        UUID plotA = save.plots().get(0).plotId();

        Map<UUID, GardenerPatch.PlotPatch> plots = Map.of(
                plotA, new GardenerPatch.PlotPatch("Plot Builder", "A plot description")
        );
        GardenerPatch patch = new GardenerPatch(new GardenerPatch.Metadata(1L, "", "", ""), plots, Map.of());

        GardenerPatchValidator.ValidationResult result = GardenerPatchValidator.validate(save, patch, 40, 200, 40, 200);

        assertThat(result.ok()).isTrue();
        assertThat(result.warnings()).isNotEmpty();
    }

    @Test
    void acceptsValidPatch() {
        GameSave save = sampleSave();
        UUID plotA = save.plots().get(0).plotId();
        UUID thingId = save.fixtures().get(0).id();

        Map<UUID, GardenerPatch.PlotPatch> plots = Map.of(
                plotA, new GardenerPatch.PlotPatch("Garden", "A calm place")
        );
        Map<UUID, GardenerPatch.ThingPatch> things = Map.of(
                thingId, new GardenerPatch.ThingPatch("Bench", "A wooden bench")
        );
        GardenerPatch patch = new GardenerPatch(new GardenerPatch.Metadata(1L, "", "", ""), plots, things);

        GardenerPatchValidator.ValidationResult result = GardenerPatchValidator.validate(save, patch, 40, 200, 40, 200);

        assertThat(result.ok()).isTrue();
        assertThat(result.problems()).isEmpty();
    }

    private GameSave sampleSave() {
        UUID plotA = UUID.randomUUID();
        UUID plotB = UUID.randomUUID();
        WorldRecipe.PlotSpec a = new WorldRecipe.PlotSpec(plotA, "Plot A", "TEST", 0, 0, "Desc A");
        WorldRecipe.PlotSpec b = new WorldRecipe.PlotSpec(plotB, "Plot B", "TEST", 1, 0, "Desc B");
        WorldRecipe.FixtureSpec fixture = new WorldRecipe.FixtureSpec(UUID.randomUUID(), "Fixture", "Fixture desc", plotA, true, Map.of());
        GameSave.ItemRecipe item = new GameSave.ItemRecipe(UUID.randomUUID(), "Item", "Item desc", plotA, true, false, "true", 1.0, 1.0, 0, 0, 0, 0, Map.of());
        GameSave.ActorRecipe actor = new GameSave.ActorRecipe(UUID.randomUUID(), "Actor", "Actor desc", plotA, true, List.of("Lockpicking"), null, null, Map.of());
        return new GameSave(1L, plotA, "preamble", List.of(a, b), List.of(), List.of(fixture), List.of(item), List.of(actor));
    }
}

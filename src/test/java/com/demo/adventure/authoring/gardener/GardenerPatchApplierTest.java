package com.demo.adventure.authoring.gardener;

import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GardenerPatchApplierTest {

    @Test
    void appliesPlotAndThingOverrides() {
        UUID plotId = UUID.randomUUID();
        UUID fixtureId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();

        WorldRecipe.PlotSpec plot = new WorldRecipe.PlotSpec(plotId, "Plot", "TEST", 0, 0, "Plot desc");
        WorldRecipe.FixtureSpec fixture = new WorldRecipe.FixtureSpec(fixtureId, "Fixture", "Fixture desc", plotId, true, Map.of());
        GameSave.ItemRecipe item = new GameSave.ItemRecipe(itemId, "Item", "Item desc", plotId, true, false, "true", 1.0, 1.0, 0, 0, 0, 0, Map.of());
        GameSave.ActorRecipe actor = new GameSave.ActorRecipe(actorId, "Actor", "Actor desc", plotId, true, List.of(), null, null, Map.of());

        GameSave save = new GameSave(1L, plotId, "preamble", List.of(plot), List.of(), List.of(fixture), List.of(item), List.of(actor));

        GardenerPatch patch = new GardenerPatch(
                new GardenerPatch.Metadata(1L, "", "", ""),
                Map.of(plotId, new GardenerPatch.PlotPatch("New Plot", "New plot desc")),
                Map.of(
                        fixtureId, new GardenerPatch.ThingPatch("New Fixture", "New fixture desc"),
                        itemId, new GardenerPatch.ThingPatch("New Item", "New item desc"),
                        actorId, new GardenerPatch.ThingPatch("New Actor", "New actor desc")
                )
        );

        GameSave updated = GardenerPatchApplier.apply(save, patch);

        assertThat(updated.plots().get(0).name()).isEqualTo("New Plot");
        assertThat(updated.fixtures().get(0).name()).isEqualTo("New Fixture");
        assertThat(updated.items().get(0).name()).isEqualTo("New Item");
        assertThat(updated.actors().get(0).name()).isEqualTo("New Actor");
    }
}

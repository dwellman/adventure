package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.gardener.FixtureDescriptionExpander;
import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.authoring.gardener.Gardener;
import com.demo.adventure.authoring.gardener.GardenerDescriptionPatch;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GardenerTest {

    private static final UUID START = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID LEAF = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID FIXTURE_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @Test
    void detectsExitThatCannotReturnToStart() throws GameBuilderException {
        WorldRecipe.PlotSpec startPlot = new WorldRecipe.PlotSpec(START, "Start", "REGION", 0, 0, "Start plot");
        WorldRecipe.PlotSpec leafPlot = new WorldRecipe.PlotSpec(LEAF, "Leaf", "REGION", 1, 0, "Leaf plot");
        List<WorldRecipe.GateSpec> gates = List.of(
                new WorldRecipe.GateSpec(START, Direction.E, LEAF, true, "true", "Start -> Leaf", "One-way to leaf")
        );

        GameSave save = new GameSave(1L, START, "", List.of(startPlot, leafPlot), gates, List.of(), List.of(), List.of());

        Gardener gardener = new Gardener();
        GardenResult result = gardener.garden(save);

        assertThat(result.report().getProblems())
                .extracting(WorldBuildProblem::code)
                .contains("E_EXIT_NO_RETURN_PATH");
    }

    @Test
    void aiPassExpandsFixtureDescriptions() throws GameBuilderException {
        WorldRecipe.PlotSpec startPlot = new WorldRecipe.PlotSpec(START, "Start", "REGION", 0, 0, "Start plot");
        WorldRecipe.FixtureSpec fixtureSpec = new WorldRecipe.FixtureSpec(
                FIXTURE_ID,
                "Ancient Desk",
                "Dusty desk.",
                START,
                true,
                java.util.Map.of()
        );

        GameSave save = new GameSave(2L, START, "", List.of(startPlot), List.of(), List.of(fixtureSpec), List.of(), List.of());

        FixtureDescriptionExpander expander = registry -> {
            Item fixture = (Item) registry.get(FIXTURE_ID);
            String updated = fixture.getDescription() + " Expanded by AI.";
            fixture.recordDescription(fixture.getDescription(), 0);
            fixture.recordDescription(updated, 1);
            return List.of(new GardenerDescriptionPatch(FIXTURE_ID, "Dusty desk.", updated, "test-ai"));
        };

        Gardener gardener = new Gardener(expander);
        GardenResult result = gardener.garden(save);

        assertThat(result.descriptionPatches())
                .extracting(GardenerDescriptionPatch::thingId)
                .containsExactly(FIXTURE_ID);
        assertThat(result.descriptionPatches())
                .extracting(GardenerDescriptionPatch::author)
                .containsExactly("test-ai");
        Item updatedFixture = (Item) result.registry().get(FIXTURE_ID);
        assertThat(updatedFixture.getDescription()).isEqualTo("Dusty desk. Expanded by AI.");
        assertThat(updatedFixture.getDescriptionHistory()).hasSize(2);
        assertThat(result.report().getProblems()).isEmpty();
    }
}

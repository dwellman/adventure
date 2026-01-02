package com.demo.adventure.authoring;

import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GameSaveAssemblerTest {

    @Test
    void buildsRegistryFromGameSave() throws Exception {
        GameSave save = StructuredGameSaveLoader.load(Path.of("src/main/resources/games/mansion/game.yaml"));

        WorldBuildResult result = new GameSaveAssembler().apply(save);
        KernelRegistry registry = result.registry();

        assertThat(result.report().getProblems()).isEmpty();
        assertThat(result.startPlotId()).isEqualTo(plotIdByName(save, "Hall"));

        assertPlotsMatch(save, registry);
        assertGatesMatch(save, registry);
        assertFixturesMatch(save, registry);
        assertItemsMatch(save, registry);
        assertActorsMatch(save, registry);
    }

    @Test
    void loadsFromCanonicalYaml() throws Exception {
        WorldBuildResult result = new GameSaveAssembler()
                .apply(Path.of("src/main/resources/cookbook/gardened-mansion.yaml"));
        assertThat(result.report().getProblems()).isEmpty();
        assertThat(result.registry().getEverything()).isNotEmpty();
    }

    private static void assertPlotsMatch(GameSave save, KernelRegistry registry) {
        List<Plot> plots = registry.getEverything().values().stream()
                .filter(Plot.class::isInstance)
                .map(Plot.class::cast)
                .toList();
        assertThat(plots).hasSize(save.plots().size());

        Map<UUID, Plot> plotById = plots.stream().collect(java.util.stream.Collectors.toMap(Plot::getId, p -> p));
        for (WorldRecipe.PlotSpec spec : save.plots()) {
            Plot plot = plotById.get(spec.plotId());
            assertThat(plot).isNotNull();
            assertThat(plot.getLabel()).isEqualTo(spec.name());
            assertThat(plot.getRegion()).isEqualTo(spec.region());
            assertThat(plot.getLocationX()).isEqualTo(spec.locationX());
            assertThat(plot.getLocationY()).isEqualTo(spec.locationY());
        }
    }

    private static UUID plotIdByName(GameSave save, String name) {
        return save.plots().stream()
                .filter(plot -> plot.name().equalsIgnoreCase(name))
                .map(WorldRecipe.PlotSpec::plotId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Plot not found: " + name));
    }

    private static void assertGatesMatch(GameSave save, KernelRegistry registry) {
        List<Gate> gates = registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .toList();

        for (WorldRecipe.GateSpec spec : save.gates()) {
            Gate gate = findGateBetween(gates, spec.fromPlotId(), spec.toPlotId());
            assertThat(gate).isNotNull();
            assertThat(gate.directionFrom(spec.fromPlotId())).isEqualTo(spec.direction());
            assertThat(gate.isVisible()).isEqualTo(spec.visible());
            assertThat(gate.getKeyString()).isEqualTo(spec.keyString());
        }
    }

    private static void assertFixturesMatch(GameSave save, KernelRegistry registry) {
        Map<UUID, Item> items = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .collect(java.util.stream.Collectors.toMap(Item::getId, i -> i));

        for (WorldRecipe.FixtureSpec spec : save.fixtures()) {
            Item fixture = items.get(spec.id());
            assertThat(fixture).as("fixture " + spec.id()).isNotNull();
            assertThat(fixture.isFixture()).isTrue();
            assertThat(fixture.getOwnerId()).isEqualTo(spec.ownerId());
            assertThat(fixture.isVisible()).isEqualTo(spec.visible());
        }
    }

    private static void assertItemsMatch(GameSave save, KernelRegistry registry) {
        Map<UUID, Item> items = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .collect(java.util.stream.Collectors.toMap(Item::getId, i -> i));

        for (GameSave.ItemRecipe spec : save.items()) {
            Item item = items.get(spec.id());
            assertThat(item).as("item " + spec.id()).isNotNull();
            assertThat(item.getOwnerId()).isEqualTo(spec.ownerId());
            assertThat(item.isVisible()).isEqualTo(spec.visible());
            assertThat(item.isFixture()).isEqualTo(spec.fixture());
            assertThat(item.getKey()).isEqualTo(spec.keyString());
        }
    }

    private static void assertActorsMatch(GameSave save, KernelRegistry registry) {
        Map<UUID, Actor> actors = registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .collect(java.util.stream.Collectors.toMap(Actor::getId, a -> a));

        for (GameSave.ActorRecipe spec : save.actors()) {
            Actor actor = actors.get(spec.id());
            assertThat(actor).as("actor " + spec.id()).isNotNull();
            assertThat(actor.getOwnerId()).isEqualTo(spec.ownerId());
            assertThat(actor.isVisible()).isEqualTo(spec.visible());
        }
    }

    private static Gate findGateBetween(List<Gate> gates, UUID a, UUID b) {
        return gates.stream()
                .filter(g -> g.connects(a) && g.connects(b))
                .findFirst()
                .orElse(null);
    }
}

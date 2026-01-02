package com.demo.adventure.authoring;

import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterials;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterialsGenerator;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class IslandAdventureIntegrationTest {

    @Test
    void buildsIslandAdventureBillOfMaterials() throws Exception {
        GameSave save = loadIslandSave();
        WorldBuildResult result = new GameSaveAssembler().apply(save);
        KernelRegistry registry = result.registry();

        assertThat(result.report().getProblems()).isEmpty();
        assertThat(result.startPlotId()).isEqualTo(plotIdByName(save, "Wreck Beach"));

        List<Plot> plots = registry.getEverything().values().stream()
                .filter(Plot.class::isInstance)
                .map(Plot.class::cast)
                .toList();
        List<Gate> gates = registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .toList();

        assertThat(plots).hasSize(save.plots().size());
        assertThat(gates.size()).isGreaterThan(0);
        assertThat(gates.size()).isLessThanOrEqualTo(save.gates().size());

        assertFixture(registry, "Treehouse Skeleton", plotIdByName(save, "Treehouse"));
        assertFixture(registry, "Spider Webs", plotIdByName(save, "Cave Inner Chamber"));
        assertFixture(registry, "Cave Wall Map", plotIdByName(save, "Cave Back Chamber"));
        assertFixture(registry, "Time Stone Pedestal", plotIdByName(save, "Volcano Altar"));

        assertOwner(registry, "Hatchet", findItemIdByLabel(registry, "Treehouse Skeleton"));
        assertOwner(registry, "Parachute", plotIdByName(save, "Plane Wreck"));
        assertOwner(registry, "Time Stone", plotIdByName(save, "Volcano Altar"));
        assertOwner(registry, "Vine Rope", plotIdByName(save, "Monkey Grove"));

        assertActor(registry, "Castaway", plotIdByName(save, "Wreck Beach"));
        assertActor(registry, "Chaos Monkey Troop", plotIdByName(save, "Monkey Grove"));
        assertActor(registry, "Scratch (Ghost)", plotIdByName(save, "Treehouse"));

        Gate backChamberGate = findGate(registry,
                plotIdByName(save, "Cave Inner Chamber"),
                plotIdByName(save, "Cave Back Chamber"));
        assertThat(backChamberGate.getKeyString()).isEqualTo("HAS(\"Lit Torch\")");
        assertThat(backChamberGate.isVisible()).isTrue();

        WorldBillOfMaterials bom = WorldBillOfMaterialsGenerator.fromRegistry(registry);
        WorldBillOfMaterials.Section mapSection = bom.getSections().stream()
                .filter(s -> "Map".equals(s.title()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Map section missing"));
        assertThat(mapSection.entries())
                .extracting(WorldBillOfMaterials.Entry::name, WorldBillOfMaterials.Entry::quantity)
                .contains(
                        tuple("Gates", gates.size()),
                        tuple("Land plots", plots.size())
                );
    }

    private static void assertFixture(KernelRegistry registry, String label, UUID ownerId) {
        Item fixture = findItemByLabel(registry, label);
        assertThat(fixture).as("fixture " + label).isNotNull();
        assertThat(fixture.isFixture()).isTrue();
        assertThat(fixture.getOwnerId()).isEqualTo(ownerId);
    }

    private static void assertOwner(KernelRegistry registry, String label, UUID expectedOwnerId) {
        Thing thing = findThingByLabel(registry, label);
        assertThat(thing).as("owner check for " + label).isNotNull();
        assertThat(thing.getOwnerId()).isEqualTo(expectedOwnerId);
    }

    private static void assertActor(KernelRegistry registry, String label, UUID expectedOwnerId) {
        Thing actor = findActorByLabel(registry, label);
        assertThat(actor).as("actor " + label).isNotNull();
        assertThat(actor.getOwnerId()).isEqualTo(expectedOwnerId);
    }

    private static UUID plotIdByName(GameSave save, String name) {
        return save.plots().stream()
                .filter(plot -> plot.name().equalsIgnoreCase(name))
                .map(WorldRecipe.PlotSpec::plotId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Plot not found: " + name));
    }

    private static UUID findItemIdByLabel(KernelRegistry registry, String label) {
        Item item = findItemByLabel(registry, label);
        return item == null ? null : item.getId();
    }

    private static Item findItemByLabel(KernelRegistry registry, String label) {
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> item.getLabel() != null && item.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

    private static Thing findThingByLabel(KernelRegistry registry, String label) {
        return registry.getEverything().values().stream()
                .filter(Thing.class::isInstance)
                .map(Thing.class::cast)
                .filter(thing -> thing.getLabel() != null && thing.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

    private static Thing findActorByLabel(KernelRegistry registry, String label) {
        return registry.getEverything().values().stream()
                .filter(Thing.class::isInstance)
                .map(Thing.class::cast)
                .filter(thing -> thing instanceof com.demo.adventure.domain.model.Actor)
                .filter(thing -> thing.getLabel() != null && thing.getLabel().equalsIgnoreCase(label))
                .findFirst()
                .orElse(null);
    }

    private static Gate findGate(KernelRegistry registry, UUID a, UUID b) {
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .filter(g -> g.connects(a) && g.connects(b))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Gate not found between " + a + " and " + b));
    }

    private static GameSave loadIslandSave() throws Exception {
        return StructuredGameSaveLoader.load(Path.of("src/main/resources/games/island/game.yaml"));
    }
}

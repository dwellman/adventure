package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterials;
import com.demo.adventure.authoring.save.build.WorldBillOfMaterialsGenerator;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.authoring.samples.IslandAdventure;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.demo.adventure.authoring.samples.IslandAdventure.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class IslandAdventureIntegrationTest {

    @Test
    void buildsIslandAdventureBillOfMaterials() throws GameBuilderException {
        GameSave save = IslandAdventure.gameSave();
        WorldBuildResult result = new GameSaveAssembler().apply(save);
        KernelRegistry registry = result.registry();

        assertThat(result.report().getProblems()).isEmpty();
        assertThat(result.startPlotId()).isEqualTo(WRECK_BEACH);

        List<Plot> plots = registry.getEverything().values().stream()
                .filter(Plot.class::isInstance)
                .map(Plot.class::cast)
                .toList();
        List<Gate> gates = registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .toList();

        assertThat(plots).hasSize(11);
        assertThat(gates).hasSizeGreaterThanOrEqualTo(12);

        assertFixture(registry, TREEHOUSE_SKELETON, TREEHOUSE);
        assertFixture(registry, CAVE_WEB_BARRIER, CAVE_WEB);
        assertFixture(registry, CAVE_WALL_MAP, CAVE_BACK_CHAMBER);
        assertFixture(registry, TIME_STONE_PEDESTAL, VOLCANO_ALTAR);

        assertOwner(registry, HATCHET, TREEHOUSE);
        assertOwner(registry, PARACHUTE, PLANE_WRECK);
        assertOwner(registry, TIME_STONE, VOLCANO_ALTAR);
        assertOwner(registry, VINE_ROPE, MONKEY_GROVE);

        assertActor(registry, CASTAWAY, WRECK_BEACH);
        assertActor(registry, MONKEY_TROOP, MONKEY_GROVE);
        assertActor(registry, SCRATCH_GHOST, TREEHOUSE);

        Gate backChamberGate = findGate(registry, CAVE_WEB, CAVE_BACK_CHAMBER);
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

    private static void assertFixture(KernelRegistry registry, UUID id, UUID ownerId) {
        Item fixture = (Item) registry.get(id);
        assertThat(fixture).as("fixture " + id).isNotNull();
        assertThat(fixture.isFixture()).isTrue();
        assertThat(fixture.getOwnerId()).isEqualTo(ownerId);
    }

    private static void assertOwner(KernelRegistry registry, UUID thingId, UUID expectedOwnerId) {
        Thing thing = registry.get(thingId);
        assertThat(thing).as("owner check for " + thingId).isNotNull();
        assertThat(thing.getOwnerId()).isEqualTo(expectedOwnerId);
    }

    private static void assertActor(KernelRegistry registry, UUID actorId, UUID expectedOwnerId) {
        Thing actor = registry.get(actorId);
        assertThat(actor).as("actor " + actorId).isNotNull();
        assertThat(actor.getOwnerId()).isEqualTo(expectedOwnerId);
    }

    private static Gate findGate(KernelRegistry registry, UUID a, UUID b) {
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .filter(g -> g.connects(a) && g.connects(b))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Gate not found between " + a + " and " + b));
    }
}

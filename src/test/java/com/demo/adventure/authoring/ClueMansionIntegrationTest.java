package com.demo.adventure.authoring;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
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
import com.demo.adventure.authoring.samples.ClueMansion;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.demo.adventure.authoring.samples.ClueMansion.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ClueMansionIntegrationTest {

    @Test
    void buildsClueMansionBillOfMaterials() throws GameBuilderException {
        GameSave save = ClueMansion.gameSave();
        WorldBuildResult result = new GameSaveAssembler().apply(save);
        KernelRegistry registry = result.registry();

        assertThat(result.report().getProblems()).isEmpty();
        assertThat(result.startPlotId()).isEqualTo(HALL);

        List<Plot> plots = registry.getEverything().values().stream()
                .filter(Plot.class::isInstance)
                .map(Plot.class::cast)
                .toList();
        List<Gate> gates = registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .toList();

        assertThat(plots).hasSize(22);
        assertThat(gates).hasSize(27);
        assertThat(gates.stream().filter(g -> !g.isVisible()).count()).isEqualTo(2);

        Item desk = (Item) registry.get(STUDY_DESK);
        Item drawer = (Item) registry.get(STUDY_DESK_DRAWER);
        assertThat(desk).isNotNull();
        assertThat(drawer).isNotNull();
        assertThat(desk.isFixture()).isTrue();
        assertThat(drawer.isFixture()).isTrue();
        assertThat(desk.getOwnerId()).isEqualTo(STUDY);
        assertThat(drawer.getOwnerId()).isEqualTo(STUDY_DESK);

        WorldBillOfMaterials bom = WorldBillOfMaterialsGenerator.fromRegistry(registry);
        WorldBillOfMaterials.Section mapSection = bom.getSections().stream()
                .filter(s -> "Map".equals(s.title()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Map section missing"));
        assertThat(mapSection.entries())
                .extracting(WorldBillOfMaterials.Entry::name, WorldBillOfMaterials.Entry::quantity)
                .contains(
                        tuple("Gates", 27),
                        tuple("Land plots", 22)
                );

        assertWeaponPlacement(registry);
        assertOwner(registry, POCKET_WATCH, DETECTIVE);

        Gate cellarGate = findGate(registry, BILLIARD_ROOM, CELLAR);
        assertThat(cellarGate.getKeyString()).isEqualTo("HAS(\"Basement Key\")");

        KeyExpressionEvaluator.HasResolver hasResolver = KeyExpressionEvaluator.registryHasResolver(registry, DETECTIVE);
        assertThat(KeyExpressionEvaluator.evaluate(cellarGate.getKeyString(), hasResolver)).isFalse();

        registry.moveOwnership(BASEMENT_KEY, DETECTIVE);
        assertThat(KeyExpressionEvaluator.evaluate(cellarGate.getKeyString(), hasResolver)).isTrue();
    }

    private static void assertWeaponPlacement(KernelRegistry registry) {
        assertOwner(registry, REVOLVER, STUDY);
        assertOwner(registry, ROPE, LOUNGE);
        assertOwner(registry, KNIFE, KITCHEN);
        assertOwner(registry, CANDLESTICK, DINING_ROOM);
        assertOwner(registry, LEAD_PIPE, BILLIARD_ROOM);
        assertOwner(registry, WRENCH, BALLROOM);
    }

    private static void assertOwner(KernelRegistry registry, UUID thingId, UUID expectedOwnerId) {
        Thing thing = registry.get(thingId);
        assertThat(thing).isNotNull();
        assertThat(thing.getOwnerId()).isEqualTo(expectedOwnerId);
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

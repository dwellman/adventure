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
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ClueMansionIntegrationTest {

    @Test
    void buildsClueMansionBillOfMaterials() throws GameBuilderException {
        GameSave save = loadMansionSave();
        WorldBuildResult result = new GameSaveAssembler().apply(save);
        KernelRegistry registry = result.registry();

        assertThat(result.report().getProblems()).isEmpty();
        assertThat(result.startPlotId()).isEqualTo(plotIdByName(save, "Hall"));

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
        assertThat(gates.stream().filter(g -> !g.isVisible()).count()).isGreaterThan(0);

        Item desk = findItemByLabel(registry, "Study Desk");
        Item drawer = findItemByLabel(registry, "Study Desk Drawer 1");
        assertThat(desk).isNotNull();
        assertThat(drawer).isNotNull();
        assertThat(desk.isFixture()).isTrue();
        assertThat(drawer.isFixture()).isTrue();
        assertThat(desk.getOwnerId()).isEqualTo(plotIdByName(save, "Study"));
        assertThat(drawer.getOwnerId()).isEqualTo(plotIdByName(save, "Study"));

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

        assertWeaponPlacement(registry, save);
        assertOwner(registry, "Pocket Watch", findActorByLabel(registry, "Detective").getId());

        Gate cellarGate = findGate(registry,
                plotIdByName(save, "Billiard Room"),
                plotIdByName(save, "Cellar"));
        assertThat(cellarGate.getKeyString()).isEqualTo("HAS(\"Basement Key\")");

        Thing detective = findActorByLabel(registry, "Detective");
        KeyExpressionEvaluator.HasResolver hasResolver =
                KeyExpressionEvaluator.registryHasResolver(registry, detective.getId());
        assertThat(KeyExpressionEvaluator.evaluate(cellarGate.getKeyString(), hasResolver)).isFalse();

        Thing basementKey = findThingByLabel(registry, "Basement Key");
        registry.moveOwnership(basementKey.getId(), detective.getId());
        assertThat(KeyExpressionEvaluator.evaluate(cellarGate.getKeyString(), hasResolver)).isTrue();
    }

    private static void assertWeaponPlacement(KernelRegistry registry, GameSave save) {
        assertOwner(registry, "Revolver", plotIdByName(save, "Study"));
        assertOwner(registry, "Rope", plotIdByName(save, "Lounge"));
        assertOwner(registry, "Knife", plotIdByName(save, "Kitchen"));
        assertOwner(registry, "Candlestick", plotIdByName(save, "Dining Room"));
        assertOwner(registry, "Lead Pipe", plotIdByName(save, "Billiard Room"));
        assertOwner(registry, "Wrench", plotIdByName(save, "Ballroom"));
    }

    private static void assertOwner(KernelRegistry registry, String label, UUID expectedOwnerId) {
        Thing thing = findThingByLabel(registry, label);
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

    private static UUID plotIdByName(GameSave save, String name) {
        return save.plots().stream()
                .filter(plot -> plot.name().equalsIgnoreCase(name))
                .map(com.demo.adventure.domain.save.WorldRecipe.PlotSpec::plotId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Plot not found: " + name));
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

    private static GameSave loadMansionSave() throws GameBuilderException {
        try {
            return StructuredGameSaveLoader.load(Path.of("src/main/resources/games/mansion/game.yaml"));
        } catch (Exception ex) {
            throw new GameBuilderException("Failed to load mansion game.yaml: " + ex.getMessage(), null);
        }
    }
}

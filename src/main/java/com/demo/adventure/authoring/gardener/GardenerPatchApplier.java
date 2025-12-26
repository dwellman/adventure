package com.demo.adventure.authoring.gardener;

import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;
import com.demo.adventure.domain.save.ActorRecipeBuilder;
import com.demo.adventure.domain.save.ItemRecipeBuilder;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * Applies narration patches to a GameSave without changing topology.
 */
public final class GardenerPatchApplier {
    private GardenerPatchApplier() {
    }

    public static GameSave apply(GameSave save, GardenerPatch patch) {
        if (save == null || patch == null) {
            return save;
        }
        Map<UUID, GardenerPatch.PlotPatch> plotPatches = patch.plots() == null ? Map.of() : patch.plots();
        Map<UUID, GardenerPatch.ThingPatch> thingPatches = patch.things() == null ? Map.of() : patch.things();

        List<WorldRecipe.PlotSpec> plots = save.plots().stream()
                .map(p -> {
                    GardenerPatch.PlotPatch gp = plotPatches.get(p.plotId());
                    if (gp == null) {
                        return p;
                    }
                    String name = gp.displayTitle() == null ? p.name() : gp.displayTitle();
                    String desc = gp.description() == null ? p.description() : gp.description();
                    return new WorldRecipe.PlotSpec(p.plotId(), name, p.region(), p.locationX(), p.locationY(), desc);
                })
                .toList();

        Function<UUID, GardenerPatch.ThingPatch> patchLookup = thingPatches::get;

        List<WorldRecipe.FixtureSpec> fixtures = save.fixtures().stream()
                .map(f -> {
                    GardenerPatch.ThingPatch tp = patchLookup.apply(f.id());
                    if (tp == null) {
                        return f;
                    }
                    String name = tp.displayName() == null ? f.name() : tp.displayName();
                    String desc = tp.description() == null ? f.description() : tp.description();
                    return new WorldRecipe.FixtureSpec(f.id(), name, desc, f.ownerId(), f.visible(), f.cells());
                })
                .toList();

        List<GameSave.ItemRecipe> items = save.items().stream()
                .map(i -> {
                    GardenerPatch.ThingPatch tp = patchLookup.apply(i.id());
                    if (tp == null) {
                        return i;
                    }
                    String name = tp.displayName() == null ? i.name() : tp.displayName();
                    String desc = tp.description() == null ? i.description() : tp.description();
                    return new ItemRecipeBuilder()
                            .withId(i.id())
                            .withName(name)
                            .withDescription(desc)
                            .withOwnerId(i.ownerId())
                            .withVisible(i.visible())
                            .withFixture(i.fixture())
                            .withKeyString(i.keyString())
                            .withFootprint(i.footprintWidth(), i.footprintHeight())
                            .withCapacity(i.capacityWidth(), i.capacityHeight())
                            .withWeaponDamage(i.weaponDamage())
                            .withArmorMitigation(i.armorMitigation())
                            .withCells(i.cells())
                            .build();
                })
                .toList();

        List<GameSave.ActorRecipe> actors = save.actors().stream()
                .map(a -> {
                    GardenerPatch.ThingPatch tp = patchLookup.apply(a.id());
                    if (tp == null) {
                        return a;
                    }
                    String name = tp.displayName() == null ? a.name() : tp.displayName();
                    String desc = tp.description() == null ? a.description() : tp.description();
                    return new ActorRecipeBuilder()
                            .withId(a.id())
                            .withName(name)
                            .withDescription(desc)
                            .withOwnerId(a.ownerId())
                            .withVisible(a.visible())
                            .withSkills(a.skills())
                            .withEquippedMainHandItemId(a.equippedMainHandItemId())
                            .withEquippedBodyItemId(a.equippedBodyItemId())
                            .withCells(a.cells())
                            .build();
                })
                .toList();

        return new GameSave(save.seed(), save.startPlotId(), save.preamble(), plots, save.gates(), fixtures, items, actors);
    }
}

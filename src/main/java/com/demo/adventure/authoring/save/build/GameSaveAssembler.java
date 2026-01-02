package com.demo.adventure.authoring.save.build;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellSpec;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.authoring.save.io.FootprintRule;
import com.demo.adventure.authoring.save.io.FootprintRuleLoader;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Builds a full game world from a {@link GameSave}, including map, fixtures, items, and actors.
 */
public final class GameSaveAssembler {

    /**
     * Build a {@link WorldBuildResult} from an in-memory {@link GameSave}.
     *
     * @param save structured save definition to assemble
     * @return fully built result with registry and validation report
     * @throws GameBuilderException when validation or build fails
     */
    // Pattern: Verification
    // - Assembles the world with validation and fails loudly on invariant violations.
    public WorldBuildResult apply(GameSave save) throws GameBuilderException {
        return apply(save, List.of());
    }

    public WorldBuildResult apply(GameSave save, List<FootprintRule> rules) throws GameBuilderException {
        Objects.requireNonNull(save, "save");
        List<FootprintRule> footprintRules = rules == null ? List.of() : rules;

        // Verification: assemble GameSave into a registry with validation; fails loud via GameBuilderException.
        WorldRecipe recipe = new WorldRecipe(save.seed(), save.startPlotId(), save.plots(), save.gates(), save.fixtures());
        WorldBuildResult base = new WorldAssembler().build(recipe);
        KernelRegistry registry = base.registry();

        for (GameSave.ActorRecipe actor : save.actors()) {
            Actor built = new ActorBuilder()
                    .withId(actor.id())
                    .withLabel(actor.name())
                    .withDescription(actor.description())
                    .withOwnerId(actor.ownerId())
                    .withVisible(actor.visible())
                    .withSkills(actor.skills())
                    .withEquippedMainHandItemId(actor.equippedMainHandItemId())
                    .withEquippedBodyItemId(actor.equippedBodyItemId())
                    .build();
            applyCells(built, actor.cells());
            registry.register(built);
        }

        for (GameSave.ItemRecipe item : save.items()) {
            Item built = new ItemBuilder()
                    .withId(item.id())
                    .withLabel(item.name())
                    .withDescription(item.description())
                    .withOwnerId(item.ownerId())
                    .withVisible(item.visible())
                    .withFixture(item.fixture())
                    .withFootprint(item.footprintWidth(), item.footprintHeight())
                    .withCapacity(item.capacityWidth(), item.capacityHeight())
                    .withWeaponDamage(item.weaponDamage())
                    .withArmorMitigation(item.armorMitigation())
                    .build();
            built.setKey(item.keyString());
            applyFootprintRules(built, footprintRules);
            applyCells(built, item.cells());
            registry.register(built);
        }

        return new WorldBuildResult(save.startPlotId(), save.seed(), registry, base.report());
    }

    /**
     * Convenience: load a YAML save from disk and apply it.
     *
     * @param yamlPath path to a GameSave YAML document
     * @return fully built result with registry and validation report
     * @throws IOException            when the file cannot be read
     * @throws GameBuilderException   when validation or build fails
     */
    public WorldBuildResult apply(Path yamlPath) throws IOException, GameBuilderException {
        GameSave save = loadGameSave(yamlPath);
        List<FootprintRule> rules = loadFootprintRules(yamlPath);
        return apply(save, rules);
    }

    private static GameSave loadGameSave(Path yamlPath) throws IOException {
        if (yamlPath == null) {
            throw new IOException("Game save path missing.");
        }
        if (!Files.exists(yamlPath)) {
            throw new IOException("File not found: " + yamlPath);
        }
        if (yamlPath.getFileName() != null && yamlPath.getFileName().toString().equalsIgnoreCase("game.yaml")) {
            try {
                return StructuredGameSaveLoader.load(yamlPath);
            } catch (Exception ex) {
                throw new IOException("Failed to load structured game: " + ex.getMessage(), ex);
            }
        }
        return GameSaveYamlLoader.load(yamlPath);
    }

    private static List<FootprintRule> loadFootprintRules(Path yamlPath) throws IOException {
        if (yamlPath == null) {
            return List.of();
        }
        Path base = yamlPath.getParent();
        if (base == null) {
            return List.of();
        }
        Path worldRules = base.resolve("world/footprints.yaml");
        if (Files.exists(worldRules)) {
            return FootprintRuleLoader.load(worldRules);
        }
        Path localRules = base.resolve("footprints.yaml");
        return FootprintRuleLoader.load(localRules);
    }

    private static void applyFootprintRules(Item item, List<FootprintRule> rules) {
        if (item == null || rules == null || rules.isEmpty()) {
            return;
        }
        double w = item.getFootprintWidth();
        double h = item.getFootprintHeight();
        // If an explicit footprint was set (bigger than the 0.1 default), leave it alone.
        if (w > 0.11 || h > 0.11) {
            return;
        }
        String label = item.getLabel();
        for (FootprintRule rule : rules) {
            if (rule != null && rule.matches(label)) {
                item.withSize(rule.width(), rule.height());
                return;
            }
        }
    }

    private static void applyCells(Thing thing, Map<String, CellSpec> specs) {
        if (thing == null || specs == null || specs.isEmpty()) {
            return;
        }
        Map<String, Cell> cells = new HashMap<>();
        for (Map.Entry<String, CellSpec> entry : specs.entrySet()) {
            CellSpec spec = entry.getValue();
            if (spec != null) {
                cells.put(entry.getKey(), spec.toCell());
            }
        }
        thing.setCells(cells);
    }
}

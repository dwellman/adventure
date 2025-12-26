package com.demo.adventure.engine.mechanics.crafting;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.engine.mechanics.cells.CellTransferResult;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.HasResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SearchResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SkillResolver;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Thing;

import java.util.ArrayList;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Minimal crafting table that evaluates key expressions (HAS, SEARCH, DICE)
 * against the player's inventory using the kernel registry, consumes ingredients,
 * and produces crafted items.
 */
public final class CraftingTable {
    private static final Logger LOG = CraftingLog.get();

    private static final Map<String, CraftingRecipe> HARDCODED_FALLBACK = Map.of(
            "TORCH", new CraftingRecipeBuilder()
                    .withName("Torch")
                    .withExpression("HAS(\"Stick\") && HAS(\"Rags\")")
                    .withConsume(List.of("Stick", "Rags"))
                    .withRequirements(List.of("Stick", "Rags"))
                    .withSkillTag("")
                    .withEmitLabel("Torch")
                    .withEmitDescription("A simple torch made of stick and rags.")
                    .build(),
            "SOAKED TORCH", new CraftingRecipeBuilder()
                    .withName("Soaked Torch")
                    .withExpression("HAS(\"Torch\") && HAS(\"Kerosene\")")
                    .withConsume(List.of("Torch", "Kerosene"))
                    .withRequirements(List.of("Torch", "Kerosene"))
                    .withSkillTag("")
                    .withEmitLabel("Soaked Torch")
                    .withEmitDescription("A torch soaked in kerosene, ready to be lit.")
                    .build(),
            "LIT TORCH", new CraftingRecipeBuilder()
                    .withName("Lit Torch")
                    .withExpression("HAS(\"Soaked Torch\") && HAS(\"Flint\") && HAS(\"River Stone\")")
                    .withConsume(List.of("Soaked Torch"))
                    .withRequirements(List.of("Soaked Torch", "Flint", "River Stone"))
                    .withSkillTag("")
                    .withEmitLabel("Lit Torch")
                    .withEmitDescription("A lit torch casting bright light.")
                    .build()
    );

    private static final Map<String, CraftingRecipe> DEFAULT_RECIPES = loadDefaultRecipes();

    private final KernelRegistry registry;
    private final UUID actorId;
    private final Map<String, CraftingRecipe> recipes;

    public CraftingTable(KernelRegistry registry, UUID actorId) {
        this(registry, actorId, null);
    }

    public CraftingTable(KernelRegistry registry, UUID actorId, Map<String, CraftingRecipe> recipes) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.actorId = Objects.requireNonNull(actorId, "actorId");
        this.recipes = recipes == null ? DEFAULT_RECIPES : normalizeKeys(recipes);
    }

    /**
     * Attempts to craft the requested item. Returns true on success, false if the recipe
     * is unknown or the key expression evaluates to false. Throws on malformed expressions.
     */
    public boolean craft(String recipeName) throws KeyExpressionCompileException {
        if (recipeName == null) {
            return false;
        }
        CraftingRecipe recipe = recipes.get(recipeName.toUpperCase());
        if (recipe == null) {
            return false;
        }

        HasResolver hasResolver = KeyExpressionEvaluator.registryHasResolver(registry, actorId);
        SearchResolver searchResolver = KeyExpressionEvaluator.registrySearchResolver(registry, actorId);
        SkillResolver skillResolver = KeyExpressionEvaluator.registrySkillResolver(registry, actorId);
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, actorId);

        LOG.info(() -> "Craft attempt: " + recipeName + " requires " + recipe.expression());
        boolean ready = KeyExpressionEvaluator.evaluate(
                recipe.expression(),
                hasResolver,
                searchResolver,
                skillResolver,
                attributeResolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
        LOG.info(() -> "Craft check: " + recipeName + " -> " + ready);
        if (!ready) {
            LOG.info(() -> "Craft fail: " + recipeName + " (requirements not met)");
            return false;
        }
        if (!recipe.skillTag().isBlank() && !skillResolver.hasSkill(recipe.skillTag())) {
            LOG.info(() -> "Craft fail: " + recipeName + " (missing skill: " + recipe.skillTag() + ")");
            return false;
        }

        List<Thing> consumed = consumeIngredients(recipe.consume());
        if (consumed.size() != recipe.consume().size()) {
            LOG.info(() -> "Craft fail: " + recipeName + " (missing ingredients)");
            return false;
        }

        Item crafted = new ItemBuilder()
                .withLabel(recipe.emitLabel())
                .withDescription(recipe.emitDescription())
                .withOwnerId(actorId)
                .build();
        crafted.setVisible(true);
        registry.register(crafted);
        transferCells(consumed, crafted);
        LOG.info(() -> "Craft success: " + recipe.emitLabel() + " created for actor " + actorId);
        return true;
    }

    /**
     * Returns a list of missing requirements for the recipe, using the current registry state.
     */
    public List<String> missingRequirements(String recipeName) {
        CraftingRecipe recipe = recipes.get(recipeName == null ? "" : recipeName.toUpperCase());
        if (recipe == null) {
            return List.of();
        }
        HasResolver hasResolver = KeyExpressionEvaluator.registryHasResolver(registry, actorId);
        SkillResolver skillResolver = KeyExpressionEvaluator.registrySkillResolver(registry, actorId);
        List<String> missing = new ArrayList<>();
        if (!recipe.skillTag().isBlank() && !skillResolver.hasSkill(recipe.skillTag())) {
            missing.add("Skill:" + recipe.skillTag());
        }
        missing.addAll(recipe.requirements().stream()
                .filter(req -> !hasResolver.has(req))
                .toList());
        return missing;
    }

    public Map<String, CraftingRecipe> getRecipes() {
        return recipes;
    }

    public CraftingRecipe findRecipe(String recipeName) {
        if (recipeName == null) {
            return null;
        }
        return recipes.get(recipeName.trim().toUpperCase(Locale.ROOT));
    }

    private List<Thing> consumeIngredients(List<String> labels) {
        List<Thing> consumed = new ArrayList<>();
        for (String label : labels) {
            Optional<Thing> match = findOwnedThing(label);
            if (match.isEmpty()) {
                return List.of();
            }
            registry.moveOwnership(match.get().getId(), null);
            consumed.add(match.get());
            LOG.info(() -> "Consume: " + label + " from actor " + actorId);
        }
        return consumed;
    }

    private Optional<Thing> findOwnedThing(String label) {
        return registry.getEverything().values().stream()
                .filter(Objects::nonNull)
                .filter(t -> actorId.equals(t.getOwnerId()))
                .filter(t -> label.equalsIgnoreCase(t.getLabel()))
                .findFirst();
    }

    private void transferCells(List<Thing> sources, Item crafted) {
        if (sources == null || sources.isEmpty() || crafted == null) {
            return;
        }
        for (Thing source : sources) {
            if (source == null || source.getCells().isEmpty()) {
                continue;
            }
            for (var entry : source.getCells().entrySet()) {
                String cellName = entry.getKey();
                Cell cell = entry.getValue();
                if (cell == null) {
                    continue;
                }
                if (crafted.getCell(cellName) == null) {
                    crafted.setCell(cellName, new Cell(cell.getCapacity(), 0));
                }
                CellTransferResult result = CellOps.transfer(source, crafted, cellName, cell.getAmount());
                if (result != null) {
                    registry.recordCellMutation(result.fromReceipt());
                    registry.recordCellMutation(result.toReceipt());
                    registry.recordCellTransfer(result.transferReceipt());
                }
            }
        }
    }

    private static Map<String, CraftingRecipe> normalizeKeys(Map<String, CraftingRecipe> recipes) {
        Map<String, CraftingRecipe> normalized = new java.util.HashMap<>();
        recipes.forEach((k, v) -> {
            String key = k == null ? "" : k.trim().toUpperCase(java.util.Locale.ROOT);
            if (!key.isEmpty() && v != null) {
                normalized.put(key, v);
            }
        });
        return normalized;
    }

    private static Map<String, CraftingRecipe> loadDefaultRecipes() {
        String[] candidates = {"games/island/world/crafting.yaml", "games/island/crafting.yaml"};
        for (String candidate : candidates) {
            try (var in = CraftingTable.class.getClassLoader().getResourceAsStream(candidate)) {
                if (in == null) {
                    continue;
                }
                Map<String, CraftingRecipe> loaded = CraftingRecipeLoader.load(in);
                if (loaded != null && !loaded.isEmpty()) {
                    return Map.copyOf(normalizeKeys(loaded));
                }
            } catch (Exception ex) {
                LOG.warning("Falling back to hardcoded recipes: " + ex.getMessage());
                break;
            }
        }
        return Map.copyOf(HARDCODED_FALLBACK);
    }
}

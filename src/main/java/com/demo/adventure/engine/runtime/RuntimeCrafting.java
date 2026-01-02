package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.mechanics.crafting.CraftingTable;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SkillResolver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

final class RuntimeCrafting {
    private final GameRuntime runtime;

    RuntimeCrafting(GameRuntime runtime) {
        this.runtime = runtime;
    }

    void craft(String target) {
        if (target.isBlank()) {
            CraftingTable table = table();
            String names = table.getRecipes().values().stream()
                    .map(CraftingRecipe::emitLabel)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("(none)");
            runtime.narrateMetaLines(List.of(
                    "Known recipes: " + names + ".",
                    "Try HOW CRAFT <item> for details."
            ));
            return;
        }
        CraftingTable table = table();
        try {
            boolean crafted = table.craft(target);
            if (crafted) {
                runtime.refreshInventory();
                runtime.narrateMeta("You craft a " + target + ".");
            } else {
                var missing = table.missingRequirements(target);
                if (table.findRecipe(target) == null) {
                    String names = table.getRecipes().values().stream()
                            .map(CraftingRecipe::emitLabel)
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("(none)");
                    runtime.narrateMeta("No recipe for that. Known craftables: " + names + ".");
                    return;
                }
                if (!missing.isEmpty()) {
                    String needs = String.join(", ", missing);
                    runtime.narrateMeta("You still need: " + needs + ".");
                } else {
                    runtime.narrateMeta("You lack the ingredients or know-how to craft that.");
                }
            }
        } catch (Exception ex) {
            runtime.narrateMeta("Crafting failed: " + ex.getMessage());
        }
    }

    void how(String argument) {
        if (argument.isBlank()) {
            CraftingTable table = table();
            String names = table.getRecipes().values().stream()
                    .map(CraftingRecipe::emitLabel)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("(none)");
            runtime.narrateMetaLines(List.of(
                    "Known craft targets: " + names + ".",
                    "Use HOW CRAFT <item> to see skill and ingredients."
            ));
            return;
        }
        String trimmed = normalizeHowCraftTarget(argument);
        if (trimmed.isBlank()) {
            runtime.narrateMeta("Which item? Try HOW CRAFT TORCH.");
            return;
        }

        CraftingTable table = table();
        CraftingRecipe recipe = table.findRecipe(trimmed);
        if (recipe == null) {
            String names = table.getRecipes().values().stream()
                    .map(CraftingRecipe::emitLabel)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("(none)");
            runtime.narrateMeta("No known way to craft '" + trimmed + "'. Known: " + names + ".");
            return;
        }

        SkillResolver skillResolver = KeyExpressionEvaluator.registrySkillResolver(runtime.registry(), runtime.playerId());
        boolean skillRequired = recipe.skillTag() != null && !recipe.skillTag().isBlank();
        boolean hasSkill = !skillRequired || skillResolver.hasSkill(recipe.skillTag());

        List<String> missing = table.missingRequirements(recipe.name());
        HashSet<String> missingNormalized = new HashSet<>();
        for (String m : missing) {
            missingNormalized.add(m.trim().toLowerCase(Locale.ROOT));
        }

        List<String> lines = new ArrayList<>();
        lines.add("How to craft " + recipe.emitLabel() + ":");
        if (skillRequired) {
            lines.add("  Skill: " + recipe.skillTag() + (hasSkill ? " (known)" : " (missing)") + ".");
        }
        if (!recipe.requirements().isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (String req : recipe.requirements()) {
                if (missingNormalized.contains(req.toLowerCase(Locale.ROOT))) {
                    parts.add(req + " (missing)");
                } else {
                    parts.add(req);
                }
            }
            lines.add("  Ingredients: " + String.join(", ", parts));
        }

        if (missing.isEmpty()) {
            lines.add("  Status: ready to craft with CRAFT " + recipe.emitLabel() + ".");
        } else {
            lines.add("  Status: missing " + String.join(", ", missing) + ".");
        }
        runtime.narrateMetaLines(lines);
    }

    private CraftingTable table() {
        return new CraftingTable(runtime.registry(), runtime.playerId(), runtime.craftingRecipes());
    }

    private String normalizeHowCraftTarget(String argument) {
        if (argument == null || argument.isBlank()) {
            return "";
        }
        List<Token> tokens = trimEolTokens(CommandScanner.scan(argument, runtime.extraKeywords()));
        if (tokens.isEmpty()) {
            return "";
        }
        int start = 0;
        if (tokens.get(0).type == TokenType.MAKE) {
            start = 1;
        }
        return tokens.subList(start, tokens.size()).stream()
                .map(t -> t.lexeme)
                .reduce((a, b) -> a + " " + b)
                .orElse("")
                .trim();
    }

    private List<Token> trimEolTokens(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<Token> trimmed = new ArrayList<>(tokens);
        while (!trimmed.isEmpty() && trimmed.get(trimmed.size() - 1).type == TokenType.EOL) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }
}

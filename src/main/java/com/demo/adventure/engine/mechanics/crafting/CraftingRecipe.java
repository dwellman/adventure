package com.demo.adventure.engine.mechanics.crafting;

import java.util.List;
import java.util.Objects;

/**
 * Immutable definition of a crafting recipe driven by key expressions.
 */
public record CraftingRecipe(
        String name,
        String expression,
        List<String> consume,
        List<String> requirements,
        String skillTag,
        String emitLabel,
        String emitDescription
) {
    public CraftingRecipe {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(expression, "expression");
        consume = List.copyOf(Objects.requireNonNullElse(consume, List.of()));
        requirements = requirements == null || requirements.isEmpty() ? consume : List.copyOf(requirements);
        skillTag = skillTag == null ? "" : skillTag;
        Objects.requireNonNull(emitLabel, "emitLabel");
        emitDescription = emitDescription == null ? "" : emitDescription;
    }
}

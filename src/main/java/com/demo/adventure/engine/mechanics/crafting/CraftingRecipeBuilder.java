package com.demo.adventure.engine.mechanics.crafting;

import java.util.List;
import java.util.Objects;

/**
 * Builder for {@link CraftingRecipe} to validate inputs before construction.
 */
public final class CraftingRecipeBuilder {
    private String name;
    private String expression;
    private List<String> consume = List.of();
    private List<String> requirements = List.of();
    private String skillTag = "";
    private String emitLabel;
    private String emitDescription = "";

    public CraftingRecipeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public CraftingRecipeBuilder withExpression(String expression) {
        this.expression = expression;
        return this;
    }

    public CraftingRecipeBuilder withConsume(List<String> consume) {
        this.consume = consume == null ? List.of() : consume;
        return this;
    }

    public CraftingRecipeBuilder withRequirements(List<String> requirements) {
        this.requirements = requirements == null ? List.of() : requirements;
        return this;
    }

    public CraftingRecipeBuilder withSkillTag(String skillTag) {
        this.skillTag = skillTag == null ? "" : skillTag;
        return this;
    }

    public CraftingRecipeBuilder withEmitLabel(String emitLabel) {
        this.emitLabel = emitLabel;
        return this;
    }

    public CraftingRecipeBuilder withEmitDescription(String emitDescription) {
        this.emitDescription = emitDescription == null ? "" : emitDescription;
        return this;
    }

    public CraftingRecipe build() {
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Recipe name is required");
        }
        Objects.requireNonNull(expression, "expression");
        String resolvedEmitLabel = emitLabel == null || emitLabel.isBlank() ? name : emitLabel;
        List<String> resolvedConsume = consume == null ? List.of() : consume;
        List<String> resolvedRequirements = requirements == null || requirements.isEmpty()
                ? resolvedConsume
                : requirements;
        return new CraftingRecipe(
                name,
                expression,
                resolvedConsume,
                resolvedRequirements,
                skillTag == null ? "" : skillTag,
                resolvedEmitLabel,
                emitDescription == null ? "" : emitDescription
        );
    }
}

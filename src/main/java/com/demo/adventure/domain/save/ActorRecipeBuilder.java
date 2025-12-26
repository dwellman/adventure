package com.demo.adventure.domain.save;

import com.demo.adventure.engine.mechanics.cells.CellSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Builder for {@link GameSave.ActorRecipe} to keep construction readable as fields evolve.
 */
public final class ActorRecipeBuilder {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private boolean visible = true;
    private List<String> skills = new ArrayList<>();
    private UUID equippedMainHandItemId;
    private UUID equippedBodyItemId;
    private Map<String, CellSpec> cells = Map.of();

    public ActorRecipeBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ActorRecipeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ActorRecipeBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ActorRecipeBuilder withOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public ActorRecipeBuilder withVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public ActorRecipeBuilder withSkills(List<String> skills) {
        this.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
        return this;
    }

    public ActorRecipeBuilder withEquippedMainHandItemId(UUID itemId) {
        this.equippedMainHandItemId = itemId;
        return this;
    }

    public ActorRecipeBuilder withEquippedBodyItemId(UUID itemId) {
        this.equippedBodyItemId = itemId;
        return this;
    }

    public ActorRecipeBuilder addSkill(String skill) {
        if (skill != null && !skill.isBlank()) {
            this.skills.add(skill);
        }
        return this;
    }

    public ActorRecipeBuilder withCells(Map<String, CellSpec> cells) {
        this.cells = cells == null ? Map.of() : cells;
        return this;
    }

    public GameSave.ActorRecipe build() {
        if (id == null) {
            throw new IllegalStateException("Actor id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Actor name is required");
        }
        if (ownerId == null) {
            throw new IllegalStateException("Actor ownerId is required");
        }
        return new GameSave.ActorRecipe(id, name, description, ownerId, visible, skills, equippedMainHandItemId, equippedBodyItemId, cells);
    }
}

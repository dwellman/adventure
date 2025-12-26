package com.demo.adventure.domain.save;

import com.demo.adventure.engine.mechanics.cells.CellSpec;

import java.util.Map;
import java.util.UUID;

/**
 * Builder for {@link GameSave.ItemRecipe} to keep construction readable as fields evolve.
 */
public final class ItemRecipeBuilder {
    private UUID id;
    private String name;
    private String description;
    private UUID ownerId;
    private boolean visible = true;
    private boolean fixture = false;
    private String keyString = "true";
    private double footprintWidth = 0.1;
    private double footprintHeight = 0.1;
    private double capacityWidth = 0.0;
    private double capacityHeight = 0.0;
    private long weaponDamage = 0L;
    private long armorMitigation = 0L;
    private Map<String, CellSpec> cells = Map.of();

    public ItemRecipeBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ItemRecipeBuilder withName(String name) {
        this.name = name;
        return this;
    }

    public ItemRecipeBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ItemRecipeBuilder withOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public ItemRecipeBuilder withVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public ItemRecipeBuilder withFixture(boolean fixture) {
        this.fixture = fixture;
        return this;
    }

    public ItemRecipeBuilder withKeyString(String keyString) {
        this.keyString = keyString;
        return this;
    }

    public ItemRecipeBuilder withFootprint(double width, double height) {
        this.footprintWidth = width;
        this.footprintHeight = height;
        return this;
    }

    public ItemRecipeBuilder withCapacity(double width, double height) {
        this.capacityWidth = width;
        this.capacityHeight = height;
        return this;
    }

    public ItemRecipeBuilder withWeaponDamage(long weaponDamage) {
        this.weaponDamage = weaponDamage;
        return this;
    }

    public ItemRecipeBuilder withArmorMitigation(long armorMitigation) {
        this.armorMitigation = armorMitigation;
        return this;
    }

    public ItemRecipeBuilder withCells(Map<String, CellSpec> cells) {
        this.cells = cells == null ? Map.of() : cells;
        return this;
    }

    public GameSave.ItemRecipe build() {
        if (id == null) {
            throw new IllegalStateException("Item id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalStateException("Item name is required");
        }
        if (ownerId == null) {
            throw new IllegalStateException("Item ownerId is required");
        }
        return new GameSave.ItemRecipe(
                id,
                name,
                description,
                ownerId,
                visible,
                fixture,
                keyString,
                footprintWidth,
                footprintHeight,
                capacityWidth,
                capacityHeight,
                weaponDamage,
                armorMitigation,
                cells
        );
    }
}

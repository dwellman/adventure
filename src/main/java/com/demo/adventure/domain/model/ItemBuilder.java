package com.demo.adventure.domain.model;

import java.util.UUID;

public final class ItemBuilder {
    private UUID id;
    private String label;
    private String description;
    private UUID ownerId;
    private boolean visible = true;
    private boolean fixture = false;
    private double footprintWidth = 0.1;
    private double footprintHeight = 0.1;
    private double capacityWidth = 0.0;
    private double capacityHeight = 0.0;
    private long weaponDamage = 0L;
    private long armorMitigation = 0L;

    public ItemBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ItemBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    public ItemBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ItemBuilder withOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public ItemBuilder withOwnerId(Thing owner) {
        this.ownerId = owner == null ? null : owner.getId();
        return this;
    }

    public ItemBuilder withVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public ItemBuilder withFixture(boolean fixture) {
        this.fixture = fixture;
        return this;
    }

    public ItemBuilder withFootprint(double width, double height) {
        this.footprintWidth = width;
        this.footprintHeight = height;
        return this;
    }

    public ItemBuilder withCapacity(double width, double height) {
        this.capacityWidth = width;
        this.capacityHeight = height;
        return this;
    }

    public ItemBuilder withWeaponDamage(long weaponDamage) {
        this.weaponDamage = weaponDamage;
        return this;
    }

    public ItemBuilder withArmorMitigation(long armorMitigation) {
        this.armorMitigation = armorMitigation;
        return this;
    }

    public Item build() {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Item label is required");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("Item ownerId is required");
        }
        UUID idToUse = id == null ? UUID.randomUUID() : id;
        Item item = new Item(idToUse, label, description, ownerId);
        item.setVisible(visible);
        item.setKey("true");
        item.setFixture(fixture);
        item.withSize(footprintWidth, footprintHeight);
        if (capacityWidth > 0 && capacityHeight > 0) {
            item.withCapacity(capacityWidth, capacityHeight, 1.0);
        }
        item.setWeaponDamage(weaponDamage);
        item.setArmorMitigation(armorMitigation);
        return item;
    }
}

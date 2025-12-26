package com.demo.adventure.domain.model;

import java.util.UUID;

public final class Item extends Thing {
    private boolean fixture;
    private double footprintWidth;
    private double footprintHeight;
    private double capacityWidth;
    private double capacityHeight;
    private long weaponDamage;
    private long armorMitigation;

    public Item(UUID id, String label, String description, UUID ownerId) {
        super(id, ThingKind.ITEM, label, description, ownerId);
        this.fixture = false;
        this.footprintWidth = 0.1;
        this.footprintHeight = 0.1;
        this.capacityWidth = 0.0;
        this.capacityHeight = 0.0;
        this.weaponDamage = 0L;
        this.armorMitigation = 0L;
    }

    public boolean isFixture() {
        return fixture;
    }

    public void setFixture(boolean fixture) {
        this.fixture = fixture;
    }

    /**
     * Set a normalized size footprint (0..1) used for packing checks.
     */
    public Item withSize(double width, double height) {
        this.footprintWidth = Math.max(0.01, Math.min(1.0, width));
        this.footprintHeight = Math.max(0.01, Math.min(1.0, height));
        return this;
    }

    /**
     * Set a normalized capacity (0..1 for each dimension) for container-like items.
     */
    public Item withCapacity(double width, double height, double depth) {
        this.capacityWidth = Math.max(0.1, Math.min(1.0, width));
        this.capacityHeight = Math.max(0.1, Math.min(1.0, height));
        return this;
    }

    public double getFootprintWidth() {
        return footprintWidth;
    }

    public double getFootprintHeight() {
        return footprintHeight;
    }

    public double getCapacityWidth() {
        return capacityWidth;
    }

    public double getCapacityHeight() {
        return capacityHeight;
    }

    public long getWeaponDamage() {
        return weaponDamage;
    }

    public void setWeaponDamage(long weaponDamage) {
        this.weaponDamage = Math.max(0L, weaponDamage);
    }

    public long getArmorMitigation() {
        return armorMitigation;
    }

    public void setArmorMitigation(long armorMitigation) {
        this.armorMitigation = Math.max(0L, armorMitigation);
    }
}

package com.demo.adventure.authoring.save.build;

import java.util.List;

/**
 * Human-friendly bill of materials describing a built world.
 */
public final class WorldBillOfMaterials {
    private final String title;
    private final List<Section> sections;

    /**
     * Create a new bill of materials.
     *
     * @param title    label for the BOM
     * @param sections ordered list of sections
     */
    public WorldBillOfMaterials(String title, List<Section> sections) {
        this.title = title;
        this.sections = List.copyOf(sections == null ? List.of() : sections);
    }

    /** @return BOM title. */
    public String getTitle() {
        return title;
    }

    /** @return ordered sections. */
    public List<Section> getSections() {
        return sections;
    }

    /**
     * Structured grouping of BOM entries.
     *
     * @param title   section label
     * @param entries entries inside the section
     */
    public record Section(String title, List<Entry> entries) {
    }

    /**
     * Single BOM line.
     *
     * @param quantity how many of the item
     * @param name     human-readable name
     * @param notes    optional details such as fixture location
     */
    public record Entry(int quantity, String name, String notes) {
    }
}

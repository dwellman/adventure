package com.demo.adventure.domain.model;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public abstract class Thing {

    private final UUID id;
    private final ThingKind kind;

    private String label;
    private final Description description;

    private UUID ownerId;
    private boolean visible;
    private String key;
    private String visibilityKey;
    private int ttl;
    private int size;
    private int weight;
    private int volume;
    private final Map<String, Cell> cells;

    protected Thing(UUID id, ThingKind kind, String label, String descriptionText, UUID ownerId) {
        if (ownerId == null) {
            throw new IllegalArgumentException("Thing ownerId is required");
        }
        this.id = id;
        this.kind = kind;
        this.label = label;
        this.description = new Description(descriptionText);
        this.ownerId = ownerId;
        this.visible = true;
        this.key = "false";
        this.visibilityKey = "true";
        this.ttl = -1;
        this.size = 0;
        this.weight = 0;
        this.volume = 0;
        this.cells = new HashMap<>();
    }

    public UUID getId() {
        return id;
    }

    public ThingKind getKind() {
        return kind;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public boolean isOpen() {
        return KeyExpressionEvaluator.evaluate(key);
    }

    public boolean isOpen(KeyExpressionEvaluator.HasResolver hasResolver, KeyExpressionEvaluator.SearchResolver searchResolver) {
        return isOpen(
                hasResolver,
                searchResolver,
                null,
                KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public boolean isOpen(
            KeyExpressionEvaluator.HasResolver hasResolver,
            KeyExpressionEvaluator.SearchResolver searchResolver,
            KeyExpressionEvaluator.AttributeResolver attributeResolver,
            KeyExpressionEvaluator.AttributeResolutionPolicy attributePolicy
    ) {
        return KeyExpressionEvaluator.evaluate(
                key,
                hasResolver,
                searchResolver,
                null,
                attributeResolver,
                attributePolicy
        );
    }

    public boolean isVisible() {
        // Visibility uses the authoring flag plus a visibility key expression (separate from the open key).
        // Set visible=false to hide entirely; set visibilityKey to expressions like HAS("Lit Torch") to gate visibility.
        return visible && KeyExpressionEvaluator.evaluate(visibilityKey);
    }

    public boolean isVisible(
            KeyExpressionEvaluator.HasResolver hasResolver,
            KeyExpressionEvaluator.SearchResolver searchResolver,
            KeyExpressionEvaluator.AttributeResolver attributeResolver,
            KeyExpressionEvaluator.AttributeResolutionPolicy attributePolicy
    ) {
        return visible && KeyExpressionEvaluator.evaluate(
                visibilityKey,
                hasResolver,
                searchResolver,
                null,
                attributeResolver,
                attributePolicy
        );
    }

    public boolean isVisibleFlag() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key == null ? "" : key;
    }

    public String getVisibilityKey() {
        return visibilityKey;
    }

    public void setVisibilityKey(String visibilityKey) {
        this.visibilityKey = visibilityKey == null ? "true" : visibilityKey;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public Map<String, Cell> getCells() {
        return Collections.unmodifiableMap(cells);
    }

    public Cell getCell(String name) {
        String key = normalizeCellKey(name);
        if (key.isBlank()) {
            return null;
        }
        return cells.get(key);
    }

    public void setCell(String name, Cell cell) {
        String key = normalizeCellKey(name);
        if (key.isBlank() || cell == null) {
            return;
        }
        cells.put(key, cell);
    }

    public void setCells(Map<String, Cell> replacements) {
        cells.clear();
        if (replacements == null || replacements.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Cell> entry : replacements.entrySet()) {
            String key = normalizeCellKey(entry.getKey());
            Cell cell = entry.getValue();
            if (!key.isBlank() && cell != null) {
                cells.put(key, cell);
            }
        }
    }

    public static String normalizeCellKey(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toUpperCase(Locale.ROOT);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description.getText();
    }

    public void setDescription(String descriptionText) {
        this.description.setText(descriptionText);
    }

    // Pattern: Learning
    // - Exposes description history so future passes can stay consistent with past edits.
    public List<DescriptionVersion> getDescriptionHistory() {
        return description.getHistory();
    }

    // Pattern: Learning
    // - Records description updates to build a feedback trail for later AI prompts.
    public void recordDescription(String descriptionText, int worldClock) {
        this.description.record(descriptionText, worldClock);
    }
}

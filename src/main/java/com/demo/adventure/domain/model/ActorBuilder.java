package com.demo.adventure.domain.model;

import java.util.List;
import java.util.UUID;

public final class ActorBuilder {
    private UUID id;
    private String label;
    private String description;
    private UUID ownerId;
    private boolean visible = true;
    private List<String> skills = new java.util.ArrayList<>();
    private UUID equippedMainHandItemId;
    private UUID equippedBodyItemId;

    public ActorBuilder withId(UUID id) {
        this.id = id;
        return this;
    }

    public ActorBuilder withLabel(String label) {
        this.label = label;
        return this;
    }

    public ActorBuilder withDescription(String description) {
        this.description = description;
        return this;
    }

    public ActorBuilder withOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
        return this;
    }

    public ActorBuilder withOwnerId(Thing owner) {
        this.ownerId = owner == null ? null : owner.getId();
        return this;
    }

    public ActorBuilder withVisible(boolean visible) {
        this.visible = visible;
        return this;
    }

    public ActorBuilder withSkills(List<String> skills) {
        this.skills = skills == null ? new java.util.ArrayList<>() : new java.util.ArrayList<>(skills);
        return this;
    }

    public ActorBuilder withEquippedMainHandItemId(UUID itemId) {
        this.equippedMainHandItemId = itemId;
        return this;
    }

    public ActorBuilder withEquippedBodyItemId(UUID itemId) {
        this.equippedBodyItemId = itemId;
        return this;
    }

    public Actor build() {
        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException("Actor label is required");
        }
        if (ownerId == null) {
            throw new IllegalArgumentException("Actor ownerId is required");
        }
        UUID idToUse = id == null ? UUID.randomUUID() : id;
        Actor actor = new Actor(idToUse, label, description, ownerId);
        actor.setVisible(visible);
        actor.setKey("true");
        actor.setSkills(skills);
        actor.setEquippedMainHandItemId(equippedMainHandItemId);
        actor.setEquippedBodyItemId(equippedBodyItemId);
        return actor;
    }
}

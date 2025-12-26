package com.demo.adventure.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public final class Actor extends Thing {
    private List<String> skills = new ArrayList<>();
    private UUID equippedMainHandItemId;
    private UUID equippedBodyItemId;

    public Actor(UUID id, String label, String description, UUID ownerId) {
        super(id, ThingKind.ACTOR, label, description, ownerId);
    }

    public List<String> getSkills() {
        return Collections.unmodifiableList(skills);
    }

    public void setSkills(List<String> skills) {
        this.skills = skills == null ? new ArrayList<>() : new ArrayList<>(skills);
    }

    public UUID getEquippedMainHandItemId() {
        return equippedMainHandItemId;
    }

    public void setEquippedMainHandItemId(UUID equippedMainHandItemId) {
        this.equippedMainHandItemId = equippedMainHandItemId;
    }

    public UUID getEquippedBodyItemId() {
        return equippedBodyItemId;
    }

    public void setEquippedBodyItemId(UUID equippedBodyItemId) {
        this.equippedBodyItemId = equippedBodyItemId;
    }
}

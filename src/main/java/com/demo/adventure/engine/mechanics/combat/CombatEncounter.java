package com.demo.adventure.engine.mechanics.combat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class CombatEncounter {
    private final UUID id;
    private final UUID locationId;
    private final List<UUID> participants;
    private final List<UUID> initiativeOrder;
    private final Set<UUID> defeated = new HashSet<>();
    private final Set<UUID> fled = new HashSet<>();
    private CombatState state;
    private int turnIndex;

    public CombatEncounter(
            UUID id,
            UUID locationId,
            List<UUID> participants,
            List<UUID> initiativeOrder,
            int turnIndex
    ) {
        this.id = id;
        this.locationId = locationId;
        this.participants = participants == null ? List.of() : List.copyOf(participants);
        this.initiativeOrder = initiativeOrder == null ? List.of() : List.copyOf(initiativeOrder);
        this.turnIndex = Math.max(0, Math.min(turnIndex, Math.max(0, this.initiativeOrder.size() - 1)));
        this.state = CombatState.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getLocationId() {
        return locationId;
    }

    public List<UUID> getParticipants() {
        return participants;
    }

    public List<UUID> getInitiativeOrder() {
        return initiativeOrder;
    }

    public int getTurnIndex() {
        return turnIndex;
    }

    public CombatState getState() {
        return state;
    }

    public void setState(CombatState state) {
        this.state = state == null ? CombatState.ENDED : state;
    }

    public UUID currentActorId() {
        if (initiativeOrder.isEmpty()) {
            return null;
        }
        return initiativeOrder.get(turnIndex);
    }

    public boolean isActiveParticipant(UUID actorId) {
        if (actorId == null || !participants.contains(actorId)) {
            return false;
        }
        return !defeated.contains(actorId) && !fled.contains(actorId);
    }

    public void markDefeated(UUID actorId) {
        if (actorId != null) {
            defeated.add(actorId);
        }
    }

    public void markFled(UUID actorId) {
        if (actorId != null) {
            fled.add(actorId);
        }
    }

    public boolean isDefeated(UUID actorId) {
        return actorId != null && defeated.contains(actorId);
    }

    public boolean isFled(UUID actorId) {
        return actorId != null && fled.contains(actorId);
    }

    public Set<UUID> getDefeated() {
        return Collections.unmodifiableSet(defeated);
    }

    public Set<UUID> getFled() {
        return Collections.unmodifiableSet(fled);
    }

    public UUID advanceTurn() {
        if (initiativeOrder.isEmpty()) {
            return null;
        }
        List<UUID> active = activeParticipants();
        if (active.isEmpty()) {
            return null;
        }
        int attempts = 0;
        while (attempts < initiativeOrder.size()) {
            turnIndex = (turnIndex + 1) % initiativeOrder.size();
            UUID candidate = initiativeOrder.get(turnIndex);
            if (isActiveParticipant(candidate)) {
                return candidate;
            }
            attempts++;
        }
        return null;
    }

    public List<UUID> activeParticipants() {
        List<UUID> active = new ArrayList<>();
        for (UUID participant : participants) {
            if (isActiveParticipant(participant)) {
                active.add(participant);
            }
        }
        return active;
    }
}

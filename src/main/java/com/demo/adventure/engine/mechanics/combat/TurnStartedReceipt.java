package com.demo.adventure.engine.mechanics.combat;

import java.util.UUID;

public record TurnStartedReceipt(
        UUID encounterId,
        UUID actorId,
        int turnIndex
) implements CombatReceipt {
}

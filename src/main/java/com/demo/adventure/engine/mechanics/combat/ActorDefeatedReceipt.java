package com.demo.adventure.engine.mechanics.combat;

import java.util.UUID;

public record ActorDefeatedReceipt(
        UUID actorId
) implements CombatReceipt {
}

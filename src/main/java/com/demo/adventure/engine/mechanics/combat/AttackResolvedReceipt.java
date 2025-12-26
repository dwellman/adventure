package com.demo.adventure.engine.mechanics.combat;

import java.util.UUID;

public record AttackResolvedReceipt(
        UUID attackerId,
        UUID targetId,
        boolean hit,
        long damageApplied
) implements CombatReceipt {
}

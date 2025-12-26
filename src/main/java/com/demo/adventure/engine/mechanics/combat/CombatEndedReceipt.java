package com.demo.adventure.engine.mechanics.combat;

import java.util.UUID;

public record CombatEndedReceipt(
        UUID encounterId,
        String outcome
) implements CombatReceipt {
}

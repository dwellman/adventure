package com.demo.adventure.engine.mechanics.combat;

import java.util.List;
import java.util.UUID;

public record CombatStartedReceipt(
        UUID encounterId,
        UUID locationId,
        List<UUID> participants,
        List<UUID> initiativeOrder
) implements CombatReceipt {
}

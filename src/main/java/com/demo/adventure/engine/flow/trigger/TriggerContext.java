package com.demo.adventure.engine.flow.trigger;

import com.demo.adventure.domain.kernel.KernelRegistry;

import java.util.UUID;

public record TriggerContext(
        KernelRegistry registry,
        UUID plotId,
        UUID playerId,
        UUID worldId
) {
}

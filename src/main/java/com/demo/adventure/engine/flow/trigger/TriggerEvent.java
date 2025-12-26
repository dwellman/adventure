package com.demo.adventure.engine.flow.trigger;

import java.util.UUID;

public record TriggerEvent(
        TriggerType type,
        String targetLabel,
        String objectLabel,
        UUID targetId,
        UUID objectId
) {
}

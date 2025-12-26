package com.demo.adventure.engine.flow.trigger;

public record TriggerAction(
        TriggerActionType type,
        String target,
        String owner,
        String text,
        String key,
        String visibilityKey,
        String cell,
        Long amount,
        String description,
        String reason,
        Boolean visible
) {
}

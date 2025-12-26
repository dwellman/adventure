package com.demo.adventure.engine.flow.trigger;

import java.util.List;

public record TriggerDefinition(
        String id,
        TriggerType type,
        String target,
        String object,
        String key,
        List<TriggerAction> actions
) {
    public TriggerDefinition {
        actions = actions == null ? List.of() : List.copyOf(actions);
    }
}

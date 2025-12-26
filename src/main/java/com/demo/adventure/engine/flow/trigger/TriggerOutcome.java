package com.demo.adventure.engine.flow.trigger;

import com.demo.adventure.engine.flow.loop.LoopResetReason;

import java.util.List;

public record TriggerOutcome(
        List<String> messages,
        LoopResetReason resetReason,
        String resetMessage,
        boolean endGame
) {
    public TriggerOutcome {
        messages = messages == null ? List.of() : List.copyOf(messages);
        resetMessage = resetMessage == null ? "" : resetMessage;
    }

    public boolean hasReset() {
        return resetReason != null;
    }

    public static TriggerOutcome empty() {
        return new TriggerOutcome(List.of(), null, "", false);
    }
}

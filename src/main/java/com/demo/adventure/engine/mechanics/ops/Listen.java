package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Thing;

/**
 * Minimal listen operation. For now it mirrors a descriptive inspect so that
 * games can add audio cues later without rewriting callers.
 */
public final class Listen {
    private Listen() {
    }

    public static String listen(Thing target) {
        if (target == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("You listen at ").append(target.getLabel() == null ? "here" : target.getLabel()).append('.');
        String desc = target.getDescription();
        if (desc != null && !desc.isBlank()) {
            sb.append(' ').append(desc);
        }
        return sb.toString();
    }
}

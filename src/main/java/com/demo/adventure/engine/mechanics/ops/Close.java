package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Thing;

public final class Close {
    private Close() {
    }

    public static String close(Thing target) {
        if (target == null) {
            return "";
        }
        target.setKey("false");
        String label = target.getLabel() == null ? "" : target.getLabel();
        return "You close " + label + ".";
    }
}

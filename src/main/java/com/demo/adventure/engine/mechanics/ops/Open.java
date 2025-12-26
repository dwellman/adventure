package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Thing;

public final class Open {
    private Open() {
    }

    public static String open(Thing target) {
        if (target == null) {
            return "";
        }
        target.setKey("true");
        String label = target.getLabel() == null ? "" : target.getLabel();
        return "You open " + label + ".";
    }
}

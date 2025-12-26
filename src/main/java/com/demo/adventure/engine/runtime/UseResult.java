package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.model.Thing;

public record UseResult(Thing source, Thing object, boolean valid) {
    public static UseResult invalid() {
        return new UseResult(null, null, false);
    }
}

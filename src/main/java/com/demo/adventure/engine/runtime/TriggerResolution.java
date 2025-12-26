package com.demo.adventure.engine.runtime;

record TriggerResolution(ResetContext reset, boolean endGame) {
    static TriggerResolution none() {
        return new TriggerResolution(null, false);
    }
}

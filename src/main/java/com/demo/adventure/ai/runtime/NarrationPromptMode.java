package com.demo.adventure.ai.runtime;

public enum NarrationPromptMode {
    SCENE("SCENE"),
    LOOK_TARGET("LOOK_TARGET"),
    LOOK_DIRECTION("LOOK_DIRECTION"),
    ACTION_RESULT("ACTION_RESULT"),
    COLOR_EVENT("COLOR_EVENT"),
    EMOTE("EMOTE"),
    CHECK_REQUEST("CHECK_REQUEST"),
    CHECK_RESULT("CHECK_RESULT");

    private final String label;

    NarrationPromptMode(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}

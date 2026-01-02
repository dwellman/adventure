package com.demo.adventure.engine.runtime;

public record InteractionState(InteractionType type, String expectedToken, String promptLine) {
    public static InteractionState none() {
        return new InteractionState(InteractionType.NONE, "", "");
    }

    public static InteractionState awaitingDice(String diceCall) {
        String expected = diceCall == null ? "" : diceCall.trim();
        String prompt = expected.isEmpty() ? "Roll dice." : "Roll " + expected + ".";
        return new InteractionState(InteractionType.AWAITING_DICE, expected, prompt);
    }
}

package com.demo.adventure.engine.command.handlers;

public record CommandOutcome(boolean endGame, boolean skipTurnAdvance) {

    public static CommandOutcome none() {
        return new CommandOutcome(false, false);
    }

    public static CommandOutcome endGameOutcome() {
        return new CommandOutcome(true, false);
    }

    public static CommandOutcome skipTurnAdvanceOutcome() {
        return new CommandOutcome(false, true);
    }
}

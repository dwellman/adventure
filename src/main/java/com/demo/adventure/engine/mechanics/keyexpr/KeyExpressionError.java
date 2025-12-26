package com.demo.adventure.engine.mechanics.keyexpr;

public record KeyExpressionError(Phase phase, String message, String expression, int position) {

    public enum Phase {
        COMPILE,
        EVALUATE
    }
}

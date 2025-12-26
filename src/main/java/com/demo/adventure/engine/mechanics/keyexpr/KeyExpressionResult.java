package com.demo.adventure.engine.mechanics.keyexpr;

public record KeyExpressionResult(boolean value, KeyExpressionError error) {

    public static KeyExpressionResult success(boolean value) {
        return new KeyExpressionResult(value, null);
    }

    public static KeyExpressionResult error(KeyExpressionError error) {
        return new KeyExpressionResult(false, error);
    }

    public boolean isSuccess() {
        return error == null;
    }
}

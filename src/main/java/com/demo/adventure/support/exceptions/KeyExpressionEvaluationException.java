package com.demo.adventure.support.exceptions;

import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionError;

public class KeyExpressionEvaluationException extends RuntimeException {
    private final KeyExpressionError error;

    public KeyExpressionEvaluationException(KeyExpressionError error) {
        super(error == null ? "Key expression evaluation error" : error.message());
        this.error = error;
    }

    public KeyExpressionError getError() {
        return error;
    }
}

package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.support.exceptions.KeyExpressionEvaluationException;

public final class UnknownReferenceException extends KeyExpressionEvaluationException {
    public UnknownReferenceException(KeyExpressionError error) {
        super(error);
    }
}

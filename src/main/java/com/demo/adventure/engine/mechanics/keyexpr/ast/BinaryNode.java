package com.demo.adventure.engine.mechanics.keyexpr.ast;

public record BinaryNode(
        KeyExpressionNode left, BinaryOperator operator, KeyExpressionNode right
) implements KeyExpressionNode {
}

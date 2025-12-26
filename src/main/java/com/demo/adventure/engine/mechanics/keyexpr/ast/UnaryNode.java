package com.demo.adventure.engine.mechanics.keyexpr.ast;

public record UnaryNode(UnaryOperator operator, KeyExpressionNode operand) implements KeyExpressionNode {
}

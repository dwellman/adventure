package com.demo.adventure.engine.mechanics.keyexpr.ast;

public sealed interface KeyExpressionNode permits
        BooleanLiteralNode,
        NumberLiteralNode,
        StringLiteralNode,
        IdentifierNode,
        AttributeAccessNode,
        UnaryNode,
        BinaryNode,
        FunctionCallNode {
}

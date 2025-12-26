package com.demo.adventure.engine.mechanics.keyexpr.ast;

import java.util.List;

public record FunctionCallNode(String name, List<KeyExpressionNode> arguments) implements KeyExpressionNode {
}

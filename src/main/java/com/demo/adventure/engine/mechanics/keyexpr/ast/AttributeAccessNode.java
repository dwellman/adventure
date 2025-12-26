package com.demo.adventure.engine.mechanics.keyexpr.ast;

import java.util.List;

public record AttributeAccessNode(String root, List<AccessSegment> segments) implements KeyExpressionNode {
}

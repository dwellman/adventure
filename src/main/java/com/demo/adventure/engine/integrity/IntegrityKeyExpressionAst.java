package com.demo.adventure.engine.integrity;

import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.FunctionCallNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.StringLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryNode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.Set;

final class IntegrityKeyExpressionAst {
    private IntegrityKeyExpressionAst() {
    }

    static List<FunctionCallNode> collectFunctions(KeyExpressionNode node) {
        if (node == null) {
            return List.of();
        }
        List<FunctionCallNode> functions = new ArrayList<>();
        Deque<KeyExpressionNode> stack = new ArrayDeque<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            KeyExpressionNode current = stack.pop();
            if (current instanceof FunctionCallNode call) {
                functions.add(call);
                for (KeyExpressionNode arg : call.arguments()) {
                    stack.push(arg);
                }
                continue;
            }
            if (current instanceof BinaryNode binary) {
                stack.push(binary.left());
                stack.push(binary.right());
                continue;
            }
            if (current instanceof UnaryNode unary) {
                stack.push(unary.operand());
                continue;
            }
        }
        return functions;
    }

    static String firstStringLiteral(FunctionCallNode call) {
        if (call == null || call.arguments().isEmpty()) {
            return null;
        }
        KeyExpressionNode arg = call.arguments().get(0);
        if (arg instanceof StringLiteralNode str) {
            return str.value();
        }
        return null;
    }

    static void collectHasReferences(KeyExpressionNode node, Set<String> collector) {
        if (node == null || collector == null) {
            return;
        }
        Deque<KeyExpressionNode> stack = new ArrayDeque<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            KeyExpressionNode current = stack.pop();
            if (current instanceof FunctionCallNode call) {
                String name = call.name() == null ? "" : call.name().trim().toUpperCase(Locale.ROOT);
                if ("HAS".equals(name)) {
                    String label = firstStringLiteral(call);
                    if (label != null) {
                        collector.add(IntegrityLabels.normalizeLabel(label));
                    }
                }
                for (KeyExpressionNode arg : call.arguments()) {
                    stack.push(arg);
                }
                continue;
            }
            if (current instanceof BinaryNode binary) {
                stack.push(binary.left());
                stack.push(binary.right());
            } else if (current instanceof UnaryNode unary) {
                stack.push(unary.operand());
            }
        }
    }
}

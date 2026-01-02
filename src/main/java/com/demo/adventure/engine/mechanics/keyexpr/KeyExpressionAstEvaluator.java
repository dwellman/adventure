package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.AttributeResolutionContext;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.AttributeResolutionPolicy;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.AttributeResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.DiceRoller;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.HasResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SearchResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SkillResolver;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AttributeAccessNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryOperator;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BooleanLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.FunctionCallNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.IdentifierNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.NumberLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.StringLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryOperator;
import com.demo.adventure.support.exceptions.KeyExpressionEvaluationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

final class KeyExpressionAstEvaluator {
    private static final Set<Integer> SUPPORTED_DICE_SIDES = Set.of(4, 6, 8, 10, 12, 20);

    private final DiceRoller diceRoller;
    private final boolean debugOutput;

    KeyExpressionAstEvaluator(DiceRoller diceRoller, boolean debugOutput) {
        this.diceRoller = diceRoller;
        this.debugOutput = debugOutput;
    }

    boolean evaluateBoolean(KeyExpressionNode node, KeyExpressionEvaluationContext ctx) {
        if (node instanceof BinaryNode binary) {
            BinaryOperator op = binary.operator();
            if (op == BinaryOperator.AND || op == BinaryOperator.OR) {
                return applyLogical(op, binary.left(), binary.right(), ctx);
            }
        }
        Object value = resolveValue(node, ctx);
        return toBoolean(value, ctx);
    }

    private boolean applyLogical(
            BinaryOperator op,
            KeyExpressionNode leftNode,
            KeyExpressionNode rightNode,
            KeyExpressionEvaluationContext ctx
    ) {
        boolean left = evaluateBoolean(leftNode, ctx);

        if (op == BinaryOperator.AND) {
            if (!left) {
                return false;
            }
            return evaluateBoolean(rightNode, ctx);
        }

        if (left) {
            return true;
        }
        return evaluateBoolean(rightNode, ctx);
    }

    private Object resolveValue(KeyExpressionNode node, KeyExpressionEvaluationContext ctx) {
        if (node instanceof NumberLiteralNode num) {
            Number value = num.value();
            return value == null ? 0.0 : value.doubleValue();
        }
        if (node instanceof StringLiteralNode str) {
            return str.value() == null ? "" : str.value();
        }
        if (node instanceof BooleanLiteralNode bool) {
            return bool.value();
        }
        if (node instanceof AttributeAccessNode access) {
            return resolveAttribute(access, ctx);
        }
        if (node instanceof FunctionCallNode functionCall) {
            return evaluateFunction(functionCall, ctx);
        }
        if (node instanceof IdentifierNode id) {
            throw evaluationError("Unresolved identifier '" + id.name() + "'", ctx);
        }
        if (node instanceof UnaryNode unary) {
            return applyUnary(unary, ctx);
        }
        if (node instanceof BinaryNode binary) {
            return applyBinary(binary, ctx);
        }

        throw evaluationError("Unsupported expression node", ctx);
    }

    private Object resolveAttribute(AttributeAccessNode access, KeyExpressionEvaluationContext ctx) {
        AttributeResolver resolver = ctx.attributeResolver();
        if (resolver == null) {
            if (ctx.attributePolicy() == AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO) {
                return 0.0;
            }
            throw unknownReference("Attribute access requires a resolver", ctx);
        }
        Object value = resolver.resolve(
                access,
                new AttributeResolutionContext(
                        ctx.hasResolver(),
                        ctx.searchResolver(),
                        ctx.skillResolver(),
                        ctx.attributeResolver(),
                        ctx.attributePolicy(),
                        ctx.input()
                )
        );
        if (value == null) {
            if (ctx.attributePolicy() == AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO) {
                return 0.0;
            }
            throw unknownReference("Unresolved attribute access: " + formatAccess(access), ctx);
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String || value instanceof Boolean) {
            return value;
        }
        throw evaluationError("Unsupported attribute value type", ctx);
    }

    private Object applyUnary(UnaryNode unary, KeyExpressionEvaluationContext ctx) {
        UnaryOperator op = unary.operator();
        if (op == UnaryOperator.NOT) {
            boolean operand = evaluateBoolean(unary.operand(), ctx);
            return !operand;
        }
        if (op == UnaryOperator.NEGATE) {
            Object value = resolveValue(unary.operand(), ctx);
            if (!(value instanceof Double number)) {
                throw evaluationError("Unary '-' expects a number", ctx);
            }
            return -number;
        }

        throw evaluationError("Unsupported unary operator", ctx);
    }

    private Object applyBinary(BinaryNode binary, KeyExpressionEvaluationContext ctx) {
        BinaryOperator op = binary.operator();
        if (op == BinaryOperator.AND || op == BinaryOperator.OR) {
            return applyLogical(op, binary.left(), binary.right(), ctx);
        }
        if (op == BinaryOperator.ADD
                || op == BinaryOperator.SUBTRACT
                || op == BinaryOperator.MULTIPLY
                || op == BinaryOperator.DIVIDE) {
            return applyArithmetic(op, binary.left(), binary.right(), ctx);
        }
        return applyComparison(op, binary.left(), binary.right(), ctx);
    }

    private Double applyArithmetic(
            BinaryOperator op,
            KeyExpressionNode leftNode,
            KeyExpressionNode rightNode,
            KeyExpressionEvaluationContext ctx
    ) {
        Object left = resolveValue(leftNode, ctx);
        Object right = resolveValue(rightNode, ctx);

        if (!(left instanceof Double l) || !(right instanceof Double r)) {
            throw evaluationError("Arithmetic operators require numeric operands", ctx);
        }

        if (op == BinaryOperator.DIVIDE && r == 0.0) {
            throw evaluationError("Division by zero", ctx);
        }

        return switch (op) {
            case ADD -> l + r;
            case SUBTRACT -> l - r;
            case MULTIPLY -> l * r;
            case DIVIDE -> l / r;
            default -> throw evaluationError("Unsupported arithmetic operator", ctx);
        };
    }

    private Boolean applyComparison(
            BinaryOperator op,
            KeyExpressionNode leftNode,
            KeyExpressionNode rightNode,
            KeyExpressionEvaluationContext ctx
    ) {
        Object left = resolveValue(leftNode, ctx);
        Object right = resolveValue(rightNode, ctx);

        if (left == null || right == null) {
            throw evaluationError("Missing operand for comparison", ctx);
        }
        if (!left.getClass().equals(right.getClass())) {
            throw evaluationError("Type mismatch in comparison", ctx);
        }

        if (left instanceof Boolean && (op == BinaryOperator.LESS_THAN
                || op == BinaryOperator.LESS_THAN_OR_EQUAL
                || op == BinaryOperator.GREATER_THAN
                || op == BinaryOperator.GREATER_THAN_OR_EQUAL)) {
            throw evaluationError("Cannot order boolean values", ctx);
        }

        int cmp = compare(left, right, ctx);
        return switch (op) {
            case EQUAL -> cmp == 0;
            case NOT_EQUAL -> cmp != 0;
            case LESS_THAN -> cmp < 0;
            case LESS_THAN_OR_EQUAL -> cmp <= 0;
            case GREATER_THAN -> cmp > 0;
            case GREATER_THAN_OR_EQUAL -> cmp >= 0;
            default -> throw evaluationError("Unsupported comparison operator", ctx);
        };
    }

    private int compare(Object left, Object right, KeyExpressionEvaluationContext ctx) {
        if (left instanceof Double l && right instanceof Double r) {
            return Double.compare(l, r);
        }
        if (left instanceof String l && right instanceof String r) {
            return l.compareTo(r);
        }
        if (left instanceof Boolean l && right instanceof Boolean r) {
            return Boolean.compare(l, r);
        }

        throw evaluationError("Unsupported comparison types", ctx);
    }

    private boolean toBoolean(Object value, KeyExpressionEvaluationContext ctx) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.doubleValue() != 0.0;
        }
        if (value instanceof String) {
            return true;
        }

        throw evaluationError("Unsupported truthiness conversion", ctx);
    }

    private KeyExpressionEvaluationException evaluationError(String message, KeyExpressionEvaluationContext ctx) {
        return new KeyExpressionEvaluationException(new KeyExpressionError(
                KeyExpressionError.Phase.EVALUATE,
                message,
                ctx.input(),
                -1
        ));
    }

    private UnknownReferenceException unknownReference(String message, KeyExpressionEvaluationContext ctx) {
        return new UnknownReferenceException(new KeyExpressionError(
                KeyExpressionError.Phase.EVALUATE,
                message,
                ctx.input(),
                -1
        ));
    }

    private String formatAccess(AttributeAccessNode access) {
        StringBuilder sb = new StringBuilder();
        sb.append(access.root());
        for (AccessSegment segment : access.segments()) {
            if (segment instanceof AccessSegment.PropertySegment prop) {
                sb.append(".").append(prop.name());
            } else if (segment instanceof AccessSegment.FixtureSegment fixture) {
                sb.append(".fixture(\"").append(fixture.name()).append("\")");
            }
        }
        return sb.toString();
    }

    private Object evaluateFunction(FunctionCallNode functionCall, KeyExpressionEvaluationContext ctx) {
        List<Object> argValues = new ArrayList<>();
        for (KeyExpressionNode argument : functionCall.arguments()) {
            argValues.add(resolveValue(argument, ctx));
        }

        if (debugOutput) {
            List<String> argText = argValues.stream()
                    .map(String::valueOf)
                    .toList();
            System.out.println("Function: " + functionCall.name());
            System.out.println("Parameters: " + String.join(", ", argText));
        }

        return switch (functionCall.name()) {
            case "DICE" -> rollDice(argValues, ctx);
            case "HAS" -> has(argValues, ctx.hasResolver(), ctx);
            case "SEARCH" -> search(argValues, ctx.searchResolver(), ctx);
            case "SKILL" -> skill(argValues, ctx.skillResolver(), ctx);
            default -> throw evaluationError("Unsupported function '" + functionCall.name() + "'", ctx);
        };
    }

    private Double rollDice(List<Object> argValues, KeyExpressionEvaluationContext ctx) {
        if (argValues.size() != 1) {
            throw evaluationError("DICE expects exactly one parameter", ctx);
        }

        int sides = parseDiceSides(argValues.get(0), ctx);
        int roll = diceRoller.roll(sides);

        if (debugOutput) {
            System.out.println("Result: " + roll);
        }
        return (double) roll;
    }

    private int parseDiceSides(Object argument, KeyExpressionEvaluationContext ctx) {
        int sides;

        if (argument instanceof Number number) {
            double value = number.doubleValue();
            if (value % 1 != 0) {
                throw evaluationError("Dice sides must be a whole number", ctx);
            }
            sides = (int) value;
        } else if (argument instanceof String text) {
            String normalized = text.trim();
            if (normalized.startsWith("d") || normalized.startsWith("D")) {
                normalized = normalized.substring(1);
            }
            try {
                sides = Integer.parseInt(normalized);
            } catch (NumberFormatException ex) {
                throw evaluationError("Dice sides must be numeric", ctx);
            }
        } else {
            throw evaluationError("Unsupported argument type for DICE", ctx);
        }

        if (!SUPPORTED_DICE_SIDES.contains(sides)) {
            throw evaluationError("Unsupported die size: d" + sides, ctx);
        }

        return sides;
    }

    private Boolean has(List<Object> argValues, HasResolver hasResolver, KeyExpressionEvaluationContext ctx) {
        if (argValues.size() != 1) {
            throw evaluationError("HAS expects exactly one parameter", ctx);
        }

        String label = Objects.toString(argValues.get(0), "");
        return hasResolver.has(label);
    }

    private Boolean search(List<Object> argValues, SearchResolver searchResolver, KeyExpressionEvaluationContext ctx) {
        if (argValues.size() != 1) {
            throw evaluationError("SEARCH expects exactly one parameter", ctx);
        }

        String label = Objects.toString(argValues.get(0), "");
        return searchResolver.search(label);
    }

    private Boolean skill(List<Object> argValues, SkillResolver skillResolver, KeyExpressionEvaluationContext ctx) {
        if (argValues.size() != 1) {
            throw evaluationError("SKILL expects exactly one parameter", ctx);
        }
        String label = Objects.toString(argValues.get(0), "");
        return skillResolver.hasSkill(label);
    }
}

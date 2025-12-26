package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellReferenceReceipt;
import com.demo.adventure.engine.mechanics.cells.CellReferenceStatus;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import com.demo.adventure.support.exceptions.KeyExpressionEvaluationException;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AttributeAccessNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryOperator;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BooleanLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.FunctionCallNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.IdentifierNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.NumberLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.StringLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryOperator;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Expression evaluator: compiles the input and walks the AST to produce a boolean result.
 * Compile and evaluation errors are returned as structured results (or thrown by boolean helpers).
 */
public final class KeyExpressionEvaluator {
    @FunctionalInterface
    public interface HasResolver {
        boolean has(String label);
    }

    @FunctionalInterface
    public interface SearchResolver {
        boolean search(String label);
    }

    @FunctionalInterface
    public interface SkillResolver {
        boolean hasSkill(String tag);
    }

    @FunctionalInterface
    public interface AttributeResolver {
        Object resolve(AttributeAccessNode access, AttributeResolutionContext context);
    }

    @FunctionalInterface
    public interface DiceRoller {
        int roll(int sides);
    }

    public enum AttributeResolutionPolicy {
        QUERY_STRICT,
        COMPUTE_FALLBACK_ZERO
    }

    public record AttributeResolutionContext(
            HasResolver hasResolver,
            SearchResolver searchResolver,
            SkillResolver skillResolver,
            AttributeResolver attributeResolver,
            AttributeResolutionPolicy attributePolicy,
            String input
    ) {
    }

    private static final HasResolver NO_OP_HAS = label -> false;
    private static final SearchResolver NO_OP_SEARCH = label -> false;
    private static final SkillResolver NO_OP_SKILL = tag -> false;
    private static final AttributeResolver NO_OP_ATTRIBUTE = (access, context) -> null;
    private static final DiceRoller DEFAULT_DICE = sides -> ThreadLocalRandom.current().nextInt(1, sides + 1);
    private static volatile HasResolver defaultHasResolver = NO_OP_HAS;
    private static volatile SearchResolver defaultSearchResolver = NO_OP_SEARCH;
    private static volatile SkillResolver defaultSkillResolver = NO_OP_SKILL;
    private static volatile DiceRoller defaultDiceRoller = DEFAULT_DICE;
    // Debug output stays enabled by default for tests; caller (e.g., GameCli) should disable for players.
    private static volatile boolean debugOutput = true;
    private KeyExpressionEvaluator() {
    }

    public static boolean evaluate(String input) {
        return evaluate(
                input,
                defaultHasResolver,
                defaultSearchResolver,
                defaultSkillResolver,
                null,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static boolean evaluate(String input, HasResolver hasResolver) {
        return evaluate(
                input,
                hasResolver,
                defaultSearchResolver,
                defaultSkillResolver,
                null,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static boolean evaluate(String input, HasResolver hasResolver, SearchResolver searchResolver) {
        return evaluate(
                input,
                hasResolver,
                searchResolver,
                defaultSkillResolver,
                null,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static boolean evaluate(String input, HasResolver hasResolver, SearchResolver searchResolver, SkillResolver skillResolver) {
        return evaluate(
                input,
                hasResolver,
                searchResolver,
                skillResolver,
                null,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static boolean evaluate(
            String input,
            HasResolver hasResolver,
            SearchResolver searchResolver,
            SkillResolver skillResolver,
            AttributeResolver attributeResolver
    ) {
        return evaluate(
                input,
                hasResolver,
                searchResolver,
                skillResolver,
                attributeResolver,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static boolean evaluate(
            String input,
            HasResolver hasResolver,
            SearchResolver searchResolver,
            SkillResolver skillResolver,
            AttributeResolver attributeResolver,
            AttributeResolutionPolicy attributePolicy
    ) {
        KeyExpressionResult result = evaluateResult(
                input,
                hasResolver,
                searchResolver,
                skillResolver,
                attributeResolver,
                attributePolicy
        );
        if (!result.isSuccess()) {
            throw new KeyExpressionEvaluationException(result.error());
        }
        return result.value();
    }

    public static KeyExpressionResult evaluateResult(String input) {
        return evaluateResult(
                input,
                defaultHasResolver,
                defaultSearchResolver,
                defaultSkillResolver,
                null,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static KeyExpressionResult evaluateResult(String input, HasResolver hasResolver) {
        return evaluateResult(
                input,
                hasResolver,
                defaultSearchResolver,
                defaultSkillResolver,
                null,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static KeyExpressionResult evaluateResult(String input, HasResolver hasResolver, SearchResolver searchResolver) {
        return evaluateResult(
                input,
                hasResolver,
                searchResolver,
                defaultSkillResolver,
                null,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static KeyExpressionResult evaluateResult(
            String input,
            HasResolver hasResolver,
            SearchResolver searchResolver,
            SkillResolver skillResolver
    ) {
        return evaluateResult(
                input,
                hasResolver,
                searchResolver,
                skillResolver,
                null,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static KeyExpressionResult evaluateResult(
            String input,
            HasResolver hasResolver,
            SearchResolver searchResolver,
            SkillResolver skillResolver,
            AttributeResolver attributeResolver
    ) {
        return evaluateResult(
                input,
                hasResolver,
                searchResolver,
                skillResolver,
                attributeResolver,
                AttributeResolutionPolicy.QUERY_STRICT
        );
    }

    public static KeyExpressionResult evaluateResult(
            String input,
            HasResolver hasResolver,
            SearchResolver searchResolver,
            SkillResolver skillResolver,
            AttributeResolver attributeResolver,
            AttributeResolutionPolicy attributePolicy
    ) {
        if (input == null) {
            return KeyExpressionResult.success(false);
        }
        AttributeResolutionPolicy safePolicy =
                attributePolicy == null ? AttributeResolutionPolicy.QUERY_STRICT : attributePolicy;
        String safeInput = input;
        try {
            KeyExpressionCompiler compiler = new KeyExpressionCompiler();
            KeyExpressionNode ast = compiler.compile(safeInput);
            HasResolver safeHas = hasResolver == null ? NO_OP_HAS : hasResolver;
            SearchResolver safeSearch = searchResolver == null ? NO_OP_SEARCH : searchResolver;
            SkillResolver safeSkill = skillResolver == null ? NO_OP_SKILL : skillResolver;
            AttributeResolver safeAttribute = attributeResolver == null ? NO_OP_ATTRIBUTE : attributeResolver;
            EvaluationContext ctx = new EvaluationContext(
                    safeInput,
                    safeHas,
                    safeSearch,
                    safeSkill,
                    safeAttribute,
                    safePolicy
            );
            boolean value = evaluateBoolean(ast, ctx);
            return KeyExpressionResult.success(value);
        } catch (KeyExpressionCompileException ex) {
            KeyExpressionError error = new KeyExpressionError(
                    KeyExpressionError.Phase.COMPILE,
                    ex.getMessage(),
                    ex.getInput(),
                    ex.getCurrentPos()
            );
            return KeyExpressionResult.error(error);
        } catch (UnknownReferenceException ex) {
            if (safePolicy == AttributeResolutionPolicy.QUERY_STRICT) {
                throw ex;
            }
            return KeyExpressionResult.error(ex.getError());
        } catch (KeyExpressionEvaluationException ex) {
            return KeyExpressionResult.error(ex.getError());
        }
    }

    public static void setDefaultHasResolver(HasResolver resolver) {
        defaultHasResolver = resolver == null ? NO_OP_HAS : resolver;
    }

    public static HasResolver getDefaultHasResolver() {
        return defaultHasResolver;
    }

    public static void setDefaultSearchResolver(SearchResolver resolver) {
        defaultSearchResolver = resolver == null ? NO_OP_SEARCH : resolver;
    }

    public static SearchResolver getDefaultSearchResolver() {
        return defaultSearchResolver;
    }

    public static void setDefaultSkillResolver(SkillResolver resolver) {
        defaultSkillResolver = resolver == null ? NO_OP_SKILL : resolver;
    }

    public static SkillResolver getDefaultSkillResolver() {
        return defaultSkillResolver;
    }

    public static void setDefaultDiceRoller(DiceRoller roller) {
        defaultDiceRoller = roller == null ? DEFAULT_DICE : roller;
    }

    public static DiceRoller getDefaultDiceRoller() {
        return defaultDiceRoller;
    }

    public static void setDebugOutput(boolean debug) {
        debugOutput = debug;
    }

    public static boolean isDebugOutput() {
        return debugOutput;
    }

    public static HasResolver registryHasResolver(KernelRegistry registry, UUID ownerId) {
        return label -> registryContains(registry, ownerId, label);
    }

    public static SearchResolver registrySearchResolver(KernelRegistry registry, UUID ownerId) {
        return label -> registryContains(registry, ownerId, label);
    }

    public static SkillResolver registrySkillResolver(KernelRegistry registry, UUID actorId) {
        return tag -> {
            if (registry == null || actorId == null) {
                return false;
            }
            var thing = registry.get(actorId);
            if (!(thing instanceof Actor actor)) {
                return false;
            }
            return actor.getSkills().stream()
                    .anyMatch(s -> s.equalsIgnoreCase(tag));
        };
    }

    public static AttributeResolver registryAttributeResolver(KernelRegistry registry, UUID... scopeOwnerIds) {
        List<UUID> scopes = new ArrayList<>();
        if (scopeOwnerIds != null) {
            for (UUID scope : scopeOwnerIds) {
                if (scope != null && !scopes.contains(scope)) {
                    scopes.add(scope);
                }
            }
        }
        return (access, context) -> resolveRegistryAttribute(registry, scopes, access, context);
    }

    private record EvaluationContext(
            String input,
            HasResolver hasResolver,
            SearchResolver searchResolver,
            SkillResolver skillResolver,
            AttributeResolver attributeResolver,
            AttributeResolutionPolicy attributePolicy
    ) {
    }

    private static Object resolveRegistryAttribute(
            KernelRegistry registry,
            List<UUID> scopes,
            AttributeAccessNode access,
            AttributeResolutionContext context
    ) {
        if (registry == null || access == null || scopes == null || scopes.isEmpty()) {
            return null;
        }
        Thing current = findThingByLabel(registry, scopes, access.root());
        if (current == null) {
            return null;
        }
        List<AccessSegment> segments = access.segments();
        for (int index = 0; index < segments.size(); index++) {
            AccessSegment segment = segments.get(index);
            boolean last = index == segments.size() - 1;
            if (segment instanceof AccessSegment.FixtureSegment fixture) {
                Thing fixtureThing = findFixtureChild(registry, current, fixture.name());
                if (fixtureThing == null) {
                    return null;
                }
                current = fixtureThing;
                continue;
            }
            if (segment instanceof AccessSegment.PropertySegment prop) {
                if (!last) {
                    if (index == segments.size() - 2 && segments.get(index + 1) instanceof AccessSegment.PropertySegment next) {
                        return resolveCellProperty(registry, current, prop.name(), next.name(), context);
                    }
                    return null;
                }
                return resolveProperty(current, prop.name(), context);
            }
        }
        return null;
    }

    private static Object resolveCellProperty(
            KernelRegistry registry,
            Thing thing,
            String cellName,
            String field,
            AttributeResolutionContext context
    ) {
        if (thing == null || cellName == null || field == null || context == null) {
            return null;
        }
        String key = Thing.normalizeCellKey(cellName);
        Cell cell = thing.getCell(key);
        String prop = field.trim().toLowerCase(Locale.ROOT);
        if (cell == null) {
            CellReferenceStatus status = context.attributePolicy() == AttributeResolutionPolicy.QUERY_STRICT
                    ? CellReferenceStatus.MISSING
                    : CellReferenceStatus.UNDEFINED;
            recordCellReference(registry, thing.getId(), key, prop, status);
            if (context.attributePolicy() == AttributeResolutionPolicy.QUERY_STRICT) {
                throw unknownReference("Unknown cell: " + key + "." + prop, context);
            }
            return fallbackCellValue(prop);
        }
        return switch (prop) {
            case "capacity" -> (double) cell.getCapacity();
            case "amount" -> (double) cell.getAmount();
            case "volume" -> cell.getVolume();
            case "name" -> key;
            default -> {
                CellReferenceStatus status = context.attributePolicy() == AttributeResolutionPolicy.QUERY_STRICT
                        ? CellReferenceStatus.MISSING
                        : CellReferenceStatus.UNDEFINED;
                recordCellReference(registry, thing.getId(), key, prop, status);
                if (context.attributePolicy() == AttributeResolutionPolicy.QUERY_STRICT) {
                    throw unknownReference("Unknown cell field: " + key + "." + prop, context);
                }
                yield fallbackCellValue(prop);
            }
        };
    }

    private static void recordCellReference(
            KernelRegistry registry,
            UUID thingId,
            String cellName,
            String field,
            CellReferenceStatus status
    ) {
        if (registry == null || thingId == null) {
            return;
        }
        registry.recordCellReference(new CellReferenceReceipt(thingId, cellName, field, status));
    }

    private static Object fallbackCellValue(String field) {
        if (field == null) {
            return 0.0;
        }
        return switch (field.trim().toLowerCase(Locale.ROOT)) {
            case "name" -> "";
            default -> 0.0;
        };
    }

    private static Thing findThingByLabel(KernelRegistry registry, List<UUID> scopes, String label) {
        if (registry == null || scopes == null || label == null) {
            return null;
        }
        for (UUID scope : scopes) {
            Thing match = findThingInScope(registry, scope, label);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static Thing findThingInScope(KernelRegistry registry, UUID ownerId, String label) {
        if (registry == null || ownerId == null || label == null) {
            return null;
        }
        Thing owner = registry.get(ownerId);
        if (owner != null && label.equalsIgnoreCase(owner.getLabel())) {
            return owner;
        }
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(ownerId);
        while (!stack.isEmpty()) {
            UUID currentOwner = stack.pop();
            if (!visited.add(currentOwner)) {
                continue;
            }
            for (Thing thing : registry.getEverything().values()) {
                if (thing == null) {
                    continue;
                }
                if (!currentOwner.equals(thing.getOwnerId())) {
                    continue;
                }
                if (label.equalsIgnoreCase(thing.getLabel())) {
                    return thing;
                }
                stack.push(thing.getId());
            }
        }
        return null;
    }

    private static Thing findFixtureChild(KernelRegistry registry, Thing owner, String label) {
        if (registry == null || owner == null || label == null) {
            return null;
        }
        for (Thing thing : registry.getEverything().values()) {
            if (thing == null || !(thing instanceof Item item)) {
                continue;
            }
            if (!owner.getId().equals(item.getOwnerId())) {
                continue;
            }
            if (!item.isFixture()) {
                continue;
            }
            if (label.equalsIgnoreCase(item.getLabel())) {
                return item;
            }
        }
        return null;
    }

    private static Object resolveProperty(Thing thing, String property, AttributeResolutionContext context) {
        if (thing == null || property == null) {
            return null;
        }
        String key = property.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "open" -> evaluateNested(thing.getKey(), context);
            case "visible" -> evaluateVisibility(thing, context);
            case "label" -> thing.getLabel();
            case "description" -> thing.getDescription();
            case "kind" -> thing.getKind() == null ? null : thing.getKind().name();
            case "id" -> thing.getId() == null ? null : thing.getId().toString();
            case "ownerid", "owner" -> thing.getOwnerId() == null ? null : thing.getOwnerId().toString();
            case "size" -> (double) thing.getSize();
            case "weight" -> (double) thing.getWeight();
            case "volume" -> (double) thing.getVolume();
            case "ttl" -> (double) thing.getTtl();
            case "fixture" -> (thing instanceof Item item) ? item.isFixture() : null;
            case "footprintwidth" -> (thing instanceof Item item) ? item.getFootprintWidth() : null;
            case "footprintheight" -> (thing instanceof Item item) ? item.getFootprintHeight() : null;
            case "capacitywidth" -> (thing instanceof Item item) ? item.getCapacityWidth() : null;
            case "capacityheight" -> (thing instanceof Item item) ? item.getCapacityHeight() : null;
            case "plotkind" -> (thing instanceof Plot plot && plot.getPlotKind() != null)
                    ? plot.getPlotKind().name()
                    : null;
            case "plotrole" -> (thing instanceof Plot plot) ? plot.getPlotRole() : null;
            case "region" -> (thing instanceof Plot plot) ? plot.getRegion() : null;
            case "locationx" -> (thing instanceof Plot plot) ? (double) plot.getLocationX() : null;
            case "locationy" -> (thing instanceof Plot plot) ? (double) plot.getLocationY() : null;
            case "direction" -> (thing instanceof Gate gate && gate.getDirection() != null)
                    ? gate.getDirection().name()
                    : null;
            case "plotaid" -> (thing instanceof Gate gate && gate.getPlotAId() != null)
                    ? gate.getPlotAId().toString()
                    : null;
            case "plotbid" -> (thing instanceof Gate gate && gate.getPlotBId() != null)
                    ? gate.getPlotBId().toString()
                    : null;
            default -> null;
        };
    }

    private static boolean evaluateNested(String expression, AttributeResolutionContext context) {
        KeyExpressionResult result = evaluateResult(
                expression,
                context.hasResolver(),
                context.searchResolver(),
                context.skillResolver(),
                context.attributeResolver(),
                context.attributePolicy()
        );
        if (!result.isSuccess()) {
            throw new KeyExpressionEvaluationException(result.error());
        }
        return result.value();
    }

    private static boolean evaluateVisibility(Thing thing, AttributeResolutionContext context) {
        boolean baseVisible = thing.isVisibleFlag();
        if (!baseVisible) {
            return false;
        }
        return evaluateNested(thing.getVisibilityKey(), context);
    }

    private static boolean evaluateBoolean(KeyExpressionNode node, EvaluationContext ctx) {
        if (node instanceof BinaryNode binary) {
            BinaryOperator op = binary.operator();
            if (op == BinaryOperator.AND || op == BinaryOperator.OR) {
                return applyLogical(op, binary.left(), binary.right(), ctx);
            }
        }
        Object value = resolveValue(node, ctx);
        return toBoolean(value, ctx);
    }

    private static boolean applyLogical(
            BinaryOperator op,
            KeyExpressionNode leftNode,
            KeyExpressionNode rightNode,
            EvaluationContext ctx
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

    private static Object resolveValue(KeyExpressionNode node, EvaluationContext ctx) {
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

    private static Object resolveAttribute(AttributeAccessNode access, EvaluationContext ctx) {
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

    private static Object applyUnary(UnaryNode unary, EvaluationContext ctx) {
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

    private static Object applyBinary(BinaryNode binary, EvaluationContext ctx) {
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

    private static Double applyArithmetic(
            BinaryOperator op,
            KeyExpressionNode leftNode,
            KeyExpressionNode rightNode,
            EvaluationContext ctx
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

    private static Boolean applyComparison(
            BinaryOperator op,
            KeyExpressionNode leftNode,
            KeyExpressionNode rightNode,
            EvaluationContext ctx
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

    private static int compare(Object left, Object right, EvaluationContext ctx) {
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

    private static boolean toBoolean(Object value, EvaluationContext ctx) {
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

    private static KeyExpressionEvaluationException evaluationError(String message, EvaluationContext ctx) {
        return new KeyExpressionEvaluationException(new KeyExpressionError(
                KeyExpressionError.Phase.EVALUATE,
                message,
                ctx.input(),
                -1
        ));
    }

    private static UnknownReferenceException unknownReference(String message, EvaluationContext ctx) {
        return new UnknownReferenceException(new KeyExpressionError(
                KeyExpressionError.Phase.EVALUATE,
                message,
                ctx.input(),
                -1
        ));
    }

    private static UnknownReferenceException unknownReference(String message, AttributeResolutionContext ctx) {
        String input = ctx == null ? "" : ctx.input();
        return new UnknownReferenceException(new KeyExpressionError(
                KeyExpressionError.Phase.EVALUATE,
                message,
                input,
                -1
        ));
    }

    private static String formatAccess(AttributeAccessNode access) {
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

    private static final Set<Integer> SUPPORTED_DICE_SIDES = Set.of(4, 6, 8, 10, 12, 20);

    private static Object evaluateFunction(FunctionCallNode functionCall, EvaluationContext ctx) {
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

    private static Double rollDice(List<Object> argValues, EvaluationContext ctx) {
        if (argValues.size() != 1) {
            throw evaluationError("DICE expects exactly one parameter", ctx);
        }

        int sides = parseDiceSides(argValues.get(0), ctx);
        int roll = defaultDiceRoller.roll(sides);

        if (debugOutput) {
            System.out.println("Result: " + roll);
        }
        return (double) roll;
    }

    private static int parseDiceSides(Object argument, EvaluationContext ctx) {
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

    private static Boolean has(List<Object> argValues, HasResolver hasResolver, EvaluationContext ctx) {
        if (argValues.size() != 1) {
            throw evaluationError("HAS expects exactly one parameter", ctx);
        }

        String label = Objects.toString(argValues.get(0), "");
        return hasResolver.has(label);
    }

    private static Boolean search(List<Object> argValues, SearchResolver searchResolver, EvaluationContext ctx) {
        if (argValues.size() != 1) {
            throw evaluationError("SEARCH expects exactly one parameter", ctx);
        }

        String label = Objects.toString(argValues.get(0), "");
        return searchResolver.search(label);
    }

    private static Boolean skill(List<Object> argValues, SkillResolver skillResolver, EvaluationContext ctx) {
        if (argValues.size() != 1) {
            throw evaluationError("SKILL expects exactly one parameter", ctx);
        }
        String label = Objects.toString(argValues.get(0), "");
        return skillResolver.hasSkill(label);
    }

    private static boolean registryContains(KernelRegistry registry, UUID ownerId, String label) {
        if (registry == null || ownerId == null || label == null) {
            return false;
        }

        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(ownerId);

        while (!stack.isEmpty()) {
            UUID currentOwner = stack.pop();
            if (!visited.add(currentOwner)) {
                continue;
            }

            for (var thing : registry.getEverything().values()) {
                if (thing == null) {
                    continue;
                }
                if (!currentOwner.equals(thing.getOwnerId())) {
                    continue;
                }
                if (label.equalsIgnoreCase(thing.getLabel())) {
                    return true;
                }
                stack.push(thing.getId());
            }
        }

        return false;
    }
}

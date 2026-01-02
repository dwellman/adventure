package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import com.demo.adventure.support.exceptions.KeyExpressionEvaluationException;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AttributeAccessNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;

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
            KeyExpressionEvaluationContext ctx = new KeyExpressionEvaluationContext(
                    safeInput,
                    safeHas,
                    safeSearch,
                    safeSkill,
                    safeAttribute,
                    safePolicy
            );
            KeyExpressionAstEvaluator evaluator = new KeyExpressionAstEvaluator(defaultDiceRoller, debugOutput);
            boolean value = evaluator.evaluateBoolean(ast, ctx);
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
        return KeyExpressionRegistryResolvers.registryHasResolver(registry, ownerId);
    }

    public static SearchResolver registrySearchResolver(KernelRegistry registry, UUID ownerId) {
        return KeyExpressionRegistryResolvers.registrySearchResolver(registry, ownerId);
    }

    public static SkillResolver registrySkillResolver(KernelRegistry registry, UUID actorId) {
        return KeyExpressionRegistryResolvers.registrySkillResolver(registry, actorId);
    }

    public static AttributeResolver registryAttributeResolver(KernelRegistry registry, UUID... scopeOwnerIds) {
        return KeyExpressionRegistryResolvers.registryAttributeResolver(registry, scopeOwnerIds);
    }
}

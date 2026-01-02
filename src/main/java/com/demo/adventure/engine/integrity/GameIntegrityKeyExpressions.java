package com.demo.adventure.engine.integrity;

import com.demo.adventure.engine.flow.trigger.TriggerDefinition;
import com.demo.adventure.engine.flow.trigger.TriggerActionType;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionCompiler;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;
import com.demo.adventure.domain.kernel.KernelRegistry;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class GameIntegrityKeyExpressions {
    private GameIntegrityKeyExpressions() {
    }

    static void validateKeyExpressions(
            GameContext game,
            KernelRegistry registry,
            LabelIndex labels,
            List<GameIntegrityIssue> issues
    ) {
        if (game == null || registry == null || labels == null || issues == null) {
            return;
        }
        List<IntegrityKeyExpressionSpec> expressions = IntegrityKeyExpressionSpecs.collect(game);

        UUID[] scopeIds = registry.getEverything().keySet().toArray(new UUID[0]);
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, scopeIds);

        for (IntegrityKeyExpressionSpec spec : expressions) {
            IntegrityKeyExpressionValidator.validateExpression(spec, labels, attributeResolver, issues);
        }
    }

    static List<Set<String>> collectWinRequirements(
            List<TriggerDefinition> triggers,
            LabelIndex labels,
            List<GameIntegrityIssue> issues
    ) {
        List<Set<String>> requirements = new ArrayList<>();
        if (triggers == null) {
            return requirements;
        }
        for (TriggerDefinition trigger : triggers) {
            if (trigger == null) {
                continue;
            }
            boolean hasWin = trigger.actions().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(action -> action.type() == TriggerActionType.END_GAME);
            if (!hasWin) {
                continue;
            }
            Set<String> required = new HashSet<>();
            if (trigger.key() != null && !trigger.key().isBlank()) {
                try {
                    KeyExpressionNode ast = new KeyExpressionCompiler().compile(trigger.key());
                    IntegrityKeyExpressionAst.collectHasReferences(ast, required);
                } catch (Exception ex) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.ERROR,
                            "E_WIN_KEYEXPR_PARSE",
                            "Win key expression parse failed: " + ex.getMessage(),
                            trigger.id()
                    ));
                }
            }
            requirements.add(required);
        }
        return requirements;
    }

}

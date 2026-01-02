package com.demo.adventure.engine.integrity;

import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionCompiler;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionResult;
import com.demo.adventure.engine.mechanics.keyexpr.UnknownReferenceException;
import com.demo.adventure.engine.mechanics.keyexpr.ast.FunctionCallNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;

import java.util.List;
import java.util.Locale;

final class IntegrityKeyExpressionValidator {
    private IntegrityKeyExpressionValidator() {
    }

    static void validateExpression(
            IntegrityKeyExpressionSpec spec,
            LabelIndex labels,
            KeyExpressionEvaluator.AttributeResolver attributeResolver,
            List<GameIntegrityIssue> issues
    ) {
        if (spec == null || spec.expression() == null || spec.expression().isBlank()) {
            return;
        }
        if (labels == null || issues == null) {
            return;
        }
        KeyExpressionNode ast;
        try {
            ast = new KeyExpressionCompiler().compile(spec.expression());
        } catch (Exception ex) {
            issues.add(new GameIntegrityIssue(
                    GameIntegritySeverity.ERROR,
                    "E_KEYEXPR_PARSE",
                    "Key expression parse failed: " + ex.getMessage(),
                    spec.context()
            ));
            return;
        }
        validateFunctionReferences(ast, labels, issues, spec.context());
        try {
            KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(
                    spec.expression(),
                    label -> false,
                    label -> false,
                    tag -> false,
                    attributeResolver,
                    KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT
            );
            if (!result.isSuccess()) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_KEYEXPR_INVALID",
                        "Key expression invalid: " + result.error().message(),
                        spec.context()
                ));
            }
        } catch (UnknownReferenceException ex) {
            issues.add(new GameIntegrityIssue(
                    GameIntegritySeverity.ERROR,
                    "E_KEYEXPR_REF",
                    "Key expression reference error: " + ex.getError().message(),
                    spec.context()
            ));
        }
    }

    private static void validateFunctionReferences(
            KeyExpressionNode node,
            LabelIndex labels,
            List<GameIntegrityIssue> issues,
            String context
    ) {
        if (node == null || labels == null || issues == null) {
            return;
        }
        for (FunctionCallNode call : IntegrityKeyExpressionAst.collectFunctions(node)) {
            if (call == null || call.name() == null) {
                continue;
            }
            String name = call.name().trim().toUpperCase(Locale.ROOT);
            if ("HAS".equals(name) || "SEARCH".equals(name)) {
                String label = IntegrityKeyExpressionAst.firstStringLiteral(call);
                if (label == null) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.WARNING,
                            "W_KEYEXPR_DYNAMIC_LABEL",
                            "Key expression uses non-literal " + name + " label.",
                            context
                    ));
                    continue;
                }
                String normalized = IntegrityLabels.normalizeLabel(label);
                if (!normalized.isBlank() && !labels.labels().contains(normalized)) {
                    GameIntegritySeverity severity = context != null && context.startsWith("craft:")
                            ? GameIntegritySeverity.WARNING
                            : GameIntegritySeverity.ERROR;
                    issues.add(new GameIntegrityIssue(
                            severity,
                            "E_KEYEXPR_LABEL_MISSING",
                            "Key expression references missing " + name + " label: " + label + ".",
                            context
                    ));
                }
            }
            if ("SKILL".equals(name)) {
                String label = IntegrityKeyExpressionAst.firstStringLiteral(call);
                if (label == null) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.WARNING,
                            "W_KEYEXPR_DYNAMIC_SKILL",
                            "Key expression uses non-literal SKILL tag.",
                            context
                    ));
                    continue;
                }
                String normalized = IntegrityLabels.normalizeLabel(label);
                if (!normalized.isBlank() && !labels.skills().contains(normalized)) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.ERROR,
                            "E_KEYEXPR_SKILL_MISSING",
                            "Key expression references missing SKILL tag: " + label + ".",
                            context
                    ));
                }
            }
        }
    }
}

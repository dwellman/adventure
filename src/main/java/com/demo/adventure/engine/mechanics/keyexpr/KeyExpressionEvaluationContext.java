package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.AttributeResolutionPolicy;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.AttributeResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.HasResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SearchResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SkillResolver;

record KeyExpressionEvaluationContext(
        String input,
        HasResolver hasResolver,
        SearchResolver searchResolver,
        SkillResolver skillResolver,
        AttributeResolver attributeResolver,
        AttributeResolutionPolicy attributePolicy
) {
}

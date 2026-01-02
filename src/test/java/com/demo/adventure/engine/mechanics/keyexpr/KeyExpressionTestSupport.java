package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.model.Thing;

import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

final class KeyExpressionTestSupport {
    private KeyExpressionTestSupport() {
    }

    static void assertError(String input) {
        try {
            KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(input);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.error()).isNotNull();
        } catch (UnknownReferenceException ex) {
            assertThat(ex.getError()).isNotNull();
        }
    }

    static void assertError(String input, KeyExpressionEvaluator.AttributeResolver resolver) {
        assertError(input, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT);
    }

    static void assertError(
            String input,
            KeyExpressionEvaluator.AttributeResolver resolver,
            KeyExpressionEvaluator.AttributeResolutionPolicy policy
    ) {
        try {
            KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(input, null, null, null, resolver, policy);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.error()).isNotNull();
        } catch (UnknownReferenceException ex) {
            assertThat(ex.getError()).isNotNull();
        }
    }

    static void assertFalseResult(
            String input,
            KeyExpressionEvaluator.AttributeResolver resolver,
            KeyExpressionEvaluator.AttributeResolutionPolicy policy
    ) {
        KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(input, null, null, null, resolver, policy);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isFalse();
    }

    static boolean hasItemWithLabel(KernelRegistry registry, UUID ownerId, String label) {
        if (registry == null || ownerId == null || label == null) {
            return false;
        }
        return registry.getEverything().values().stream()
                .filter(Objects::nonNull)
                .filter(t -> ownerId.equals(t.getOwnerId()))
                .anyMatch(t -> label.equalsIgnoreCase(t.getLabel()));
    }

    static Plot plot(UUID id, String label, String description) {
        return new PlotBuilder()
                .withId(id)
                .withLabel(label)
                .withDescription(description)
                .build();
    }

    static Item item(String label, String description, Thing owner) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(description)
                .withOwnerId(owner)
                .build();
    }
}

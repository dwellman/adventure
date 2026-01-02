package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.assertError;
import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.assertFalseResult;
import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.item;
import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.plot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyExpressionAttributeResolverTest {

    @Test
    void resolvesCellAttributesFromRegistry() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = plot(UUID.randomUUID(), "Plot", "Plot");
        Item lantern = item("Lantern", "Old lantern", plot);
        lantern.setCell("kerosene", new Cell(10, 4));
        registry.register(plot);
        registry.register(lantern);

        var resolver = KeyExpressionEvaluator.registryAttributeResolver(registry, plot.getId());

        assertTrue(KeyExpressionEvaluator.evaluate("Lantern.kerosene.amount == 4", null, null, null, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT));
        assertTrue(KeyExpressionEvaluator.evaluate("Lantern.kerosene.capacity == 10", null, null, null, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT));
        assertTrue(KeyExpressionEvaluator.evaluate("Lantern.kerosene.volume > 0.39", null, null, null, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT));
        assertTrue(KeyExpressionEvaluator.evaluate("Lantern.kerosene.name == \"KEROSENE\"", null, null, null, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT));
    }

    @Test
    void missingCellFallsBackToZeroInComputeMode() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = plot(UUID.randomUUID(), "Plot", "Plot");
        Item lantern = item("Lantern", "Old lantern", plot);
        registry.register(plot);
        registry.register(lantern);

        var resolver = KeyExpressionEvaluator.registryAttributeResolver(registry, plot.getId());
        boolean result = KeyExpressionEvaluator.evaluate(
                "Lantern.kerosene.amount > 0",
                null,
                null,
                null,
                resolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );

        assertFalse(result);
        assertThat(registry.getCellReferenceReceipts())
                .anyMatch(r -> r.thingId().equals(lantern.getId())
                        && r.cellName().equals("KEROSENE")
                        && r.status() == com.demo.adventure.engine.mechanics.cells.CellReferenceStatus.UNDEFINED);
    }

    @Test
    void missingCellThrowsUnknownReferenceInQueryMode() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = plot(UUID.randomUUID(), "Plot", "Plot");
        Item lantern = item("Lantern", "Old lantern", plot);
        registry.register(plot);
        registry.register(lantern);

        var resolver = KeyExpressionEvaluator.registryAttributeResolver(registry, plot.getId());

        assertThatThrownBy(() -> KeyExpressionEvaluator.evaluate(
                "Lantern.kerosene.amount > 0",
                null,
                null,
                null,
                resolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT
        )).isInstanceOf(UnknownReferenceException.class);
    }

    @Test
    void resolvesAttributeAccessWithRegistryResolver() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = plot(UUID.randomUUID(), "Room", "A room");
        Item chest = item("Chest", "A chest", plot);
        Item lock = item("Lock", "A lock", chest);
        lock.setFixture(true);
        lock.setKey("true");
        registry.register(plot);
        registry.register(chest);
        registry.register(lock);

        KeyExpressionEvaluator.AttributeResolver resolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, plot.getId());
        KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(
                "Chest.fixture(\"Lock\").open",
                null,
                null,
                null,
                resolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT
        );
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isTrue();
    }

    @Test
    void resolvesAttributeAccessWithResolver() {
        KeyExpressionEvaluator.AttributeResolver resolver = (access, context) -> {
            if (!"thing".equalsIgnoreCase(access.root())) {
                return null;
            }
            String fixtureName = null;
            for (AccessSegment segment : access.segments()) {
                if (segment instanceof AccessSegment.FixtureSegment fixture) {
                    fixtureName = fixture.name();
                } else if (segment instanceof AccessSegment.PropertySegment prop) {
                    if ("x".equalsIgnoreCase(fixtureName) && "open".equalsIgnoreCase(prop.name())) {
                        return true;
                    }
                    return null;
                }
            }
            return null;
        };

        KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(
                "thing.fixture(\"x\").open",
                null,
                null,
                null,
                resolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT
        );
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isTrue();
    }

    @Test
    void attributeAccessWithoutResolverReportsError() {
        assertError("thing.fixture(\"x\").open");
    }

    @Test
    void attributeAccessMissingPathReportsError() {
        KeyExpressionEvaluator.AttributeResolver resolver = (access, context) -> null;
        assertError("thing.fixture(\"x\").open", resolver);
    }

    @Test
    void attributeAccessMissingPathFallsBackToFalse() {
        KeyExpressionEvaluator.AttributeResolver resolver = (access, context) -> null;
        assertFalseResult(
                "thing.fixture(\"x\").open",
                resolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
    }

    @Test
    void attributeAccessWithoutResolverFallsBackToFalse() {
        assertFalseResult(
                "thing.fixture(\"x\").open",
                null,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
    }
}

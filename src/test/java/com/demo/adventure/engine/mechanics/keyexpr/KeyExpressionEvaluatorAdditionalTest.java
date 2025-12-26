package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.GateBuilder;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.support.exceptions.KeyExpressionEvaluationException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyExpressionEvaluatorAdditionalTest {

    @Test
    void skillFunctionUsesResolver() {
        KeyExpressionEvaluator.SkillResolver resolver = tag -> "LOCKPICKING".equalsIgnoreCase(tag);

        assertThat(KeyExpressionEvaluator.evaluate("SKILL(\"Lockpicking\")", null, null, resolver)).isTrue();
        assertThat(KeyExpressionEvaluator.evaluate("SKILL(\"Swimming\")", null, null, resolver)).isFalse();
    }

    @Test
    void evaluatesArithmeticAndComparisons() {
        assertThat(KeyExpressionEvaluator.evaluate("1 + 2 * 3 == 7")).isTrue();
        assertThat(KeyExpressionEvaluator.evaluate("10 / 2 == 5")).isTrue();
        assertThat(KeyExpressionEvaluator.evaluate("5 - 2 > 1")).isTrue();
    }

    @Test
    void evaluatesStringComparisons() {
        assertThat(KeyExpressionEvaluator.evaluate("\"A\" == \"A\"")).isTrue();
        assertThat(KeyExpressionEvaluator.evaluate("\"A\" != \"B\"")).isTrue();
    }

    @Test
    void divisionByZeroThrows() {
        assertThatThrownBy(() -> KeyExpressionEvaluator.evaluate("1 / 0 > 0"))
                .isInstanceOf(KeyExpressionEvaluationException.class);
    }

    @Test
    void resolvesThingAttributesFromRegistry() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withId(UUID.randomUUID())
                .withLabel("StartPlot")
                .withDescription("A room")
                .withRegion("TEST")
                .withPlotRole("START")
                .build();
        Plot other = new PlotBuilder()
                .withId(UUID.randomUUID())
                .withLabel("OtherPlot")
                .withDescription("Another room")
                .withPlotRole("OTHER")
                .build();
        Gate gate = new GateBuilder()
                .withLabel("Gate")
                .withDescription("A gate")
                .withPlotA(plot)
                .withPlotB(other)
                .withDirection(com.demo.adventure.domain.model.Direction.E)
                .build();
        Item item = new ItemBuilder()
                .withLabel("Box")
                .withDescription("A box")
                .withOwnerId(plot)
                .build();
        item.setFixture(true);
        registry.register(plot);
        registry.register(other);
        registry.register(gate);
        registry.register(item);

        var resolver = KeyExpressionEvaluator.registryAttributeResolver(registry, plot.getId());

        assertThat(KeyExpressionEvaluator.evaluate("StartPlot.region == \"TEST\"", null, null, null, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT)).isTrue();
        assertThat(KeyExpressionEvaluator.evaluate("Gate.direction == \"E\"", null, null, null, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT)).isTrue();
        assertThat(KeyExpressionEvaluator.evaluate("Box.fixture == true", null, null, null, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT)).isTrue();
    }
}

package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.HasResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SearchResolver;
import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.UUID;

import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.assertError;
import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.hasItemWithLabel;
import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.item;
import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.plot;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyExpressionEvaluatorFunctionTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void printsFunctionNameAndParameters() {
        String input = "DICE(6)";

        console.reset();
        boolean priorDebug = KeyExpressionEvaluator.isDebugOutput();
        KeyExpressionEvaluator.setDebugOutput(true);
        try {
            assertTrue(KeyExpressionEvaluator.evaluate(input));
        } finally {
            KeyExpressionEvaluator.setDebugOutput(priorDebug);
        }
        String printed = console.output();
        assertThat(printed).contains("Function: DICE");
        assertThat(printed).contains("Parameters: 6.0");
        assertThat(printed).contains("Result: ");
    }

    @Test
    void rejectsUnsupportedDiceSize() {
        assertError("DICE(3)");
    }

    @Test
    void diceRollEvaluatesGreaterThanZero() {
        assertTrue(KeyExpressionEvaluator.evaluate("Dice(4) > 0"));
    }

    @Test
    void hasMatchesPlotContentsByLabel() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = plot(UUID.randomUUID(), "Test Plot", "A place");
        Item desk = item("Desk", "A sturdy desk", plot);
        desk.setFixture(true);
        Item itemX = item("X", "Item X", plot);
        registry.register(plot);
        registry.register(desk);
        registry.register(itemX);

        HasResolver hasResolver = label -> hasItemWithLabel(registry, plot.getId(), label);

        assertTrue(KeyExpressionEvaluator.evaluate("HAS(\"X\")", hasResolver));
        assertFalse(KeyExpressionEvaluator.evaluate("HAS(\"Y\")", hasResolver));
    }

    @Test
    void hasDefaultsToFalseWithoutResolver() {
        assertFalse(KeyExpressionEvaluator.evaluate("HAS(\"anything\")"));
    }

    @Test
    void searchUsesResolver() {
        SearchResolver searchResolver = label -> "HIDDEN".equalsIgnoreCase(label);
        assertTrue(KeyExpressionEvaluator.evaluate("SEARCH(\"hidden\")", null, searchResolver));
        assertFalse(KeyExpressionEvaluator.evaluate("SEARCH(\"other\")", null, searchResolver));
    }

    @Test
    void searchDefaultsToFalseWithoutResolver() {
        assertFalse(KeyExpressionEvaluator.evaluate("SEARCH(\"anything\")"));
    }

    @Test
    void registryResolversTraverseOwnership() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = plot(UUID.randomUUID(), "Plot", "Plot");
        Item desk = item("Desk", "Desk", plot);
        Item drawer = item("Drawer", "Drawer", desk);
        Item hidden = item("Hidden Note", "Hidden", drawer);
        registry.register(plot);
        registry.register(desk);
        registry.register(drawer);
        registry.register(hidden);

        SearchResolver searchResolver = KeyExpressionEvaluator.registrySearchResolver(registry, plot.getId());
        HasResolver hasResolver = KeyExpressionEvaluator.registryHasResolver(registry, plot.getId());

        assertTrue(KeyExpressionEvaluator.evaluate("SEARCH(\"Hidden Note\")", hasResolver, searchResolver));
        assertTrue(KeyExpressionEvaluator.evaluate("HAS(\"Drawer\")", hasResolver, searchResolver));
        assertFalse(KeyExpressionEvaluator.evaluate("HAS(\"Missing\")", hasResolver, searchResolver));
    }
}

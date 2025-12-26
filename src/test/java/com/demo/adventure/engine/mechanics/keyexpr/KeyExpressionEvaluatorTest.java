package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.HasResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SearchResolver;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class KeyExpressionEvaluatorTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    private void assertError(String input) {
        try {
            KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(input);
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.error()).isNotNull();
        } catch (UnknownReferenceException ex) {
            assertThat(ex.getError()).isNotNull();
        }
    }

    private void assertError(String input, KeyExpressionEvaluator.AttributeResolver resolver) {
        assertError(input, resolver, KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT);
    }

    private void assertError(
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

    private void assertFalseResult(
            String input,
            KeyExpressionEvaluator.AttributeResolver resolver,
            KeyExpressionEvaluator.AttributeResolutionPolicy policy
    ) {
        KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(input, null, null, null, resolver, policy);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.value()).isFalse();
    }

    @Test
    void scansSingleKeyword() throws KeyExpressionCompileException {
        String input = "DICE";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansFunctionLikeExpression() throws KeyExpressionCompileException {
        String input = "DICE(6)";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.FUNCTION,
                TokenType.LEFT_PAREN,
                TokenType.NUMBER,
                TokenType.RIGHT_PAREN,
                TokenType.EOL
        );
        assertThat(tokens.get(2).literal).isEqualTo(6L);

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void printsFunctionNameAndParameters() throws KeyExpressionCompileException {
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
    void scansStringLiteral() throws KeyExpressionCompileException {
        String input = "\"hello\"";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.INTERPOLATED_STRING_START,
                TokenType.LITERAL_STRING,
                TokenType.INTERPOLATED_STRING_END,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo("hello");

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansIdentifierAsWord() throws KeyExpressionCompileException {
        String input = "flag1";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).hasSize(2);
        Token t = tokens.get(0);
        assertThat(t.type).isEqualTo(TokenType.IDENTIFIER);
        assertThat(t.lexeme).isEqualTo("FLAG1");
        assertThat(t.literal).isEqualTo("flag1");

        assertError(input);
    }

    @Test
    void scansEmptyInput() throws KeyExpressionCompileException {
        String input = "";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansBooleanTrue() throws KeyExpressionCompileException {
        String input = "true";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.TRUE,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo("true");

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansBooleanFalse() throws KeyExpressionCompileException {
        String input = "false";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.FALSE,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo("false");

        assertFalse(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansBangTrue() throws KeyExpressionCompileException {
        String input = "!true";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.NOT,
                TokenType.TRUE,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo("true");

        assertFalse(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansDoubleBangFalse() throws KeyExpressionCompileException {
        String input = "!!false";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.NOT,
                TokenType.NOT,
                TokenType.FALSE,
                TokenType.EOL
        );
        assertThat(tokens.get(2).literal).isEqualTo("false");

        assertFalse(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansParenthesizedTrue() throws KeyExpressionCompileException {
        String input = "(true)";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.LEFT_PAREN,
                TokenType.TRUE,
                TokenType.RIGHT_PAREN,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo("true");

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansDoubleParenthesizedTrue() throws KeyExpressionCompileException {
        String input = "((true))";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.LEFT_PAREN,
                TokenType.LEFT_PAREN,
                TokenType.TRUE,
                TokenType.RIGHT_PAREN,
                TokenType.RIGHT_PAREN,
                TokenType.EOL
        );
        assertThat(tokens.get(2).literal).isEqualTo("true");

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansTrueAndFalse() throws KeyExpressionCompileException {
        String input = "true && false";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.TRUE,
                TokenType.AND,
                TokenType.FALSE,
                TokenType.EOL
        );

        assertFalse(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansTrueOrFalseTight() throws KeyExpressionCompileException {
        String input = "true||false";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.TRUE,
                TokenType.OR,
                TokenType.FALSE,
                TokenType.EOL
        );

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansOrAndMix() throws KeyExpressionCompileException {
        String input = "true || false && false";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.TRUE,
                TokenType.OR,
                TokenType.FALSE,
                TokenType.AND,
                TokenType.FALSE,
                TokenType.EOL
        );

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansParenthesizedOrAndMix() throws KeyExpressionCompileException {
        String input = "(true || false) && false";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.LEFT_PAREN,
                TokenType.TRUE,
                TokenType.OR,
                TokenType.FALSE,
                TokenType.RIGHT_PAREN,
                TokenType.AND,
                TokenType.FALSE,
                TokenType.EOL
        );

        assertFalse(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansSingleLetterIdentifier() throws KeyExpressionCompileException {
        String input = "a";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo("a");

        assertError(input);
    }

    @Test
    void scansCamelCaseIdentifier() throws KeyExpressionCompileException {
        String input = "worldClock";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo("worldClock");

        assertError(input);
    }

    @Test
    void scansGreaterThanWithSpaces() throws KeyExpressionCompileException {
        String input = "n > 15";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.GREATER_THAN,
                TokenType.NUMBER,
                TokenType.EOL
        );
        assertThat(tokens.get(2).literal).isEqualTo(15L);

        assertError(input);
    }

    @Test
    void scansGreaterThanOrEqualTight() throws KeyExpressionCompileException {
        String input = "n>=15";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.GREATER_THAN_OR_EQUAL,
                TokenType.NUMBER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansDoubleEqualsSpaced() throws KeyExpressionCompileException {
        String input = "n == 15";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.EQUAL,
                TokenType.NUMBER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansNotEqualsTight() throws KeyExpressionCompileException {
        String input = "n!=15";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.NOT_EQUAL,
                TokenType.NUMBER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansLessThanWithSpaces() throws KeyExpressionCompileException {
        String input = "n < 15";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.LESS_THAN,
                TokenType.NUMBER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansLessThanOrEqualTight() throws KeyExpressionCompileException {
        String input = "n<=15";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.LESS_THAN_OR_EQUAL,
                TokenType.NUMBER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansGreaterThanNegativeLiteral() throws KeyExpressionCompileException {
        String input = "n > -1";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.GREATER_THAN,
                TokenType.MINUS,
                TokenType.NUMBER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansNegativeLiteral() throws KeyExpressionCompileException {
        String input = "-1";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.MINUS,
                TokenType.NUMBER,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo(1L);

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansDecimalLiteral() throws KeyExpressionCompileException {
        String input = "3.14";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.NUMBER,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo(3.14d);

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansNegativeDecimalLiteral() throws KeyExpressionCompileException {
        String input = "-0.5";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.MINUS,
                TokenType.NUMBER,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo(0.5d);

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansComparisonWithDecimalLiteral() throws KeyExpressionCompileException {
        String input = "n >= 1.25";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.GREATER_THAN_OR_EQUAL,
                TokenType.NUMBER,
                TokenType.EOL
        );
        assertThat(tokens.get(2).literal).isEqualTo(1.25d);

        assertError(input);
    }

    @Test
    void scansIntegerLiteral123() throws KeyExpressionCompileException {
        String input = "123";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.NUMBER,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo(123L);

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansIntegerLiteral12345() throws KeyExpressionCompileException {
        String input = "12345";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.NUMBER,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo(12345L);

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansQuotedStringLitTorch() throws KeyExpressionCompileException {
        String input = "\"lit torch\"";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.INTERPOLATED_STRING_START,
                TokenType.LITERAL_STRING,
                TokenType.INTERPOLATED_STRING_END,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo("lit torch");

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansQuotedStringWithSpaces() throws KeyExpressionCompileException {
        String input = "\"a b c\"";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.INTERPOLATED_STRING_START,
                TokenType.LITERAL_STRING,
                TokenType.INTERPOLATED_STRING_END,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo("a b c");

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansQuotedStringWithEscapedQuote() throws KeyExpressionCompileException {
        String input = "\"quote: \\\"\"";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.INTERPOLATED_STRING_START,
                TokenType.LITERAL_STRING,
                TokenType.INTERPOLATED_STRING_END,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo("quote: \"");

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansTrueWithCommentSeparated() throws KeyExpressionCompileException {
        String input = "true // comment";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.TRUE,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo("true");

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansTrueWithCommentNoSpace() throws KeyExpressionCompileException {
        String input = "true//comment";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.TRUE,
                TokenType.EOL
        );

        assertTrue(KeyExpressionEvaluator.evaluate(input));
    }

    @Test
    void scansExpressionWithTrailingComment() throws KeyExpressionCompileException {
        String input = "a && b // trailing";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.AND,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void scansIdentifierWithCommentLikeText() throws KeyExpressionCompileException {
        String input = "a//b";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo("a");

        assertError(input);
    }

    @Test
    void divisionBetweenWordsReportsError() {
        assertError("A/B");
    }

    @Test
    void divisionBetweenBooleansReportsError() {
        assertError("true/false");
    }

    @Test
    void divisionWithSpacesReportsError() {
        assertError("a / b");
    }

    @Test
    void loneSlashReportsError() {
        assertError("/");
    }

    @Test
    void throwsOnDollarToken() {
        String input = "true $ false";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);

        assertError(input);
    }

    @Test
    void throwsOnAtSign() {
        String input = "@";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);

        assertError(input);
    }

    @Test
    void throwsOnHashSign() {
        String input = "#";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);

        assertError(input);
    }

    @Test
    void throwsOnUnterminatedString() throws KeyExpressionCompileException {
        String input = "\"unterminated";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.INTERPOLATED_STRING_START,
                TokenType.LITERAL_STRING,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo("unterminated");

        assertError(input);
    }

    @Test
    void throwsOnUnterminatedStringBeforeNewline() throws KeyExpressionCompileException {
        String input = "\"\n";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.INTERPOLATED_STRING_START,
                TokenType.LITERAL_STRING,
                TokenType.EOL
        );
        assertThat(tokens.get(1).literal).isEqualTo(" ");

        assertError(input);
    }

    @Test
    void throwsOnSingleAmpersand() {
        String input = "a & b";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);

        assertError(input);
    }

    @Test
    void throwsOnSinglePipe() {
        String input = "a | b";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);

        assertError(input);
    }

    @Test
    void throwsOnSingleEquals() {
        String input = "a = b";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);

        assertError(input);
    }

    @Test
    void throwsOnSingleBang() throws KeyExpressionCompileException {
        String input = "a ! b";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.NOT,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );

        assertError(input);
    }

    @Test
    void evaluatesArithmeticComparison() {
        assertTrue(KeyExpressionEvaluator.evaluate("1 + 2 * 3 == 7"));
    }

    @Test
    void evaluatesUnaryMinusComparison() {
        assertTrue(KeyExpressionEvaluator.evaluate("-1 < 0"));
    }

    @Test
    void reportsErrorForStringAddition() {
        assertError("\"a\" + \"b\"");
    }

    @Test
    void reportsErrorForDivisionByZero() {
        assertError("1 / 0");
    }

    @Test
    void reportsErrorForDiceZero() {
        assertError("DICE(0)");
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

    private static boolean hasItemWithLabel(KernelRegistry registry, UUID ownerId, String label) {
        if (registry == null || ownerId == null || label == null) {
            return false;
        }
        return registry.getEverything().values().stream()
                .filter(Objects::nonNull)
                .filter(t -> ownerId.equals(t.getOwnerId()))
                .anyMatch(t -> label.equalsIgnoreCase(t.getLabel()));
    }

    private static Plot plot(UUID id, String label, String description) {
        return new PlotBuilder()
                .withId(id)
                .withLabel(label)
                .withDescription(description)
                .build();
    }

    private static Item item(String label, String description, Thing owner) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(description)
                .withOwnerId(owner)
                .build();
    }
}

package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryOperator;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BooleanLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment;
import com.demo.adventure.engine.mechanics.keyexpr.ast.AttributeAccessNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.FunctionCallNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.IdentifierNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.NumberLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryOperator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KeyExpressionCompilerTest {

    private final KeyExpressionCompiler compiler = new KeyExpressionCompiler();

    @Test
    void parsesOrAndPrecedence() throws KeyExpressionCompileException {
        String input = "TRUE || FALSE && FALSE";

        KeyExpressionNode ast = compiler.compile(input);

        KeyExpressionNode expected = new BinaryNode(
                new BooleanLiteralNode(true),
                BinaryOperator.OR,
                new BinaryNode(
                        new BooleanLiteralNode(false),
                        BinaryOperator.AND,
                        new BooleanLiteralNode(false)
                )
        );

        assertThat(ast).isEqualTo(expected);
    }

    @Test
    void parsesAndWithGroupedOr() throws KeyExpressionCompileException {
        String input = "(TRUE || FALSE) && FALSE";

        KeyExpressionNode ast = compiler.compile(input);

        KeyExpressionNode expected = new BinaryNode(
                new BinaryNode(
                        new BooleanLiteralNode(true),
                        BinaryOperator.OR,
                        new BooleanLiteralNode(false)
                ),
                BinaryOperator.AND,
                new BooleanLiteralNode(false)
        );

        assertThat(ast).isEqualTo(expected);
    }

    @Test
    void parsesNotBeforeAnd() throws KeyExpressionCompileException {
        String input = "!TRUE && FALSE";

        KeyExpressionNode ast = compiler.compile(input);

        KeyExpressionNode expected = new BinaryNode(
                new UnaryNode(UnaryOperator.NOT, new BooleanLiteralNode(true)),
                BinaryOperator.AND,
                new BooleanLiteralNode(false)
        );

        assertThat(ast).isEqualTo(expected);
    }

    @Test
    void parsesComparisonChain() throws KeyExpressionCompileException {
        String input = "n > 15 && n < 20";

        KeyExpressionNode ast = compiler.compile(input);

        KeyExpressionNode expected = new BinaryNode(
                new BinaryNode(
                        new IdentifierNode("n"),
                        BinaryOperator.GREATER_THAN,
                        new NumberLiteralNode(15L)
                ),
                BinaryOperator.AND,
                new BinaryNode(
                        new IdentifierNode("n"),
                        BinaryOperator.LESS_THAN,
                        new NumberLiteralNode(20L)
                )
        );

        assertThat(ast).isEqualTo(expected);
    }

    @Test
    void parsesArithmeticPrecedence() throws KeyExpressionCompileException {
        String input = "1 + 2 * 3";

        KeyExpressionNode ast = compiler.compile(input);

        KeyExpressionNode expected = new BinaryNode(
                new NumberLiteralNode(1L),
                BinaryOperator.ADD,
                new BinaryNode(
                        new NumberLiteralNode(2L),
                        BinaryOperator.MULTIPLY,
                        new NumberLiteralNode(3L)
                )
        );

        assertThat(ast).isEqualTo(expected);
    }

    @Test
    void parsesUnaryMinusLiteral() throws KeyExpressionCompileException {
        String input = "-1";

        KeyExpressionNode ast = compiler.compile(input);

        KeyExpressionNode expected = new UnaryNode(
                UnaryOperator.NEGATE,
                new NumberLiteralNode(1L)
        );

        assertThat(ast).isEqualTo(expected);
    }

    @Test
    void parsesAttributeAccessWithFixture() throws KeyExpressionCompileException {
        String input = "thing.fixture(\"x\").open";

        KeyExpressionNode ast = compiler.compile(input);

        KeyExpressionNode expected = new AttributeAccessNode(
                "thing",
                List.of(
                        new AccessSegment.FixtureSegment("x"),
                        new AccessSegment.PropertySegment("open")
                )
        );

        assertThat(ast).isEqualTo(expected);
    }

    @Test
    void failsOnMissingClosingParen() {
        String input = "(TRUE || FALSE";

        assertThatThrownBy(() -> compiler.compile(input))
                .isInstanceOf(KeyExpressionCompileException.class);
    }

    @Test
    void failsOnMissingOperand() {
        String input = "TRUE ||";

        assertThatThrownBy(() -> compiler.compile(input))
                .isInstanceOf(KeyExpressionCompileException.class);
    }

    @Test
    void failsOnMissingLeftOperand() {
        String input = "&& TRUE";

        assertThatThrownBy(() -> compiler.compile(input))
                .isInstanceOf(KeyExpressionCompileException.class);
    }

    @Test
    void failsOnFunctionCallShape() throws KeyExpressionCompileException {
        String input = "HAS(TRUE)";

        KeyExpressionNode ast = compiler.compile(input);

        KeyExpressionNode expected = new FunctionCallNode("HAS", List.of(new BooleanLiteralNode(true)));

        assertThat(ast).isEqualTo(expected);
    }

    @Test
    void failsOnNestedFunctionCall() {
        String input = "HAS(SEARCH(\"x\"))";

        assertThatThrownBy(() -> compiler.compile(input))
                .isInstanceOf(KeyExpressionCompileException.class);
    }
}

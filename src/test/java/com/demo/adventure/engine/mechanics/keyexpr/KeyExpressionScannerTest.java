package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.assertError;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyExpressionScannerTest {

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
}

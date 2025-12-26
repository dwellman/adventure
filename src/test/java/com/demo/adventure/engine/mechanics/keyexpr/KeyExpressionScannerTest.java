package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.support.exceptions.KeyExpressionCompileException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    }

    @Test
    void scansFunctionLikeExpression() throws KeyExpressionCompileException {
        String input = "DICE(1,2)";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.FUNCTION,
                TokenType.LEFT_PAREN,
                TokenType.NUMBER,
                TokenType.COMMA,
                TokenType.NUMBER,
                TokenType.RIGHT_PAREN,
                TokenType.EOL
        );
        assertThat(tokens.get(2).literal).isEqualTo(1L);
        assertThat(tokens.get(4).literal).isEqualTo(2L);
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
    }

    @Test
    void scansEmptyInput() throws KeyExpressionCompileException {
        String input = "";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.EOL
        );
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
    }

    @Test
    void scansIdentifierWithUnderscore() throws KeyExpressionCompileException {
        String input = "world_clock";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
        assertThat(tokens.get(0).literal).isEqualTo("world_clock");
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
    }

    @Test
    void scansAdditionExpression() throws KeyExpressionCompileException {
        String input = "1 + 2";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.NUMBER,
                TokenType.PLUS,
                TokenType.NUMBER,
                TokenType.EOL
        );
    }

    @Test
    void scansMultiplicationExpression() throws KeyExpressionCompileException {
        String input = "2*3";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.NUMBER,
                TokenType.STAR,
                TokenType.NUMBER,
                TokenType.EOL
        );
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
    }

    @Test
    void scansDivisionBetweenWords() throws KeyExpressionCompileException {
        String input = "A/B";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.SLASH,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
    }

    @Test
    void scansDivisionBetweenBooleans() throws KeyExpressionCompileException {
        String input = "true/false";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.TRUE,
                TokenType.SLASH,
                TokenType.FALSE,
                TokenType.EOL
        );
    }

    @Test
    void scansDivisionWithSpaces() throws KeyExpressionCompileException {
        String input = "a / b";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.IDENTIFIER,
                TokenType.SLASH,
                TokenType.IDENTIFIER,
                TokenType.EOL
        );
    }

    @Test
    void scansLoneSlash() throws KeyExpressionCompileException {
        String input = "/";

        KeyExpressionScanner scanner = new KeyExpressionScanner(input);
        List<Token> tokens = scanner.getTokens();
        assertThat(tokens).extracting(t -> t.type).containsExactly(
                TokenType.SLASH,
                TokenType.EOL
        );
    }

    @Test
    void throwsOnDollarToken() {
        String input = "true $ false";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);
    }

    @Test
    void throwsOnAtSign() {
        String input = "@";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);
    }

    @Test
    void throwsOnHashSign() {
        String input = "#";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);
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
    }

    @Test
    void throwsOnSingleAmpersand() {
        String input = "a & b";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);
    }

    @Test
    void throwsOnSinglePipe() {
        String input = "a | b";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);
    }

    @Test
    void throwsOnSingleEquals() {
        String input = "a = b";

        assertThatThrownBy(() -> new KeyExpressionScanner(input))
                .isInstanceOf(KeyExpressionCompileException.class);
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
    }
}

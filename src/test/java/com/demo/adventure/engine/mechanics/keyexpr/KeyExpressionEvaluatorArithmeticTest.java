package com.demo.adventure.engine.mechanics.keyexpr;

import org.junit.jupiter.api.Test;

import static com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionTestSupport.assertError;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeyExpressionEvaluatorArithmeticTest {

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
}

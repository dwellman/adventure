package com.demo.adventure.engine.mechanics.combat;

public final class CombatRules {
    public static final int DICE_SIDES = 20;
    public static final int HIT_MIN_ROLL = 11;
    public static final int FLEE_MIN_ROLL = 11;
    public static final long UNARMED_DAMAGE = 1L;

    private CombatRules() {
    }

    public static String hitExpression() {
        return diceExpression(DICE_SIDES, HIT_MIN_ROLL);
    }

    public static String fleeExpression() {
        return diceExpression(DICE_SIDES, FLEE_MIN_ROLL);
    }

    public static String diceExpression(int sides, int minRoll) {
        return "DICE(" + sides + ") >= " + minRoll;
    }
}

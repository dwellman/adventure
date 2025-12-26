package com.demo.adventure.engine.mechanics.combat;

public record DiceCheckedReceipt(
        String contextTag,
        int size,
        int minRoll,
        int result
) implements CombatReceipt {
}

package com.demo.adventure.engine.mechanics.combat;

public record UnknownReferenceReceipt(
        String commandText,
        String token
) implements CombatReceipt {
}

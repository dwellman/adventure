package com.demo.adventure.engine.integrity;

import com.demo.adventure.engine.flow.trigger.TriggerAction;
import com.demo.adventure.engine.flow.trigger.TriggerDefinition;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.ArrayList;
import java.util.List;

final class IntegrityKeyExpressionSpecs {
    private IntegrityKeyExpressionSpecs() {
    }

    static List<IntegrityKeyExpressionSpec> collect(GameContext game) {
        if (game == null) {
            return List.of();
        }
        List<IntegrityKeyExpressionSpec> expressions = new ArrayList<>();
        for (WorldRecipe.GateSpec gate : game.save().gates()) {
            if (gate.keyString() != null && !gate.keyString().isBlank()) {
                expressions.add(new IntegrityKeyExpressionSpec(gate.keyString(), "gate:" + gate.label()));
            }
        }
        for (GameSave.ItemRecipe item : game.save().items()) {
            if (item.keyString() != null && !item.keyString().isBlank()) {
                expressions.add(new IntegrityKeyExpressionSpec(item.keyString(), "item:" + item.name()));
            }
        }
        for (TriggerDefinition trigger : game.triggers()) {
            if (trigger.key() != null && !trigger.key().isBlank()) {
                expressions.add(new IntegrityKeyExpressionSpec(trigger.key(), "trigger:" + trigger.id()));
            }
            for (TriggerAction action : trigger.actions()) {
                if (action == null) {
                    continue;
                }
                if (action.key() != null && !action.key().isBlank()) {
                    expressions.add(new IntegrityKeyExpressionSpec(action.key(), "trigger-action:" + trigger.id()));
                }
                if (action.visibilityKey() != null && !action.visibilityKey().isBlank()) {
                    expressions.add(new IntegrityKeyExpressionSpec(action.visibilityKey(), "trigger-visibility:" + trigger.id()));
                }
            }
        }
        for (CraftingRecipe recipe : game.craftingRecipes().values()) {
            if (recipe == null) {
                continue;
            }
            if (recipe.expression() != null && !recipe.expression().isBlank()) {
                expressions.add(new IntegrityKeyExpressionSpec(recipe.expression(), "craft:" + recipe.name()));
            }
        }
        return expressions;
    }
}

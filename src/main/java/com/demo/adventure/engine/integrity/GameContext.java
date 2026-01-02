package com.demo.adventure.engine.integrity;

import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.trigger.TriggerDefinition;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.domain.save.GameSave;

import java.util.List;
import java.util.Map;

record GameContext(
        String resourcePath,
        GameSave save,
        LoopConfig loopConfig,
        List<TriggerDefinition> triggers,
        Map<String, CraftingRecipe> craftingRecipes,
        Map<String, TokenType> aliases,
        List<UseSpec> useSpecs
) {
}

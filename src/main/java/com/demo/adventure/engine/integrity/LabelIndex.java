package com.demo.adventure.engine.integrity;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

record LabelIndex(Set<String> labels, Set<String> skills) {
    static LabelIndex fromRegistry(KernelRegistry registry, Map<String, CraftingRecipe> recipes) {
        Set<String> labels = new HashSet<>();
        Set<String> skills = new HashSet<>();
        if (registry != null) {
            for (Thing thing : registry.getEverything().values()) {
                if (thing == null) {
                    continue;
                }
                String label = IntegrityLabels.normalizeLabel(thing.getLabel());
                if (!label.isBlank()) {
                    labels.add(label);
                    if (thing instanceof Gate) {
                        String reversed = IntegrityLabels.reverseGateLabel(thing.getLabel());
                        if (!reversed.isBlank()) {
                            labels.add(IntegrityLabels.normalizeLabel(reversed));
                        }
                    }
                }
                if (thing instanceof Actor actor) {
                    for (String skill : actor.getSkills()) {
                        String normalized = IntegrityLabels.normalizeLabel(skill);
                        if (!normalized.isBlank()) {
                            skills.add(normalized);
                        }
                    }
                }
            }
        }
        if (recipes != null && !recipes.isEmpty()) {
            for (CraftingRecipe recipe : recipes.values()) {
                if (recipe == null) {
                    continue;
                }
                String emit = IntegrityLabels.normalizeLabel(recipe.emitLabel());
                if (!emit.isBlank()) {
                    labels.add(emit);
                }
            }
        }
        return new LabelIndex(labels, skills);
    }
}

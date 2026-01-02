package com.demo.adventure.engine.integrity;

import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Thing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

final class IntegritySimulationActions {
    private IntegritySimulationActions() {
    }

    static List<String> generateActions(
            GameRuntime runtime,
            Map<String, CraftingRecipe> recipes,
            List<UseSpec> useSpecs
    ) {
        if (runtime == null) {
            return List.of();
        }
        Set<String> actions = new LinkedHashSet<>();
        actions.add("search");

        List<Direction> exits = visibleExits(runtime);
        for (Direction direction : exits) {
            actions.add("go " + direction.toLongName().toLowerCase(Locale.ROOT));
        }

        List<String> visibleItems = runtime.visibleItemLabels();
        for (String item : visibleItems) {
            actions.add("take " + item);
        }

        List<String> inventory = runtime.inventoryLabels();
        for (String item : inventory) {
            actions.add("drop " + item);
        }

        List<String> fixtures = runtime.visibleFixtureLabels();
        for (String fixture : fixtures) {
            actions.add("open " + fixture);
        }
        for (String item : inventory) {
            actions.add("open " + item);
        }

        List<String> sources = unionLabels(inventory, fixtures, visibleItems);
        List<String> objects = unionLabels(fixtures, visibleItems, inventory);
        if (useSpecs != null && !useSpecs.isEmpty()) {
            for (UseSpec spec : useSpecs) {
                if (spec == null || spec.target() == null || spec.target().isBlank()) {
                    continue;
                }
                String target = matchLabel(spec.target(), sources);
                if (target == null) {
                    continue;
                }
                if (spec.object() == null || spec.object().isBlank()) {
                    actions.add("use " + target);
                } else {
                    String object = matchLabel(spec.object(), objects);
                    if (object != null) {
                        actions.add("use " + target + " on " + object);
                    }
                }
            }
        }
        for (String source : sources) {
            if (thingHasCells(runtime, source)) {
                actions.add("use " + source);
            }
        }

        if (recipes != null && !recipes.isEmpty()) {
            for (CraftingRecipe recipe : recipes.values()) {
                if (recipe == null || recipe.name().isBlank()) {
                    continue;
                }
                actions.add("make " + recipe.name());
            }
        }

        List<String> sorted = new ArrayList<>(actions);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    private static List<Direction> visibleExits(GameRuntime runtime) {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        if (registry == null || plotId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .filter(Gate::isVisible)
                .filter(gate -> gate.connects(plotId))
                .map(gate -> gate.directionFrom(plotId))
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Direction::toLongName))
                .toList();
    }

    private static List<String> unionLabels(List<String>... lists) {
        Set<String> labels = new LinkedHashSet<>();
        if (lists != null) {
            for (List<String> list : lists) {
                if (list == null) {
                    continue;
                }
                for (String label : list) {
                    if (label != null && !label.isBlank()) {
                        labels.add(label);
                    }
                }
            }
        }
        return new ArrayList<>(labels);
    }

    private static String matchLabel(String target, List<String> candidates) {
        if (target == null || target.isBlank() || candidates == null) {
            return null;
        }
        String normalized = IntegrityLabels.normalizeLabel(target);
        for (String candidate : candidates) {
            if (normalized.equals(IntegrityLabels.normalizeLabel(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean thingHasCells(GameRuntime runtime, String label) {
        if (runtime == null || runtime.registry() == null || label == null || label.isBlank()) {
            return false;
        }
        Thing thing = findThingByLabel(runtime.registry(), label);
        return thing != null && thing.getCells() != null && !thing.getCells().isEmpty();
    }

    private static Thing findThingByLabel(KernelRegistry registry, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        String trimmed = label.trim();
        return registry.getEverything().values().stream()
                .filter(Objects::nonNull)
                .filter(thing -> thing.getLabel() != null)
                .filter(thing -> thing.getLabel().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }
}

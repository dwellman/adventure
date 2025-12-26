package com.demo.adventure.engine.mechanics.crafting;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loads crafting recipes from a YAML file.
 *
 * Expected shape:
 * recipes:
 *   - name: Torch
 *     expression: HAS("Stick") && HAS("Rags")
 *     consume: ["Stick", "Rags"]
 *     emitLabel: Torch
 *     emitDescription: A simple torch made of stick and rags.
 */
public final class CraftingRecipeLoader {
    private CraftingRecipeLoader() {}

    public static Map<String, CraftingRecipe> load(Path path) throws IOException {
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return load(text);
    }

    public static Map<String, CraftingRecipe> load(InputStream in) throws IOException {
        if (in == null) {
            return Map.of();
        }
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        return load(text);
    }

    public static Map<String, CraftingRecipe> load(String yamlText) {
        if (yamlText == null || yamlText.isBlank()) {
            return Map.of();
        }
        Object raw = new Yaml().load(yamlText);
        if (!(raw instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Object recipesObj = map.get("recipes");
        List<?> list = recipesObj instanceof List<?> l ? l : List.of();
        Map<String, CraftingRecipe> result = new HashMap<>();
        for (Object entry : list) {
            if (!(entry instanceof Map<?, ?> m)) {
                continue;
            }
            String name = asString(m.get("name"));
            if (name.isBlank()) {
                continue;
            }
            String expression = asString(m.get("expression"));
            String emitLabel = resolveEmitLabel(m, name);
            String emitDescription = resolveEmitDescription(m);
            List<String> consume = toStringList(m.get("consume"));
            List<String> requirements = toStringList(m.get("requirements"));
            String skillTag = asString(m.get("skill"));
            CraftingRecipe recipe = new CraftingRecipeBuilder()
                    .withName(name)
                    .withExpression(expression)
                    .withConsume(consume)
                    .withRequirements(requirements)
                    .withSkillTag(skillTag)
                    .withEmitLabel(emitLabel)
                    .withEmitDescription(emitDescription)
                    .build();
            result.put(normalizeKey(name), recipe);
        }
        return result;
    }

    private static String resolveEmitLabel(Map<?, ?> m, String name) {
        if (m.containsKey("emitLabel")) {
            return asString(m.get("emitLabel"));
        }
        // Backward compatibility with older field name.
        if (m.containsKey("outputLabel")) {
            return asString(m.get("outputLabel"));
        }
        return name;
    }

    private static String resolveEmitDescription(Map<?, ?> m) {
        if (m.containsKey("emitDescription")) {
            return asString(m.get("emitDescription"));
        }
        // Backward compatibility with older field name.
        if (m.containsKey("outputDescription")) {
            return asString(m.get("outputDescription"));
        }
        return "";
    }

    private static String normalizeKey(String name) {
        return name == null ? "" : name.trim().toUpperCase(Locale.ROOT);
    }

    private static String asString(Object obj) {
        return obj == null ? "" : obj.toString();
    }

    private static List<String> toStringList(Object obj) {
        if (obj instanceof List<?> list) {
            List<String> result = new ArrayList<>();
            for (Object entry : list) {
                if (entry != null) {
                    result.add(entry.toString());
                }
            }
            return result;
        }
        return List.of();
    }
}

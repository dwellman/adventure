package com.demo.adventure.engine.cli;

import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.VerbAliasLoader;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipeLoader;
import com.demo.adventure.support.exceptions.GdlCompileException;
import com.demo.adventure.authoring.lang.gdl.GdlLoader;
import com.demo.adventure.authoring.save.io.GameSaveYamlLoader;
import com.demo.adventure.authoring.save.io.StructuredGameSaveLoader;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.loop.LoopConfigLoader;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.engine.flow.trigger.TriggerConfigLoader;
import com.demo.adventure.engine.flow.trigger.TriggerDefinition;
import com.demo.adventure.ai.runtime.smart.SmartActorSpec;
import com.demo.adventure.ai.runtime.smart.SmartActorSpecLoader;
import com.demo.adventure.ai.runtime.smart.SmartActorTagIndex;
import com.demo.adventure.ai.runtime.smart.SmartActorTagLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class RuntimeLoader {

    private RuntimeLoader() {
    }

    public static GameSave loadSave(String resourcePath) throws IOException {
        // Grounding: prefer structured filesystem games; fail loud on structured breakage
        // (no silent fallback to monolithic).
        Path fsPath = Path.of(resourcePath);
        boolean isGdl = resourcePath.toLowerCase(Locale.ROOT).endsWith(".gdl");
        if (Files.exists(fsPath)) {
            if (isGdl) {
                try {
                    return GdlLoader.load(fsPath);
                } catch (GdlCompileException ex) {
                    throw new IOException("Failed to load GDL: " + ex.getMessage(), ex);
                }
            }
            try {
                return StructuredGameSaveLoader.load(fsPath);
            } catch (Exception ex) {
                // If structured load fails, only fall back when this is not a structured game.yaml
                if (!fsPath.getFileName().toString().equalsIgnoreCase("game.yaml")) {
                    return GameSaveYamlLoader.load(fsPath);
                }
                throw new IOException("Failed to load structured game: " + ex.getMessage(), ex);
            }
        }

        ClassLoader cl = RuntimeLoader.class.getClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            String source = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            if (isGdl) {
                try {
                    return GdlLoader.load(source);
                } catch (GdlCompileException ex) {
                    throw new IOException("Failed to load GDL: " + ex.getMessage(), ex);
                }
            }
            return GameSaveYamlLoader.load(source);
        }
    }

    public static String loadBackstory(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            throw new IllegalStateException("Backstory path missing for resource: " + resourcePath);
        }
        Path dir = Path.of(resourcePath).getParent();
        if (dir != null) {
            Path backstoryPath = resolveGameFile(dir, "narrative", "backstory.md");
            if (backstoryPath != null) {
                try {
                    return Files.readString(backstoryPath, StandardCharsets.UTF_8).trim();
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to read backstory: " + backstoryPath, ex);
                }
            }
            throw new IllegalStateException(
                    "Missing backstory file: " + dir.resolve("narrative/backstory.md"
                    ));
        }
        String[] candidates = classpathCandidates(resourcePath, "narrative", "backstory.md");
        try (InputStream in = openClasspathResource(candidates)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8).trim();
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to read backstory from classpath: " + candidates[0], ex);
        }
        throw new IllegalStateException("Missing classpath backstory file: " + candidates[0]);
    }

    public static Map<String, CraftingRecipe> loadCraftingRecipes(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return Map.of();
        }
        Path dir = Path.of(resourcePath).getParent();
        if (dir != null) {
            Path recipesPath = resolveGameFile(dir, "world", "crafting.yaml");
            if (recipesPath != null) {
                try {
                    return CraftingRecipeLoader.load(recipesPath);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load crafting recipes: " + recipesPath, ex);
                }
            }
        }
        String[] candidates = classpathCandidates(resourcePath, "world", "crafting.yaml");
        try (InputStream in = openClasspathResource(candidates)) {
            if (in != null) {
                return CraftingRecipeLoader.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load crafting recipes from classpath: " + candidates[0], ex);
        }
        return Map.of();
    }

    public static Map<String, TokenType> loadVerbAliases(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return Map.of();
        }
        Path dir = Path.of(resourcePath).getParent();
        if (dir != null) {
            Path aliasPath = resolveGameFile(dir, "motif", "aliases.yaml");
            if (aliasPath != null) {
                try {
                    return VerbAliasLoader.load(aliasPath);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load verb aliases: " + aliasPath, ex);
                }
            }
        }
        String[] candidates = classpathCandidates(resourcePath, "motif", "aliases.yaml");
        try (InputStream in = openClasspathResource(candidates)) {
            if (in != null) {
                return VerbAliasLoader.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to load verb aliases from classpath: " + candidates[0], ex
            );
        }
        return Map.of();
    }

    public static LoopConfig loadLoopConfig(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return LoopConfig.disabled();
        }
        Path dir = Path.of(resourcePath).getParent();
        if (dir != null) {
            Path loopPath = resolveGameFile(dir, "world", "loop.yaml");
            if (loopPath != null) {
                try {
                    return LoopConfigLoader.load(loopPath);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load loop config: " + loopPath, ex);
                }
            }
        }
        String[] candidates = classpathCandidates(resourcePath, "world", "loop.yaml");
        try (InputStream in = openClasspathResource(candidates)) {
            if (in != null) {
                return LoopConfigLoader.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to load loop config from classpath: " + candidates[0], ex
            );
        }
        return LoopConfig.disabled();
    }

    public static List<TriggerDefinition> loadTriggerDefinitions(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return List.of();
        }
        Path dir = Path.of(resourcePath).getParent();
        if (dir != null) {
            Path triggersPath = resolveGameFile(dir, "world", "triggers.yaml");
            if (triggersPath != null) {
                try {
                    return TriggerConfigLoader.load(triggersPath);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load trigger config: " + triggersPath, ex);
                }
            }
        }
        String[] candidates = classpathCandidates(resourcePath, "world", "triggers.yaml");
        try (InputStream in = openClasspathResource(candidates)) {
            if (in != null) {
                return TriggerConfigLoader.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to load trigger config from classpath: " + candidates[0], ex
            );
        }
        return List.of();
    }

    public static List<SmartActorSpec> loadSmartActorSpecs(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return List.of();
        }
        Path dir = Path.of(resourcePath).getParent();
        if (dir != null) {
            Path smartPath = resolveGameFile(dir, "world", "smart-actors.yaml");
            if (smartPath != null) {
                try {
                    return SmartActorSpecLoader.load(smartPath);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load smart actors: " + smartPath, ex);
                }
            }
        }
        String[] candidates = classpathCandidates(resourcePath, "world", "smart-actors.yaml");
        try (InputStream in = openClasspathResource(candidates)) {
            if (in != null) {
                return SmartActorSpecLoader.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load smart actors from classpath: " + candidates[0], ex);
        }
        return List.of();
    }

    public static SmartActorTagIndex loadSmartActorTags(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return SmartActorTagIndex.empty();
        }
        Path dir = Path.of(resourcePath).getParent();
        if (dir != null) {
            Path tagsPath = resolveGameFile(dir, "motif", "tags.yaml");
            if (tagsPath != null) {
                try {
                    return SmartActorTagLoader.loadOptional(tagsPath);
                } catch (IOException ex) {
                    throw new IllegalStateException("Failed to load smart actor tags: " + tagsPath, ex);
                }
            }
        }
        String[] candidates = classpathCandidates(resourcePath, "motif", "tags.yaml");
        try (InputStream in = openClasspathResource(candidates)) {
            if (in != null) {
                return SmartActorTagLoader.load(in);
            }
        } catch (IOException ex) {
            throw new IllegalStateException(
                    "Failed to load smart actor tags from classpath: " + candidates[0], ex
            );
        }
        return SmartActorTagIndex.empty();
    }

    private static Path resolveGameFile(Path dir, String subdir, String filename) {
        if (dir == null || filename == null || filename.isBlank()) {
            return null;
        }
        if (subdir != null && !subdir.isBlank()) {
            Path candidate = dir.resolve(subdir).resolve(filename);
            if (Files.exists(candidate)) {
                return candidate;
            }
        }
        Path fallback = dir.resolve(filename);
        if (Files.exists(fallback)) {
            return fallback;
        }
        return null;
    }

    private static InputStream openClasspathResource(String[] candidates) {
        if (candidates == null || candidates.length == 0) {
            return null;
        }
        ClassLoader cl = RuntimeLoader.class.getClassLoader();
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            InputStream in = cl.getResourceAsStream(candidate);
            if (in != null) {
                return in;
            }
        }
        return null;
    }

    private static String[] classpathCandidates(String resourcePath, String subdir, String filename) {
        String classpath = resourcePath == null ? "" : resourcePath;
        if (classpath.startsWith("src/main/resources/")) {
            classpath = classpath.substring("src/main/resources/".length());
        }
        Path classpathPath = Path.of(classpath);
        Path parent = classpathPath.getParent();
        String base = parent == null ? "" : parent.toString().replace('\\', '/');
        String normalizedSubdir = subdir == null ? "" : subdir.trim();
        String candidate;
        if (normalizedSubdir.isBlank()) {
            candidate = base.isEmpty() ? filename : base + "/" + filename;
        } else {
            candidate = base.isEmpty()
                    ? normalizedSubdir + "/" + filename
                    : base + "/" + normalizedSubdir + "/" + filename;
        }
        String fallback = base.isEmpty() ? filename : base + "/" + filename;
        return candidate.equals(fallback) ? new String[]{candidate} : new String[]{candidate, fallback};
    }
}

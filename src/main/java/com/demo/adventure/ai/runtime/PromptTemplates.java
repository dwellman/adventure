package com.demo.adventure.ai.runtime;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tiny helper to load prompt templates from classpath resources.
 */
public final class PromptTemplates {
    private static final Map<String, String> CACHE = new ConcurrentHashMap<>();

    private PromptTemplates() {}

    public static String load(String resourcePath) {
        if (resourcePath == null || resourcePath.isBlank()) {
            return null;
        }
        String cached = CACHE.get(resourcePath);
        if (cached != null) {
            return cached;
        }
        String loaded = loadOnce(resourcePath);
        if (loaded != null) {
            CACHE.put(resourcePath, loaded);
        }
        return loaded;
    }

    private static String loadOnce(String resourcePath) {
        try (InputStream in = PromptTemplates.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                return null;
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            return null;
        }
    }
}

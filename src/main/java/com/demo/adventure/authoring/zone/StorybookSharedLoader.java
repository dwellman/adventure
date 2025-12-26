package com.demo.adventure.authoring.zone;

import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

final class StorybookSharedLoader {
    private static final String ROOT = "storybook/shared";

    private StorybookSharedLoader() {
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> load(String filename) {
        String resourcePath = ROOT + "/" + filename;
        try (InputStream in = StorybookSharedLoader.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in == null) {
                throw new IllegalStateException("Missing storybook shared config: " + resourcePath);
            }
            Object raw = new Yaml().load(in);
            if (!(raw instanceof Map<?, ?> map)) {
                throw new IllegalStateException("Storybook config must be a map: " + resourcePath);
            }
            return (Map<String, Object>) map;
        }
        catch (Exception ex) {
            throw new IllegalStateException("Failed to read storybook config: " + resourcePath + " (" + ex.getMessage() + ")", ex);
        }
    }
}

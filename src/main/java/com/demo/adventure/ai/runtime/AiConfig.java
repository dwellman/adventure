package com.demo.adventure.ai.runtime;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * Centralized AI configuration resolver. Priority:
 * JVM system properties > environment variables > application.properties (classpath or repo root).
 */
public final class AiConfig {
    private final Properties fileProps;

    private AiConfig(Properties fileProps) {
        this.fileProps = fileProps;
    }

    public static AiConfig load() {
        Properties props = new Properties();
        loadFromClasspath(props, "application.properties");
        loadFromFile(props, Path.of("application.properties"));
        return new AiConfig(props);
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = resolveString(key);
        if (value != null) {
            return Boolean.parseBoolean(value.trim());
        }
        return defaultValue;
    }

    public String getString(String key, String defaultValue) {
        String value = resolveString(key);
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        return defaultValue;
    }

    public double getDouble(String key, double defaultValue) {
        String value = resolveString(key);
        if (value != null && !value.isBlank()) {
            try {
                return Double.parseDouble(value.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    public int getInt(String key, int defaultValue) {
        String value = resolveString(key);
        if (value != null && !value.isBlank()) {
            try {
                return Integer.parseInt(value.trim());
            } catch (NumberFormatException ignored) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private String resolveString(String key) {
        String sys = System.getProperty(key);
        if (sys != null && !sys.isBlank()) {
            return sys;
        }
        String envKey = key.replace('.', '_').toUpperCase(Locale.ROOT);
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String file = fileProps.getProperty(key);
        if (file != null && !file.isBlank()) {
            return file;
        }
        return null;
    }

    private static void loadFromClasspath(Properties props, String resourceName) {
        try (InputStream in = AiConfig.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in != null) {
                props.load(in);
            }
        } catch (Exception ignored) {
            // Best-effort
        }
    }

    private static void loadFromFile(Properties props, Path path) {
        try {
            if (Files.exists(path)) {
                try (InputStream in = Files.newInputStream(path)) {
                    props.load(in);
                }
            }
        } catch (Exception ignored) {
            // Best-effort
        }
    }
}

package com.demo.adventure.buui;

import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FormatterRegistry {

    private final Map<Class<?>, Formatter> registry = new HashMap<>();
    private final Formatter fallback = value -> value == null ? "" : value.toString();

    public FormatterRegistry register(Class<?> type, Formatter formatter) {
        registry.put(type, formatter);
        return this;
    }

    public String format(Object value) {
        if (value == null) {
            return "";
        }

        Formatter formatter = registry.get(value.getClass());
        if (formatter != null) {
            return Objects.toString(formatter.format(value), "");
        }

        for (var entry : registry.entrySet()) {
            if (entry.getKey().isInstance(value)) {
                return Objects.toString(entry.getValue().format(value), "");
            }
        }

        return fallback.format(value);
    }

    public static FormatterRegistry defaults() {
        FormatterRegistry registry = new FormatterRegistry();
        registry
                .register(Integer.class, v -> Integer.toString((Integer) v))
                .register(Long.class, v -> Long.toString((Long) v))
                .register(Float.class, v -> new DecimalFormat("#,##0.###").format(v))
                .register(Double.class, v -> new DecimalFormat("#,##0.###").format(v))
                .register(Boolean.class, v -> Boolean.TRUE.equals(v) ? "true" : "false")
                .register(String.class, v -> (String) v)
                .register(TemporalAccessor.class, v -> DateTimeFormatter.ISO_LOCAL_DATE_TIME.format((TemporalAccessor) v));
        return registry;
    }
}

package com.demo.adventure.authoring.save.io;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FootprintRuleLoader {
    private FootprintRuleLoader() {
    }

    public static List<FootprintRule> load(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return List.of();
        }
        String text = Files.readString(path, StandardCharsets.UTF_8);
        return load(text, path.toString());
    }

    public static List<FootprintRule> load(InputStream in) throws IOException {
        if (in == null) {
            return List.of();
        }
        String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        return load(text, "classpath");
    }

    private static List<FootprintRule> load(String yamlText, String source) throws IOException {
        if (yamlText == null || yamlText.isBlank()) {
            return List.of();
        }
        Object raw = new Yaml().load(yamlText);
        if (!(raw instanceof Map<?, ?> root)) {
            throw new IOException("Footprint rules root must be a map: " + source);
        }
        Object rulesRaw = root.get("rules");
        List<?> rules = rulesRaw instanceof List<?> list ? list : List.of();
        List<FootprintRule> result = new ArrayList<>();
        int index = 0;
        for (Object entry : rules) {
            index++;
            if (!(entry instanceof Map<?, ?> ruleMap)) {
                continue;
            }
            Map<?, ?> match = ruleMap.get("match") instanceof Map<?, ?> map ? map : Map.of();
            Map<?, ?> size = ruleMap.get("size") instanceof Map<?, ?> map ? map : Map.of();
            String contains = string(match.get("contains"));
            Double width = number(size.get("w"), size.get("width"));
            Double height = number(size.get("h"), size.get("height"));
            if (contains.isBlank() || width == null || height == null) {
                throw new IOException("Invalid footprint rule #" + index + " in " + source);
            }
            result.add(new FootprintRule(contains, width, height));
        }
        return result;
    }

    private static String string(Object raw) {
        return raw == null ? "" : raw.toString().trim();
    }

    private static Double number(Object primary, Object secondary) {
        Double value = number(primary);
        return value != null ? value : number(secondary);
    }

    private static Double number(Object raw) {
        if (raw instanceof Number num) {
            return num.doubleValue();
        }
        if (raw == null) {
            return null;
        }
        try {
            return Double.parseDouble(raw.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}

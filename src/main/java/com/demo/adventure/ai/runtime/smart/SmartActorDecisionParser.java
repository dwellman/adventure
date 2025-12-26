package com.demo.adventure.ai.runtime.smart;

import org.yaml.snakeyaml.Yaml;

import java.util.Map;

public final class SmartActorDecisionParser {
    private SmartActorDecisionParser() {
    }

    public static Result parse(String raw) {
        if (raw == null) {
            return Result.error("smart actor returned null");
        }
        if (raw.indexOf('\n') >= 0 || raw.indexOf('\r') >= 0) {
            return Result.error("smart actor returned multiple lines");
        }
        String trimmed = raw.trim();
        if (trimmed.isBlank()) {
            return Result.error("smart actor returned empty output");
        }
        Object parsed = new Yaml().load(trimmed);
        if (!(parsed instanceof Map<?, ?> map)) {
            return Result.error("smart actor output must be a JSON object");
        }
        String typeRaw = readString(map.get("type"));
        SmartActorDecision.Type type = SmartActorDecision.Type.parse(typeRaw);
        if (type == null) {
            return Result.error("smart actor output missing type");
        }
        String utterance = readString(map.get("utterance"));
        String color = readString(map.get("color"));
        String rule = readString(map.get("rule"));
        if (type == SmartActorDecision.Type.UTTERANCE && utterance.isBlank()) {
            return Result.error("smart actor output missing utterance");
        }
        if (type == SmartActorDecision.Type.COLOR && color.isBlank()) {
            return Result.error("smart actor output missing color");
        }
        return Result.success(new SmartActorDecision(type, utterance, color, rule));
    }

    private static String readString(Object value) {
        return value == null ? "" : value.toString().trim();
    }

    public record Result(Type type, SmartActorDecision decision, String error) {
        public enum Type {DECISION, ERROR}

        public static Result success(SmartActorDecision decision) {
            return new Result(Type.DECISION, decision, null);
        }

        public static Result error(String message) {
            return new Result(Type.ERROR, null, message == null ? "" : message.trim());
        }
    }
}

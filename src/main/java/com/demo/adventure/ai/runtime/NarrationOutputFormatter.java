package com.demo.adventure.ai.runtime;

import java.util.List;
import java.util.Locale;

final class NarrationOutputFormatter {
    private static final String EMOTE_PREFIX = "EMOTE:";
    private static final String CHECK_REQUEST_PREFIX = "CHECK_REQUEST:";
    private static final String CHECK_RESULT_PREFIX = "CHECK_RESULT:";

    private NarrationOutputFormatter() {
    }

    static String buildColorFallback(String snapshot, String colorEvent) {
        String[] lines = (snapshot == null ? "" : snapshot).split("\\r?\\n");
        String location = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (location.isEmpty() && !trimmed.isEmpty()) {
                location = stripHeadingPrefix(trimmed);
                break;
            }
        }
        if (location.isEmpty()) {
            location = "This place";
        }
        String color = colorEvent == null ? "" : colorEvent.trim();
        StringBuilder sb = new StringBuilder();
        if (!location.isBlank()) {
            sb.append(location);
            if (!location.endsWith(".")) {
                sb.append(".");
            }
        }
        if (!color.isBlank()) {
            if (!sb.isEmpty()) {
                sb.append(" ");
            }
            sb.append(color);
            if (!color.endsWith(".")) {
                sb.append(".");
            }
        }
        return sb.toString().trim();
    }

    static String deterministicNarration(String rawEngineOutput, String sceneSnapshot, NarrationPromptMode mode) {
        if (rawEngineOutput == null || rawEngineOutput.isBlank()) {
            return "";
        }
        String raw = rawEngineOutput.trim();
        if (isEmoteActionResult(raw)) {
            String normalized = normalizeEmoteOutput(raw);
            return normalized.isBlank() ? stripEmotePrefix(raw).trim() : normalized;
        }
        boolean includeExits = mode == NarrationPromptMode.SCENE;
        if (containsExitsLine(rawEngineOutput)) {
            return includeExits ? raw : stripExitsLines(raw);
        }
        if (!includeExits) {
            return raw;
        }
        String exitsLine = extractExitsLine(rawEngineOutput, sceneSnapshot);
        if (exitsLine.isBlank()) {
            return raw;
        }
        return attachExits(raw, exitsLine);
    }

    static String normalizeNarratorOutput(String text, String rawEngineOutput, String sceneSnapshot, NarrationPromptMode mode) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = stripEmptyFooters(text.trim());
        if (isMultiSceneError(trimmed)) {
            return trimmed;
        }
        if (isEmoteActionResult(rawEngineOutput)) {
            String normalized = normalizeEmoteOutput(trimmed);
            if (!normalized.isBlank()) {
                return normalized;
            }
            return stripEmotePrefix(rawEngineOutput).trim();
        }
        String exitsLine = extractExitsLine(rawEngineOutput, sceneSnapshot);
        String body = stripExitsLines(trimmed);
        body = stripLeadingLocationSentence(body).stripLeading();
        body = collapseParagraphs(body);
        if (mode == NarrationPromptMode.SCENE && containsExitsLine(rawEngineOutput)) {
            body = appendSceneLists(body, rawEngineOutput);
        }
        if (mode == NarrationPromptMode.SCENE) {
            return attachExits(body, exitsLine);
        }
        return body.stripTrailing();
    }

    static String extractExitsLine(String rawEngineOutput, String sceneSnapshot) {
        String source = rawEngineOutput != null && containsExitsLine(rawEngineOutput)
                ? rawEngineOutput
                : sceneSnapshot;
        if (source == null || source.isBlank()) {
            return "";
        }
        String[] lines = source.split("\\R", -1);
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("exits:")) {
                return trimmed;
            }
        }
        return "";
    }

    static String attachExits(String body, String exitsLine) {
        String trimmedBody = body == null ? "" : body.stripTrailing();
        if (exitsLine == null || exitsLine.isBlank()) {
            return trimmedBody;
        }
        List<String> merged = new java.util.ArrayList<>();
        if (!trimmedBody.isBlank()) {
            merged.add(trimmedBody);
            merged.add("");
        }
        merged.add(exitsLine);
        return String.join("\n", merged).stripTrailing();
    }

    static String stripLeadingLocationSentence(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.stripLeading();
        if (!trimmed.toLowerCase(Locale.ROOT).startsWith("you are in")) {
            return trimmed;
        }
        int period = trimmed.indexOf('.');
        if (period == -1) {
            return "";
        }
        return trimmed.substring(period + 1);
    }

    static String stripHeadingPrefix(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        int idx = 0;
        while (idx < trimmed.length() && trimmed.charAt(idx) == '#') {
            idx++;
        }
        if (idx == 0) {
            return trimmed;
        }
        String stripped = trimmed.substring(idx).trim();
        return stripped.isEmpty() ? trimmed : stripped;
    }

    static String appendSceneLists(String body, String sceneSource) {
        if (sceneSource == null || sceneSource.isBlank()) {
            return body == null ? "" : body;
        }
        List<String> fixtures = extractSectionLines(sceneSource, "fixtures:");
        List<String> items = extractSectionLines(sceneSource, "items:");
        if (fixtures.isEmpty() && items.isEmpty()) {
            return body == null ? "" : body;
        }
        StringBuilder sb = new StringBuilder();
        String trimmedBody = body == null ? "" : body.stripTrailing();
        if (!trimmedBody.isBlank()) {
            sb.append(trimmedBody);
        }
        if (!fixtures.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Fixtures:");
            for (String line : fixtures) {
                sb.append("\n").append(line);
            }
        }
        if (!items.isEmpty()) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            sb.append("Items:");
            for (String line : items) {
                sb.append("\n").append(line);
            }
        }
        return sb.toString();
    }

    static List<String> extractSectionLines(String source, String header) {
        if (source == null || source.isBlank() || header == null || header.isBlank()) {
            return List.of();
        }
        String target = header.trim().toLowerCase(Locale.ROOT);
        List<String> lines = new java.util.ArrayList<>();
        String[] rawLines = source.split("\\R", -1);
        boolean inSection = false;
        for (String line : rawLines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith(target)) {
                inSection = true;
                continue;
            }
            if (inSection && isSceneSectionHeader(lower)) {
                break;
            }
            if (inSection) {
                lines.add(trimmed);
            }
        }
        return lines;
    }

    static boolean isSceneSectionHeader(String trimmedLower) {
        if (trimmedLower == null || trimmedLower.isBlank()) {
            return false;
        }
        return trimmedLower.startsWith("fixtures:")
                || trimmedLower.startsWith("items:")
                || trimmedLower.startsWith("exits:");
    }

    static boolean isMultiSceneError(String text) {
        if (text == null) {
            return false;
        }
        return text.trim().startsWith("ERROR: MULTI_SCENE_INPUT");
    }

    static boolean containsExitsLine(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String[] lines = text.split("\\R", -1);
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("exits:")) {
                return true;
            }
        }
        return false;
    }

    static boolean isEmoteActionResult(String rawEngineOutput) {
        if (rawEngineOutput == null || rawEngineOutput.isBlank()) {
            return false;
        }
        String trimmed = rawEngineOutput.trim();
        if (trimmed.length() < EMOTE_PREFIX.length()) {
            return false;
        }
        return trimmed.regionMatches(true, 0, EMOTE_PREFIX, 0, EMOTE_PREFIX.length())
                || trimmed.regionMatches(true, 0, CHECK_REQUEST_PREFIX, 0, CHECK_REQUEST_PREFIX.length())
                || trimmed.regionMatches(true, 0, CHECK_RESULT_PREFIX, 0, CHECK_RESULT_PREFIX.length());
    }

    static String normalizeEmoteOutput(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String stripped = stripEmotePrefix(text.trim());
        String[] lines = stripped.split("\\R", -1);
        List<String> kept = new java.util.ArrayList<>();
        for (String line : lines) {
            String value = line == null ? "" : line.trim();
            if (value.isEmpty()) {
                continue;
            }
            if (value.toLowerCase(Locale.ROOT).startsWith("exits:")) {
                continue;
            }
            kept.add(value);
        }
        String joined = String.join(" ", kept).trim();
        String withoutLocation = stripLeadingLocationSentence(joined).trim();
        return stripEmotePrefix(withoutLocation).trim();
    }

    static String stripEmotePrefix(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() < EMOTE_PREFIX.length()) {
            return trimmed;
        }
        if (trimmed.regionMatches(true, 0, EMOTE_PREFIX, 0, EMOTE_PREFIX.length())) {
            return trimmed.substring(EMOTE_PREFIX.length()).trim();
        }
        if (trimmed.regionMatches(true, 0, CHECK_REQUEST_PREFIX, 0, CHECK_REQUEST_PREFIX.length())) {
            return trimmed.substring(CHECK_REQUEST_PREFIX.length()).trim();
        }
        if (trimmed.regionMatches(true, 0, CHECK_RESULT_PREFIX, 0, CHECK_RESULT_PREFIX.length())) {
            return trimmed.substring(CHECK_RESULT_PREFIX.length()).trim();
        }
        return trimmed;
    }

    static String stripEmptyFooters(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String[] lines = text.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("fixtures:")
                    || trimmed.toLowerCase(Locale.ROOT).startsWith("items:")) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    static String collapseParagraphs(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        List<String> lines = List.of(text.split("\\R", -1));
        List<String> parts = new java.util.ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                if (current.length() > 0) {
                    parts.add(current.toString().trim());
                    current.setLength(0);
                }
                continue;
            }
            if (current.length() > 0) {
                current.append(' ');
            }
            current.append(trimmed);
        }
        if (current.length() > 0) {
            parts.add(current.toString().trim());
        }
        if (parts.isEmpty()) {
            return "";
        }
        return String.join(" ", parts).trim();
    }

    static String stripExitsLines(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        List<String> lines = new java.util.ArrayList<>(List.of(text.split("\\R", -1)));
        lines.removeIf(line -> {
            String value = line == null ? "" : line.trim();
            return value.toLowerCase(Locale.ROOT).startsWith("exits");
        });
        while (!lines.isEmpty() && (lines.get(lines.size() - 1) == null || lines.get(lines.size() - 1).isBlank())) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines).stripTrailing();
    }
}

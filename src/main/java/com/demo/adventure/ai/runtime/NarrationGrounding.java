package com.demo.adventure.ai.runtime;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class NarrationGrounding {
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "to", "of", "in", "on", "at", "by", "for",
            "from", "with", "without", "into", "onto", "as", "is", "are", "was", "were", "be",
            "been", "being", "this", "that", "these", "those", "you", "your", "yours", "i",
            "we", "they", "he", "she", "it", "its", "there", "here", "around", "about",
            "over", "under", "near", "far", "through", "within", "each", "every",
            "no", "not", "only", "just", "if", "then", "than", "so"
    );
    private static final Set<String> DIRECTION_TOKENS = Set.of(
            "north", "south", "east", "west", "northeast", "northwest", "southeast", "southwest",
            "up", "down"
    );
    private static final Pattern WORD_PATTERN = Pattern.compile("[A-Za-z]+");

    private NarrationGrounding() {
    }

    static boolean isGroundedNarration(String text, String rawEngineOutput, NarrationPromptMode mode) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (rawEngineOutput == null || rawEngineOutput.isBlank()) {
            return true;
        }
        String source = rawEngineOutput;
        return unitsAreGrounded(text, source, mode);
    }

    static boolean isGroundedNarrationSnapshot(String text, String sceneSnapshot, String colorEvent, NarrationPromptMode mode) {
        if (text == null || text.isBlank()) {
            return false;
        }
        StringBuilder sb = new StringBuilder();
        if (sceneSnapshot != null && !sceneSnapshot.isBlank()) {
            sb.append(sceneSnapshot.trim());
        }
        if (colorEvent != null && !colorEvent.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(colorEvent.trim());
        }
        String source = sb.toString();
        if (source.isBlank()) {
            return true;
        }
        return unitsAreGrounded(text, source, mode);
    }

    private static boolean unitsAreGrounded(String text, String source, NarrationPromptMode mode) {
        List<String> outputUnits = extractUnits(NarrationOutputFormatter.stripExitsLines(text));
        if (outputUnits.isEmpty()) {
            return false;
        }
        List<String> sourceUnits = extractUnits(source);
        if (sourceUnits.isEmpty()) {
            return false;
        }
        Set<String> sourceListUnits = new java.util.HashSet<>();
        for (String unit : sourceUnits) {
            if (isListUnit(unit)) {
                sourceListUnits.add(normalizeListUnit(unit));
            }
        }
        Set<String> sourceTokens = extractContentTokens(sourceUnits);
        if (sourceTokens.isEmpty()) {
            return false;
        }
        if (mode == NarrationPromptMode.LOOK_DIRECTION) {
            Set<String> requiredDirections = directionTokensInSource(sourceTokens);
            if (!requiredDirections.isEmpty()) {
                Set<String> outputTokens = extractContentTokens(outputUnits);
                if (!outputTokens.containsAll(requiredDirections)) {
                    return false;
                }
            }
        }
        for (String unit : outputUnits) {
            if (isListUnit(unit)) {
                if (!sourceListUnits.contains(normalizeListUnit(unit))) {
                    return false;
                }
                continue;
            }
            if (!sentenceIsGrounded(unit, sourceTokens, mode)) {
                return false;
            }
        }
        return true;
    }

    private static List<String> extractUnits(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        List<String> units = new java.util.ArrayList<>();
        String[] lines = text.split("\\R", -1);
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.startsWith("exits:")) {
                continue;
            }
            if (lower.startsWith("#")) {
                continue;
            }
            if (lower.startsWith("fixtures:") || lower.startsWith("items:")
                    || trimmed.startsWith("- ") || trimmed.startsWith("\u2022 ")) {
                units.add(normalizeListUnit(trimmed));
                continue;
            }
            units.addAll(splitSentences(trimmed));
        }
        return units;
    }

    private static boolean isListUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return false;
        }
        String trimmed = unit.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        return lower.startsWith("fixtures:")
                || lower.startsWith("items:")
                || trimmed.startsWith("- ")
                || trimmed.startsWith("\u2022 ");
    }

    private static String normalizeListUnit(String unit) {
        if (unit == null) {
            return "";
        }
        String trimmed = unit.trim();
        if (trimmed.startsWith("\u2022 ")) {
            return "- " + trimmed.substring(2).trim();
        }
        return trimmed;
    }

    private static Set<String> extractContentTokens(List<String> units) {
        Set<String> tokens = new java.util.HashSet<>();
        for (String unit : units) {
            if (unit == null || unit.isBlank()) {
                continue;
            }
            tokens.addAll(contentTokens(unit));
        }
        return tokens;
    }

    private static Set<String> directionTokensInSource(Set<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return Set.of();
        }
        Set<String> directions = new java.util.HashSet<>();
        for (String token : tokens) {
            if (DIRECTION_TOKENS.contains(token)) {
                directions.add(token);
            }
        }
        return directions;
    }

    private static List<String> contentTokens(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }
        List<String> tokens = new java.util.ArrayList<>();
        Matcher matcher = WORD_PATTERN.matcher(line);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase(Locale.ROOT);
            if (STOPWORDS.contains(word)) {
                continue;
            }
            tokens.add(word);
        }
        return tokens;
    }

    private static boolean sentenceIsGrounded(String unit, Set<String> sourceTokens, NarrationPromptMode mode) {
        List<String> tokens = contentTokens(unit);
        if (tokens.isEmpty()) {
            return true;
        }
        if (mode == NarrationPromptMode.EMOTE
                || mode == NarrationPromptMode.CHECK_REQUEST
                || mode == NarrationPromptMode.CHECK_RESULT) {
            for (String token : tokens) {
                if (DIRECTION_TOKENS.contains(token)) {
                    return false;
                }
            }
            return true;
        }
        GroundingThreshold threshold = groundingThreshold(mode);
        int unknown = 0;
        int total = 0;
        boolean hasOverlap = false;
        for (String token : tokens) {
            total++;
            boolean known = sourceTokens.contains(token);
            if (known) {
                hasOverlap = true;
            } else {
                if (DIRECTION_TOKENS.contains(token)) {
                    return false;
                }
                unknown++;
            }
        }
        if (!hasOverlap) {
            return false;
        }
        if (unknown > threshold.maxUnknownCount()) {
            return false;
        }
        double ratio = total == 0 ? 0.0d : (double) unknown / (double) total;
        return ratio <= threshold.maxUnknownRatio();
    }

    private static GroundingThreshold groundingThreshold(NarrationPromptMode mode) {
        if (mode == null) {
            return GroundingThreshold.defaultThreshold();
        }
        return switch (mode) {
            case SCENE -> new GroundingThreshold(8, 0.85d);
            case COLOR_EVENT -> new GroundingThreshold(6, 0.85d);
            case ACTION_RESULT, LOOK_TARGET -> new GroundingThreshold(6, 0.8d);
            case LOOK_DIRECTION -> new GroundingThreshold(4, 0.7d);
            case EMOTE, CHECK_REQUEST, CHECK_RESULT -> new GroundingThreshold(8, 0.9d);
        };
    }

    private record GroundingThreshold(int maxUnknownCount, double maxUnknownRatio) {
        private static GroundingThreshold defaultThreshold() {
            return new GroundingThreshold(2, 0.5d);
        }
    }

    private static List<String> splitSentences(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }
        String trimmed = line.trim();
        String[] parts = trimmed.split("(?<=[.!?])\\s+");
        List<String> out = new java.util.ArrayList<>();
        for (String part : parts) {
            String piece = part.trim();
            if (!piece.isEmpty()) {
                out.add(piece);
            }
        }
        if (out.isEmpty()) {
            return List.of(trimmed);
        }
        return out;
    }
}

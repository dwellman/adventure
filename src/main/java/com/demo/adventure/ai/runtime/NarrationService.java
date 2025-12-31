package com.demo.adventure.ai.runtime;

import com.demo.adventure.buui.BuuiConsole;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wraps narrator prompting and debugging.
 * Pattern hooks: Orchestration (engine -> narrator), Trust UX (debug toggle), Verification (fails loud on prompt errors).
 */
public final class NarrationService extends BuuiConsole {
    private static final String EMOTE_PREFIX = "EMOTE:";
    private static final String CHECK_REQUEST_PREFIX = "CHECK_REQUEST:";
    private static final String CHECK_RESULT_PREFIX = "CHECK_RESULT:";
    private static final java.util.Set<String> STOPWORDS = java.util.Set.of(
            "a", "an", "the", "and", "or", "but", "to", "of", "in", "on", "at", "by", "for",
            "from", "with", "without", "into", "onto", "as", "is", "are", "was", "were", "be",
            "been", "being", "this", "that", "these", "those", "you", "your", "yours", "i",
            "we", "they", "he", "she", "it", "its", "there", "here", "around", "about",
            "over", "under", "near", "far", "through", "within", "each", "every",
            "no", "not", "only", "just", "if", "then", "than", "so"
    );
    private static final java.util.Set<String> DIRECTION_TOKENS = java.util.Set.of(
            "north", "south", "east", "west", "northeast", "northwest", "southeast", "southwest",
            "up", "down"
    );
    private static final java.util.regex.Pattern WORD_PATTERN = java.util.regex.Pattern.compile("[A-Za-z]+");
    @FunctionalInterface
    public interface NarratorRewriter {
        String rewrite(String apiKey, String prompt, boolean debug) throws Exception;
    }

    private final boolean aiEnabled;
    private final String apiKey;
    private final boolean debug;
    private final NarratorRewriter rewriter;
    private Consumer<String> narrationObserver;

    public NarrationService(boolean aiEnabled,
                            String apiKey,
                            boolean debug) {
        this(aiEnabled, apiKey, debug, NarratorService::rewrite);
    }

    NarrationService(boolean aiEnabled,
                     String apiKey,
                     boolean debug,
                     NarratorRewriter rewriter) {
        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        this.debug = debug;
        this.rewriter = rewriter == null ? NarratorService::rewrite : rewriter;
    }

    public void setNarrationObserver(Consumer<String> narrationObserver) {
        this.narrationObserver = narrationObserver;
    }

    // Pattern: Orchestration + Trust UX
    // - Passes deterministic engine text into the narrator prompt and falls back to raw output on errors.
    public void narrateEngine(String text,
                              String sceneSnapshot,
                              String playerUtterance,
                              String canonicalCommand,
                              String backstory,
                              List<RecentAction> recentActions,
                              List<RecentNarration> recentNarrations) {
        if (text == null || text.isBlank()) {
            return;
        }
        String raw = text.trim();
        String snapshot = sceneSnapshot == null ? "" : sceneSnapshot.trim();
        if (containsExitsLine(raw)) {
            snapshot = "";
        }
        NarrationPromptMode mode = NarrationPromptSelector.select(canonicalCommand, raw, "");
        String fallback = deterministicNarration(raw, snapshot, mode);
        if (debug) {
            printText("~ narrator raw: " + raw);
            printText("~ narrator raw snapshot: " + snapshot);
            printText("~ narrator playerUtterance: " + nullToEmpty(playerUtterance));
            printText("~ narrator canonicalCommand: " + nullToEmpty(canonicalCommand));
            printText("~ narrator backstory: " + nullToEmpty(backstory));
            printText("~ narrator input json: " + debugJson("engine", raw, snapshot, "", nullToEmpty(playerUtterance), nullToEmpty(canonicalCommand), nullToEmpty(backstory)));
        }
        if (!aiEnabled) {
            emitNarration(raw);
            return;
        }
        try {
            String prompt = NarratorPromptBuilder.buildEngine(
                    nullToEmpty(playerUtterance),
                    nullToEmpty(canonicalCommand),
                    raw,
                    snapshot,
                    backstory,
                    recentActions,
                    recentNarrations
            );
            String out = rewriter.rewrite(apiKey, prompt, debug);
            if (out != null && !out.isBlank()) {
                if (isMultiSceneError(out)) {
                    if (debug) {
                        printText("~ narrator returned multi-scene error; using raw output");
                    }
                } else {
                    String normalized = normalizeNarratorOutput(out, raw, snapshot, mode);
                    if (!normalized.isBlank()) {
                        if (isEmoteActionResult(raw) || isGroundedNarration(normalized, raw, mode)) {
                            emitNarration(normalized);
                            return;
                        }
                        if (debug) {
                            printText("~ narrator output was not grounded; using deterministic output");
                        }
                        emitNarration(fallback);
                        return;
                    }
                }
            }
            if (debug) {
                printText("~ narrator returned empty; using deterministic output");
            }
        } catch (Exception ex) {
            if (debug) {
                printText("~ narrator error: " + ex.getMessage());
            }
        }
        emitNarration(fallback);
    }

    // Pattern: Orchestration + Trust UX
    // - Narrates snapshots and color events while guaranteeing a deterministic fallback path.
    public void narrateSnapshot(String sceneSnapshot,
                                String colorEvent,
                                String playerUtterance,
                                String canonicalCommand,
                                String backstory,
                                List<RecentAction> recentActions,
                                List<RecentNarration> recentNarrations) {
        if ((sceneSnapshot == null || sceneSnapshot.isBlank()) && (colorEvent == null || colorEvent.isBlank())) {
            return;
        }
        String snapshot = sceneSnapshot == null ? "" : sceneSnapshot.trim();
        if (debug) {
            printText("~ narrator raw snapshot: " + snapshot + (colorEvent == null ? "" : " | color: " + colorEvent));
            printText("~ narrator playerUtterance: " + nullToEmpty(playerUtterance));
            printText("~ narrator canonicalCommand: " + nullToEmpty(canonicalCommand));
            printText("~ narrator backstory: " + nullToEmpty(backstory));
            printText("~ narrator input json: " + debugJson("snapshot", "", snapshot, nullToEmpty(colorEvent), nullToEmpty(playerUtterance), nullToEmpty(canonicalCommand), nullToEmpty(backstory)));
        }
        if (!aiEnabled) {
            emitNarration(buildColorFallback(snapshot, colorEvent));
            return;
        }
        NarrationPromptMode mode = NarrationPromptSelector.select(canonicalCommand, "", colorEvent);
        String prompt = NarratorPromptBuilder.buildSnapshot(
                nullToEmpty(playerUtterance),
                nullToEmpty(canonicalCommand),
                snapshot,
                colorEvent,
                backstory,
                recentActions,
                recentNarrations
        );
        try {
            String out = rewriter.rewrite(apiKey, prompt, debug);
            if (out != null && !out.isBlank()) {
                if (isMultiSceneError(out)) {
                    if (debug) {
                        printText("~ narrator returned multi-scene error; using fallback");
                    }
                } else {
                    String trimmed = normalizeNarratorOutput(out, "", snapshot, mode);
                    if (!trimmed.startsWith("ERROR: MULTI_SCENE_INPUT")) {
                        if (isGroundedNarrationSnapshot(trimmed, snapshot, colorEvent, mode)) {
                            emitNarration(trimmed);
                            return;
                        }
                        if (debug) {
                            printText("~ narrator output was not grounded; using fallback");
                        }
                        emitNarration(buildColorFallback(snapshot, colorEvent));
                        return;
                    }
                    if (debug) {
                        printText("~ narrator returned multi-scene error; using fallback");
                    }
                }
            } else {
                if (debug) {
                    printText("~ narrator returned empty; using raw snapshot");
                }
            }
        } catch (Exception ex) {
            if (debug) {
                printText("~ narrator error: " + ex.getMessage());
            }
        }
        // Fallback: location + color + exits from snapshot.
        emitNarration(buildColorFallback(snapshot, colorEvent));
    }

    private void emitNarration(String text) {
        if (text == null) {
            return;
        }
        print(text);
        if (narrationObserver != null) {
            narrationObserver.accept(text);
        }
    }

    private String buildColorFallback(String snapshot, String colorEvent) {
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

    private String deterministicNarration(String rawEngineOutput, String sceneSnapshot, NarrationPromptMode mode) {
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

    private String normalizeNarratorOutput(String text, String rawEngineOutput, String sceneSnapshot, NarrationPromptMode mode) {
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

    private String extractExitsLine(String rawEngineOutput, String sceneSnapshot) {
        String source = rawEngineOutput != null && containsExitsLine(rawEngineOutput)
                ? rawEngineOutput
                : sceneSnapshot;
        if (source == null || source.isBlank()) {
            return "";
        }
        String[] lines = source.split("\\R", -1);
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("exits:")) {
                return trimmed;
            }
        }
        return "";
    }

    private String attachExits(String body, String exitsLine) {
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

    private String stripLeadingLocationSentence(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.stripLeading();
        if (!trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("you are in")) {
            return trimmed;
        }
        int period = trimmed.indexOf('.');
        if (period == -1) {
            return "";
        }
        return trimmed.substring(period + 1);
    }

    private String stripHeadingPrefix(String text) {
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

    private String appendSceneLists(String body, String sceneSource) {
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

    private List<String> extractSectionLines(String source, String header) {
        if (source == null || source.isBlank() || header == null || header.isBlank()) {
            return List.of();
        }
        String target = header.trim().toLowerCase(java.util.Locale.ROOT);
        List<String> lines = new java.util.ArrayList<>();
        String[] rawLines = source.split("\\R", -1);
        boolean inSection = false;
        for (String line : rawLines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
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

    private boolean isSceneSectionHeader(String trimmedLower) {
        if (trimmedLower == null || trimmedLower.isBlank()) {
            return false;
        }
        return trimmedLower.startsWith("fixtures:")
                || trimmedLower.startsWith("items:")
                || trimmedLower.startsWith("exits:");
    }

    private boolean isMultiSceneError(String text) {
        if (text == null) {
            return false;
        }
        return text.trim().startsWith("ERROR: MULTI_SCENE_INPUT");
    }

    private boolean containsExitsLine(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        String[] lines = text.split("\\R", -1);
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("exits:")) {
                return true;
            }
        }
        return false;
    }

    private boolean isEmoteActionResult(String rawEngineOutput) {
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

    private String normalizeEmoteOutput(String text) {
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
            if (value.toLowerCase(java.util.Locale.ROOT).startsWith("exits:")) {
                continue;
            }
            kept.add(value);
        }
        String joined = String.join(" ", kept).trim();
        String withoutLocation = stripLeadingLocationSentence(joined).trim();
        return stripEmotePrefix(withoutLocation).trim();
    }

    private String stripEmotePrefix(String text) {
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String stripEmptyFooters(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String[] lines = text.split("\\r?\\n");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("fixtures:")
                    || trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("items:")) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n");
            }
            sb.append(line);
        }
        return sb.toString();
    }

    private boolean isGroundedNarration(String text, String rawEngineOutput, NarrationPromptMode mode) {
        if (text == null || text.isBlank()) {
            return false;
        }
        if (rawEngineOutput == null || rawEngineOutput.isBlank()) {
            return true;
        }
        String source = rawEngineOutput;
        return unitsAreGrounded(text, source, mode);
    }

    private boolean isGroundedNarrationSnapshot(String text, String sceneSnapshot, String colorEvent, NarrationPromptMode mode) {
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

    private boolean unitsAreGrounded(String text, String source, NarrationPromptMode mode) {
        List<String> outputUnits = extractUnits(stripExitsLines(text));
        if (outputUnits.isEmpty()) {
            return false;
        }
        List<String> sourceUnits = extractUnits(source);
        if (sourceUnits.isEmpty()) {
            return false;
        }
        java.util.Set<String> sourceListUnits = new java.util.HashSet<>();
        for (String unit : sourceUnits) {
            if (isListUnit(unit)) {
                sourceListUnits.add(normalizeListUnit(unit));
            }
        }
        java.util.Set<String> sourceTokens = extractContentTokens(sourceUnits);
        if (sourceTokens.isEmpty()) {
            return false;
        }
        if (mode == NarrationPromptMode.LOOK_DIRECTION) {
            java.util.Set<String> requiredDirections = directionTokensInSource(sourceTokens);
            if (!requiredDirections.isEmpty()) {
                java.util.Set<String> outputTokens = extractContentTokens(outputUnits);
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

    private List<String> extractUnits(String text) {
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
            String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
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

    private boolean isListUnit(String unit) {
        if (unit == null || unit.isBlank()) {
            return false;
        }
        String trimmed = unit.trim();
        String lower = trimmed.toLowerCase(java.util.Locale.ROOT);
        return lower.startsWith("fixtures:")
                || lower.startsWith("items:")
                || trimmed.startsWith("- ")
                || trimmed.startsWith("\u2022 ");
    }

    private String normalizeListUnit(String unit) {
        if (unit == null) {
            return "";
        }
        String trimmed = unit.trim();
        if (trimmed.startsWith("\u2022 ")) {
            return "- " + trimmed.substring(2).trim();
        }
        return trimmed;
    }

    private java.util.Set<String> extractContentTokens(List<String> units) {
        java.util.Set<String> tokens = new java.util.HashSet<>();
        for (String unit : units) {
            if (unit == null || unit.isBlank()) {
                continue;
            }
            tokens.addAll(contentTokens(unit));
        }
        return tokens;
    }

    private java.util.Set<String> directionTokensInSource(java.util.Set<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return java.util.Set.of();
        }
        java.util.Set<String> directions = new java.util.HashSet<>();
        for (String token : tokens) {
            if (DIRECTION_TOKENS.contains(token)) {
                directions.add(token);
            }
        }
        return directions;
    }

    private List<String> contentTokens(String line) {
        if (line == null || line.isBlank()) {
            return List.of();
        }
        java.util.List<String> tokens = new java.util.ArrayList<>();
        java.util.regex.Matcher matcher = WORD_PATTERN.matcher(line);
        while (matcher.find()) {
            String word = matcher.group().toLowerCase(java.util.Locale.ROOT);
            if (STOPWORDS.contains(word)) {
                continue;
            }
            tokens.add(word);
        }
        return tokens;
    }

    private boolean sentenceIsGrounded(String unit, java.util.Set<String> sourceTokens, NarrationPromptMode mode) {
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

    private GroundingThreshold groundingThreshold(NarrationPromptMode mode) {
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

    private List<String> splitSentences(String line) {
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

    private String collapseParagraphs(String text) {
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

    private String stripExitsLines(String text) {
        if (text == null || text.isBlank()) {
            return text == null ? "" : text;
        }
        List<String> lines = new java.util.ArrayList<>(List.of(text.split("\\R", -1)));
        lines.removeIf(line -> {
            String value = line == null ? "" : line.trim();
            return value.toLowerCase(java.util.Locale.ROOT).startsWith("exits");
        });
        while (!lines.isEmpty() && (lines.get(lines.size() - 1) == null || lines.get(lines.size() - 1).isBlank())) {
            lines.remove(lines.size() - 1);
        }
        return String.join("\n", lines).stripTrailing();
    }

    private String debugJson(String mode, String rawEngineOutput, String sceneSnapshot, String colorEvent, String playerUtterance, String canonicalCommand, String backstory) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"mode\":\"").append(escape(mode)).append("\",");
        sb.append("\"playerUtterance\":\"").append(escape(playerUtterance)).append("\",");
        sb.append("\"canonicalCommand\":\"").append(escape(canonicalCommand)).append("\",");
        sb.append("\"backstory\":\"").append(escape(backstory)).append("\",");
        sb.append("\"rawEngineOutput\":\"").append(escape(rawEngineOutput)).append("\",");
        sb.append("\"sceneSnapshot\":\"").append(escape(sceneSnapshot)).append("\",");
        sb.append("\"colorEvent\":\"").append(escape(colorEvent)).append("\"");
        sb.append("}");
        return sb.toString();
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}

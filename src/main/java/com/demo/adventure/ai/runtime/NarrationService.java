package com.demo.adventure.ai.runtime;

import com.demo.adventure.buui.BuuiConsole;

/**
 * Wraps narrator prompting and debugging.
 * Pattern hooks: Orchestration (engine -> narrator), Trust UX (debug toggle), Verification (fails loud on prompt errors).
 */
public final class NarrationService extends BuuiConsole {
    @FunctionalInterface
    public interface NarratorRewriter {
        String rewrite(String apiKey, String prompt, boolean debug) throws Exception;
    }

    private final boolean aiEnabled;
    private final String apiKey;
    private final boolean debug;
    private final NarratorRewriter rewriter;

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

    // Pattern: Orchestration + Trust UX
    // - Passes deterministic engine text into the narrator prompt and falls back to raw output on errors.
    public void narrateEngine(String text, String sceneSnapshot, String playerUtterance, String canonicalCommand, String backstory) {
        if (text == null || text.isBlank()) {
            return;
        }
        String raw = text.trim();
        String snapshot = sceneSnapshot == null ? "" : sceneSnapshot.trim();
        if (debug) {
            printText("~ narrator raw: " + raw);
            printText("~ narrator raw snapshot: " + snapshot);
            printText("~ narrator playerUtterance: " + nullToEmpty(playerUtterance));
            printText("~ narrator canonicalCommand: " + nullToEmpty(canonicalCommand));
            printText("~ narrator backstory: " + nullToEmpty(backstory));
            printText("~ narrator input json: " + debugJson("engine", raw, snapshot, "", nullToEmpty(playerUtterance), nullToEmpty(canonicalCommand), nullToEmpty(backstory)));
        }
        if (!aiEnabled) {
            print(raw);
            return;
        }
        try {
            String prompt = NarratorPromptBuilder.buildEngine(nullToEmpty(playerUtterance), nullToEmpty(canonicalCommand), raw, snapshot, backstory);
            if (debug) {
                printText("~ narrator prompt (engine):\n" + prompt);
            }
            String out = rewriter.rewrite(apiKey, prompt, debug);
            if (out != null && !out.isBlank()) {
                print(stripEmptyFooters(out.trim()));
                return;
            }
            printText("~ narrator returned empty; using raw output");
        } catch (Exception ex) {
            printText("~ narrator error: " + ex.getMessage());
        }
        print(raw);
    }

    // Pattern: Orchestration + Trust UX
    // - Narrates snapshots and color events while guaranteeing a deterministic fallback path.
    public void narrateSnapshot(String sceneSnapshot, String colorEvent, String playerUtterance, String canonicalCommand, String backstory) {
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
            String combined = snapshot.isBlank() ? colorEvent : snapshot + (colorEvent == null ? "" : "\n" + colorEvent);
            print(combined);
            return;
        }
        String prompt = NarratorPromptBuilder.buildSnapshot(nullToEmpty(playerUtterance), nullToEmpty(canonicalCommand), snapshot, colorEvent, backstory);
        if (debug) {
            printText("~ narrator prompt (snapshot):\n" + prompt);
        }
        try {
            String out = rewriter.rewrite(apiKey, prompt, debug);
            if (out != null && !out.isBlank()) {
                String trimmed = stripEmptyFooters(out.trim());
                if (!trimmed.startsWith("ERROR: MULTI_SCENE_INPUT")) {
                    print(trimmed);
                    return;
                }
                printText("~ narrator returned multi-scene error; using fallback");
            } else {
                printText("~ narrator returned empty; using raw snapshot");
            }
        } catch (Exception ex) {
            printText("~ narrator error: " + ex.getMessage());
        }
        // Fallback: location + color + exits from snapshot.
        print(buildColorFallback(snapshot, colorEvent));
    }

    private String buildColorFallback(String snapshot, String colorEvent) {
        String[] lines = (snapshot == null ? "" : snapshot).split("\\r?\\n");
        String location = lines.length > 0 ? lines[0].trim() : "This place";
        String exits = "";
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.toLowerCase(java.util.Locale.ROOT).startsWith("exits")) {
                exits = trimmed.replace("Exits are available to the", "Exits lead").replace("Exits:", "Exits lead");
                if (!exits.endsWith(".")) {
                    exits = exits + ".";
                }
                break;
            }
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
        if (!exits.isBlank()) {
            if (sb.length() > 0) {
                sb.append("\n");
            }
            sb.append(exits);
        }
        return sb.toString().trim();
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

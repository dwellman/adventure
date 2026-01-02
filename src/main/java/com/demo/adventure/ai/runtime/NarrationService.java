package com.demo.adventure.ai.runtime;

import com.demo.adventure.buui.BuuiConsole;
import java.util.List;
import java.util.function.Consumer;

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
    private final NarrationTransport transport;
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
        this.transport = new NarrationTransport(aiEnabled, apiKey, debug, this.rewriter);
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
        if (NarrationOutputFormatter.containsExitsLine(raw)) {
            snapshot = "";
        }
        NarrationPromptMode mode = NarrationPromptSelector.select(canonicalCommand, raw, "");
        String fallback = NarrationOutputFormatter.deterministicNarration(raw, snapshot, mode);
        if (debug) {
            printText("~ narrator raw: " + raw);
            printText("~ narrator raw snapshot: " + snapshot);
            printText("~ narrator playerUtterance: " + nullToEmpty(playerUtterance));
            printText("~ narrator canonicalCommand: " + nullToEmpty(canonicalCommand));
            printText("~ narrator backstory: " + nullToEmpty(backstory));
            printText("~ narrator input json: " + NarrationDebugPayload.debugJson(
                    "engine",
                    raw,
                    snapshot,
                    "",
                    nullToEmpty(playerUtterance),
                    nullToEmpty(canonicalCommand),
                    nullToEmpty(backstory)
            ));
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
            String out = transport.rewrite(prompt);
            if (out != null && !out.isBlank()) {
                if (NarrationOutputFormatter.isMultiSceneError(out)) {
                    if (debug) {
                        printText("~ narrator returned multi-scene error; using raw output");
                    }
                } else {
                    String normalized = NarrationOutputFormatter.normalizeNarratorOutput(out, raw, snapshot, mode);
                    if (!normalized.isBlank()) {
                        if (NarrationOutputFormatter.isEmoteActionResult(raw)
                                || NarrationGrounding.isGroundedNarration(normalized, raw, mode)) {
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
            printText("~ narrator input json: " + NarrationDebugPayload.debugJson(
                    "snapshot",
                    "",
                    snapshot,
                    nullToEmpty(colorEvent),
                    nullToEmpty(playerUtterance),
                    nullToEmpty(canonicalCommand),
                    nullToEmpty(backstory)
            ));
        }
        if (!aiEnabled) {
            emitNarration(NarrationOutputFormatter.buildColorFallback(snapshot, colorEvent));
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
            String out = transport.rewrite(prompt);
            if (out != null && !out.isBlank()) {
                if (NarrationOutputFormatter.isMultiSceneError(out)) {
                    if (debug) {
                        printText("~ narrator returned multi-scene error; using fallback");
                    }
                } else {
                    String trimmed = NarrationOutputFormatter.normalizeNarratorOutput(out, "", snapshot, mode);
                    if (!trimmed.startsWith("ERROR: MULTI_SCENE_INPUT")) {
                        if (NarrationGrounding.isGroundedNarrationSnapshot(trimmed, snapshot, colorEvent, mode)) {
                            emitNarration(trimmed);
                            return;
                        }
                        if (debug) {
                            printText("~ narrator output was not grounded; using fallback");
                        }
                        emitNarration(NarrationOutputFormatter.buildColorFallback(snapshot, colorEvent));
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
        emitNarration(NarrationOutputFormatter.buildColorFallback(snapshot, colorEvent));
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

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

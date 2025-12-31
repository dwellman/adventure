package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.NarrationService;
import com.demo.adventure.ai.runtime.RecentAction;
import com.demo.adventure.ai.runtime.RecentNarration;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Deque;
import java.util.List;

public final class SceneNarrator {
    private static final int RECENT_ACTION_LIMIT = 2;
    private static final int RECENT_NARRATION_LIMIT = 2;

    private final NarrationService narrationService;
    private String lastState = "";
    private String lastSceneHeader = "";
    private String lastCommand = "";
    private String lastUtterance = "";
    private String backstory = "";
    private final Deque<RecentAction> recentActions = new ArrayDeque<>();
    private final Deque<RecentNarration> recentNarrations = new ArrayDeque<>();

    public SceneNarrator(NarrationService narrationService) {
        this.narrationService = narrationService;
        if (this.narrationService != null) {
            this.narrationService.setNarrationObserver(this::recordRecentNarration);
        }
    }

    public void setBackstory(String backstory) {
        this.backstory = backstory == null ? "" : backstory.trim();
    }

    public void setLastUtterance(String lastUtterance) {
        this.lastUtterance = lastUtterance == null ? "" : lastUtterance;
    }

    public void setLastCommand(String lastCommand) {
        this.lastCommand = lastCommand == null ? "" : lastCommand;
    }

    public String lastState() {
        return lastState;
    }

    public String lastCommand() {
        return lastCommand;
    }

    public void resetScene() {
        lastState = "";
        lastSceneHeader = "";
        recentActions.clear();
        recentNarrations.clear();
    }

    public void updateScene(String rawScene) {
        lastState = rawScene == null ? "" : rawScene.trim();
        lastSceneHeader = sceneHeaderFromSnapshot(lastState);
    }

    public void narrate(String text) {
        String snapshot = selectSceneSnapshot(text);
        recordRecentAction(lastCommand, text);
        narrationService.narrateEngine(text, snapshot, lastUtterance, lastCommand, backstory, recentActions(), recentNarrations());
    }

    public void narrateColor(String colorEvent) {
        narrationService.narrateSnapshot(lastState, colorEvent, lastUtterance, lastCommand, backstory, recentActions(), recentNarrations());
    }

    private String selectSceneSnapshot(String rawEngineOutput) {
        if (rawEngineOutput == null || rawEngineOutput.isBlank()) {
            return lastState;
        }
        if (rawEngineOutput.contains("Exits:")) {
            return lastState;
        }
        if (lastSceneHeader != null && !lastSceneHeader.isBlank()) {
            return lastSceneHeader;
        }
        return lastState;
    }

    private String sceneHeaderFromSnapshot(String snapshot) {
        if (snapshot == null || snapshot.isBlank()) {
            return "";
        }
        String[] lines = snapshot.split("\\R", -1);
        String location = "";
        String exits = "";
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            if (location.isEmpty()) {
                location = stripHeadingPrefix(lines[i].trim());
                continue;
            }
            if (line.toLowerCase(Locale.ROOT).startsWith("exits:")) {
                exits = lines[i].trim();
                break;
            }
        }
        if (location.isEmpty() && exits.isEmpty()) {
            return "";
        }
        if (exits.isEmpty()) {
            return location;
        }
        return location + "\n" + exits;
    }

    private void recordRecentAction(String canonicalCommand, String rawEngineOutput) {
        if (canonicalCommand == null || canonicalCommand.isBlank()) {
            return;
        }
        String sanitized = sanitizeActionForMemory(rawEngineOutput);
        if (sanitized.isBlank()) {
            return;
        }
        recentActions.addLast(new RecentAction(canonicalCommand.trim(), sanitized));
        while (recentActions.size() > RECENT_ACTION_LIMIT) {
            recentActions.removeFirst();
        }
    }

    private List<RecentAction> recentActions() {
        return new ArrayList<>(recentActions);
    }

    private List<RecentNarration> recentNarrations() {
        return new ArrayList<>(recentNarrations);
    }

    private void recordRecentNarration(String narration) {
        String sanitized = sanitizeNarrationForMemory(narration);
        if (sanitized.isBlank()) {
            return;
        }
        if (!recentNarrations.isEmpty()) {
            RecentNarration last = recentNarrations.peekLast();
            if (last != null && sanitized.equals(last.text())) {
                return;
            }
        }
        recentNarrations.addLast(new RecentNarration(sanitized));
        while (recentNarrations.size() > RECENT_NARRATION_LIMIT) {
            recentNarrations.removeFirst();
        }
    }

    private String sanitizeActionForMemory(String rawEngineOutput) {
        if (rawEngineOutput == null || rawEngineOutput.isBlank()) {
            return "";
        }
        String normalized = rawEngineOutput.replace("\r\n", "\n").trim();
        String[] lines = normalized.split("\\n");
        List<String> kept = new ArrayList<>();
        boolean skippingList = false;
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (isListHeader(lower)) {
                skippingList = true;
                continue;
            }
            if (lower.startsWith("exits:")) {
                skippingList = false;
                continue;
            }
            if (skippingList) {
                if (trimmed.startsWith("-")) {
                    continue;
                }
                skippingList = false;
            }
            if (trimmed.startsWith("#")) {
                trimmed = stripHeadingPrefix(trimmed);
                if (trimmed.isEmpty()) {
                    continue;
                }
            }
            kept.add(trimmed);
        }
        return String.join(" ", kept).trim();
    }

    private String sanitizeNarrationForMemory(String narration) {
        if (narration == null || narration.isBlank()) {
            return "";
        }
        String normalized = narration.replace("\r\n", "\n").trim();
        String[] lines = normalized.split("\\n");
        List<String> kept = new ArrayList<>();
        boolean strippedLocation = false;
        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.toLowerCase(Locale.ROOT).startsWith("exits:")) {
                continue;
            }
            if (!strippedLocation) {
                String stripped = stripLocationSentence(trimmed);
                if (!stripped.equals(trimmed)) {
                    strippedLocation = true;
                    trimmed = stripped;
                    if (trimmed.isEmpty()) {
                        continue;
                    }
                }
            }
            kept.add(trimmed);
        }
        return String.join(" ", kept).trim();
    }

    private String stripLocationSentence(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String trimmed = text.trim();
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (!lower.startsWith("you are in")) {
            return trimmed;
        }
        int period = trimmed.indexOf('.');
        if (period == -1) {
            return "";
        }
        return trimmed.substring(period + 1).trim();
    }

    private boolean isListHeader(String lower) {
        return lower.startsWith("items:")
                || lower.startsWith("fixtures:")
                || lower.startsWith("you see:");
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
}

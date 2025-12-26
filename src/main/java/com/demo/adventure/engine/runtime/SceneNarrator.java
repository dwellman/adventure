package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.NarrationService;

import java.util.Locale;

public final class SceneNarrator {
    private final NarrationService narrationService;
    private String lastState = "";
    private String lastSceneHeader = "";
    private String lastCommand = "";
    private String lastUtterance = "";
    private String backstory = "";

    public SceneNarrator(NarrationService narrationService) {
        this.narrationService = narrationService;
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
    }

    public void updateScene(String rawScene) {
        lastState = rawScene == null ? "" : rawScene.trim();
        lastSceneHeader = sceneHeaderFromSnapshot(lastState);
    }

    public void narrate(String text) {
        String snapshot = selectSceneSnapshot(text);
        narrationService.narrateEngine(text, snapshot, lastUtterance, lastCommand, backstory);
    }

    public void narrateColor(String colorEvent) {
        narrationService.narrateSnapshot(lastState, colorEvent, lastUtterance, lastCommand, backstory);
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
                location = lines[i].trim();
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
}

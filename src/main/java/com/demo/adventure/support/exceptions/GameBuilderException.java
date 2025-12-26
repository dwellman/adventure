package com.demo.adventure.support.exceptions;

import com.demo.adventure.authoring.save.build.WorldBuildReport;

public class GameBuilderException extends GameException {
    private final WorldBuildReport report;

    public GameBuilderException(String message) {
        super(message);
        this.report = null;
    }

    public GameBuilderException(String message, WorldBuildReport report) {
        super(message);
        this.report = report;
    }

    public WorldBuildReport getReport() {
        return report;
    }
}

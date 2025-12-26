package com.demo.adventure.engine.flow.loop;

import com.demo.adventure.authoring.save.build.WorldBuildResult;

public record LoopResetResult(WorldBuildResult world, LoopResetReason reason, int loopCount, String message) {
}

package com.demo.adventure.authoring.zone;

import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.List;
import java.util.Objects;

/**
 * Convenience to turn a generated zone into a GameSave for YAML emission.
 */
public final class ZoneSaveCompiler {
    private ZoneSaveCompiler() {
    }

    public static GameSave toGameSave(ZoneBuildResult zone, String preamble) {
        Objects.requireNonNull(zone, "zone");
        WorldRecipe recipe = zone.recipe();
        return new GameSave(
                recipe.seed(),
                recipe.startPlotId(),
                preamble == null ? "" : preamble,
                recipe.plots(),
                recipe.gates(),
                recipe.fixtures(),
                List.of(),
                List.of()
        );
    }
}

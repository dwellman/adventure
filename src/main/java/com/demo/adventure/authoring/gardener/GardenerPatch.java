package com.demo.adventure.authoring.gardener;

import java.util.Map;
import java.util.UUID;

/**
 * Patch describing narration overrides keyed by stable IDs.
 */
public record GardenerPatch(
        Metadata metadata,
        Map<UUID, PlotPatch> plots,
        Map<UUID, ThingPatch> things
) {
    // Pattern: Learning
    // - Records model/prompt metadata and world fingerprint to trace how patches were produced.
    public record Metadata(
            long seed,
            String worldFingerprint,
            String modelId,
            String promptVersion
    ) {
    }

    public record PlotPatch(
            String displayTitle,
            String description
    ) {
    }

    public record ThingPatch(
            String displayName,
            String description
    ) {
    }
}

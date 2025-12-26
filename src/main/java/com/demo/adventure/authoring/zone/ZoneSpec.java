package com.demo.adventure.authoring.zone;

import java.util.List;
import java.util.Objects;

/**
 * Inputs for the zone-first graph generator.
 */
public record ZoneSpec(
        String id,
        String region,
        int targetPlotCount,
        MappingDifficulty difficulty,
        PacingProfile pacing,
        TopologyBias topology,
        List<AnchorSpec> anchors
) {
    public ZoneSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(difficulty, "difficulty");
        Objects.requireNonNull(pacing, "pacing");
        Objects.requireNonNull(topology, "topology");
        anchors = List.copyOf(Objects.requireNonNullElse(anchors, List.of()));
        if (anchors.stream().noneMatch(a -> a.role() == AnchorRole.ENTRY)) {
            throw new IllegalArgumentException("At least one ENTRY anchor is required");
        }
        if (anchors.stream().noneMatch(a -> a.role() == AnchorRole.EXIT)) {
            throw new IllegalArgumentException("At least one EXIT anchor is required");
        }
        if (targetPlotCount < anchors.size()) {
            throw new IllegalArgumentException("targetPlotCount must be >= anchor count");
        }
    }
}

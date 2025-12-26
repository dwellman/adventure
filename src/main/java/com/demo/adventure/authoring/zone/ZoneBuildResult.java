package com.demo.adventure.authoring.zone;

import com.demo.adventure.domain.save.WorldRecipe;

/**
 * Output from the zone graph generator plus a few metrics for validation/debugging.
 */
public record ZoneBuildResult(
        WorldRecipe recipe,
        Metrics metrics,
        java.util.Map<String, java.util.UUID> anchorPlotIds,
        java.util.Map<AnchorRole, java.util.List<java.util.UUID>> anchorRolePlotIds
) {
    public record Metrics(
            int plotCount,
            int gateCount,
            int loopCount,
            double averageDegree,
            int longestPathLength
    ) {
    }
}

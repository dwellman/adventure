package com.demo.adventure.authoring.gardener;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.WorldBuildReport;

import java.util.List;
import java.util.UUID;

/**
 * Result of the Gardener pipeline: deterministic checks plus optional description patches.
 *
 * @param startPlotId         starting plot id
 * @param seed                world seed
 * @param registry            populated registry
 * @param report              combined build and gardener report
 * @param descriptionPatches  description updates applied to fixtures
 */
public record GardenResult(
        UUID startPlotId,
        long seed,
        KernelRegistry registry,
        WorldBuildReport report,
        List<GardenerDescriptionPatch> descriptionPatches
) {
    public GardenResult {
        report = report == null ? new WorldBuildReport() : report;
        descriptionPatches = List.copyOf(descriptionPatches == null ? List.of() : descriptionPatches);
    }
}

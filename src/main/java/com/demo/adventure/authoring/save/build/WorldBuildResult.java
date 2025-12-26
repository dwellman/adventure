package com.demo.adventure.authoring.save.build;

import com.demo.adventure.domain.kernel.KernelRegistry;

import java.util.UUID;

/**
 * Immutable result of assembling a world.
 *
 * @param startPlotId starting plot id for the world
 * @param seed        seed used to generate deterministic ids/placements
 * @param registry    populated registry
 * @param report      validation/build report
 */
public record WorldBuildResult(UUID startPlotId, long seed, KernelRegistry registry, WorldBuildReport report) {
}

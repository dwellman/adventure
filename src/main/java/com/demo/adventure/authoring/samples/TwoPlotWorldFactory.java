package com.demo.adventure.authoring.samples;

import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.save.build.WorldBuilder;


/**
 * Seeds a minimal two-plot world: a center plot and a north plot connected via a gate.
 */
public final class TwoPlotWorldFactory {
    private TwoPlotWorldFactory() {
    }

    /**
     * Seed a simple two-plot world into the given registry for testing.
     *
     * @param registry registry to populate
     * @throws GameBuilderException when plots or gate cannot be created
     */
    public static void build(KernelRegistry registry) throws GameBuilderException {
        WorldBuilder builder = new WorldBuilder(registry);

        Plot center = new PlotBuilder()
                .withLabel("Center")
                .withDescription("Center world plot")
                .withPlotRole("CENTER")
                .build();
        Plot north = new PlotBuilder()
                .withLabel("North")
                .withDescription("Northern plot")
                .withPlotRole("NORTH")
                .build();

        builder.addPlot(center);
        builder.addPlot(north);
        builder.connectPlots(center.getId(), Direction.N, north.getId(), "Center-North Gate", "Connects center to north");
    }
}

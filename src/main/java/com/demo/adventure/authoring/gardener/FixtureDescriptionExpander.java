package com.demo.adventure.authoring.gardener;

import com.demo.adventure.domain.kernel.KernelRegistry;

import java.util.List;

/**
 * Strategy for generating updated descriptions for fixtures.
 */
@FunctionalInterface
public interface FixtureDescriptionExpander {
    /**
     * Expand fixture descriptions inside the provided registry and return patches.
     *
     * @param registry populated registry to mutate
     * @return description patches applied
     */
    List<GardenerDescriptionPatch> expand(KernelRegistry registry);
}

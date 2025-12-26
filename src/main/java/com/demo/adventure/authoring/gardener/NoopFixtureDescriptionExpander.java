package com.demo.adventure.authoring.gardener;

import com.demo.adventure.domain.kernel.KernelRegistry;

import java.util.List;

/**
 * No-op fixture description expander (explicitly avoids heuristic fallback text).
 */
public final class NoopFixtureDescriptionExpander implements FixtureDescriptionExpander {
    @Override
    public List<GardenerDescriptionPatch> expand(KernelRegistry registry) {
        return List.of();
    }
}

package com.demo.adventure.authoring.gardener.ai;

import com.demo.adventure.authoring.gardener.GardenerDescriptionPatch;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AiFixtureDescriptionExpanderTest {

    @Test
    void fallsBackWhenChatClientMissing() {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Room")
                .withDescription("A quiet room")
                .withPlotRole("ROOM")
                .build();
        Item fixture = new ItemBuilder()
                .withLabel("Chair")
                .withDescription("A wooden chair")
                .withOwnerId(plot)
                .build();
        fixture.setFixture(true);
        registry.register(plot);
        registry.register(fixture);

        AiFixtureDescriptionExpander expander = new AiFixtureDescriptionExpander(null);

        List<GardenerDescriptionPatch> patches = expander.expand(registry);

        assertThat(patches).isEmpty();
        assertThat(fixture.getDescription()).isEqualTo("A wooden chair");
    }

    @Test
    void applyResponseUpdatesFixtureDescription() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Room")
                .withDescription("A quiet room")
                .withPlotRole("ROOM")
                .build();
        UUID fixtureId = UUID.randomUUID();
        Item fixture = new ItemBuilder()
                .withId(fixtureId)
                .withLabel("Chair")
                .withDescription("A wooden chair")
                .withOwnerId(plot)
                .build();
        fixture.setFixture(true);
        registry.register(plot);
        registry.register(fixture);

        AiFixtureDescriptionExpander expander = new AiFixtureDescriptionExpander(null);
        String response = "- thingId: " + fixtureId + "\n  description: New chair description";

        @SuppressWarnings("unchecked")
        List<GardenerDescriptionPatch> patches = (List<GardenerDescriptionPatch>) invoke(expander, "applyResponse",
                new Class<?>[]{String.class, KernelRegistry.class}, response, registry);

        assertThat(patches).hasSize(1);
        assertThat(fixture.getDescription()).isEqualTo("New chair description");
        assertThat(fixture.getDescriptionHistory()).isNotEmpty();
    }

    @Test
    void applyResponseIgnoresInvalidEntries() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        AiFixtureDescriptionExpander expander = new AiFixtureDescriptionExpander(null);

        @SuppressWarnings("unchecked")
        List<GardenerDescriptionPatch> patches = (List<GardenerDescriptionPatch>) invoke(expander, "applyResponse",
                new Class<?>[]{String.class, KernelRegistry.class}, "{}", registry);

        assertThat(patches).isEmpty();
    }

    private Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = AiFixtureDescriptionExpander.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}

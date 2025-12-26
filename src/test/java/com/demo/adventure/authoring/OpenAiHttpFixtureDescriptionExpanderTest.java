package com.demo.adventure.authoring;

import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.GateBuilder;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.gardener.GardenerDescriptionPatch;
import com.demo.adventure.authoring.gardener.ai.OpenAiHttpFixtureDescriptionExpander;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAiHttpFixtureDescriptionExpanderTest {

    @Test
    void returnsNoChangesWhenKeyMissing() {
        String originalProp = System.getProperty("openai.key.path");
        System.setProperty("openai.key.path", "src/test/resources/does-not-exist.key");
        try {
            KernelRegistry registry = new KernelRegistry();
            Plot plot = new PlotBuilder()
                    .withLabel("Lab")
                    .withDescription("A quiet lab.")
                    .withPlotRole("LAB")
                    .build();
            Item fixture = new ItemBuilder()
                    .withLabel("Console")
                    .withDescription("A dusty console.")
                    .withOwnerId(plot)
                    .build();
            fixture.setFixture(true);
            registry.register(plot);
            registry.register(fixture);

            OpenAiHttpFixtureDescriptionExpander expander = new OpenAiHttpFixtureDescriptionExpander();

            List<GardenerDescriptionPatch> patches = expander.expand(registry);

            assertThat(patches).isEmpty();
            assertThat(fixture.getDescription()).isEqualTo("A dusty console.");
            assertThat(fixture.getDescriptionHistory()).isEmpty();
        } finally {
            if (originalProp == null) {
                System.clearProperty("openai.key.path");
            } else {
                System.setProperty("openai.key.path", originalProp);
            }
        }
    }

    @Test
    void parseContentStripsCodeFence() throws Exception {
        String fenced = "```json\\n- thingId: 123\\n  description: hello\\n```";
        String parsed = (String) invokeStatic("parseContent", new Class<?>[]{Object.class}, fenced);

        assertThat(parsed).contains("thingId");
    }

    @Test
    void applyResponseUpdatesGateDescription() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot from = new PlotBuilder()
                .withLabel("From")
                .withDescription("From plot")
                .withPlotRole("FROM")
                .build();
        Plot to = new PlotBuilder()
                .withLabel("To")
                .withDescription("To plot")
                .withPlotRole("TO")
                .build();
        Gate gate = new GateBuilder()
                .withLabel("From -> To")
                .withDescription("Old path")
                .withPlotA(from)
                .withPlotB(to)
                .withDirection(Direction.E)
                .build();
        registry.register(from);
        registry.register(to);
        registry.register(gate);

        OpenAiHttpFixtureDescriptionExpander expander = new OpenAiHttpFixtureDescriptionExpander();

        @SuppressWarnings("unchecked")
        Map<UUID, Plot> plots = (Map<UUID, Plot>) invokeStatic("plotsById", new Class<?>[]{KernelRegistry.class}, registry);
        @SuppressWarnings("unchecked")
        List<Object> targets = (List<Object>) invokeStatic("collectTargets", new Class<?>[]{KernelRegistry.class, Map.class}, registry, plots);

        Object gateTarget = null;
        for (Object target : targets) {
            if ("GATE".equals(invokeTarget(target, "type"))) {
                gateTarget = target;
                break;
            }
        }
        assertThat(gateTarget).isNotNull();

        UUID gateId = (UUID) invokeTarget(gateTarget, "id");
        String response = String.join("\n",
                "choices:",
                "  - message:",
                "      content: |",
                "        - thingId: " + gateId,
                "          description: Crossing line"
        );

        @SuppressWarnings("unchecked")
        List<GardenerDescriptionPatch> patches = (List<GardenerDescriptionPatch>) invokeInstance(
                expander,
                "applyResponseForTarget",
                new Class<?>[]{gateTarget.getClass(), String.class, KernelRegistry.class},
                gateTarget,
                response,
                registry
        );

        UUID fromPlotId = (UUID) invokeTarget(gateTarget, "fromPlotId");
        assertThat(patches).hasSize(1);
        assertThat(gate.getDescriptionFrom(fromPlotId)).contains("Crossing line").contains("to:");
    }

    @Test
    void collectTargetsIncludesFixturesAndItems() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Start")
                .withDescription("Start plot")
                .withPlotRole("START")
                .build();
        Item fixture = new ItemBuilder()
                .withLabel("Lamp")
                .withDescription("A brass lamp")
                .withOwnerId(plot)
                .build();
        fixture.setFixture(true);
        Item item = new ItemBuilder()
                .withLabel("Coin")
                .withDescription("An old coin")
                .withOwnerId(plot)
                .build();
        registry.register(plot);
        registry.register(fixture);
        registry.register(item);

        @SuppressWarnings("unchecked")
        Map<UUID, Plot> plots = (Map<UUID, Plot>) invokeStatic("plotsById", new Class<?>[]{KernelRegistry.class}, registry);
        @SuppressWarnings("unchecked")
        List<Object> targets = (List<Object>) invokeStatic("collectTargets", new Class<?>[]{KernelRegistry.class, Map.class}, registry, plots);

        List<String> types = new ArrayList<>();
        for (Object target : targets) {
            types.add((String) invokeTarget(target, "type"));
        }

        assertThat(types).contains("FIXTURE", "ITEM");
    }

    @Test
    void resolveTypeAndOwningPlotIdHandleNestedItems() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Hub")
                .withDescription("Hub plot")
                .withPlotRole("HUB")
                .build();
        Item container = new ItemBuilder()
                .withLabel("Crate")
                .withDescription("A crate")
                .withOwnerId(plot)
                .build();
        container.setFixture(true);
        Item nested = new ItemBuilder()
                .withLabel("Note")
                .withDescription("A note")
                .withOwnerId(container)
                .build();
        registry.register(plot);
        registry.register(container);
        registry.register(nested);

        String plotType = (String) invokeStatic("resolveType", new Class<?>[]{com.demo.adventure.domain.model.Thing.class}, plot);
        String fixtureType = (String) invokeStatic("resolveType", new Class<?>[]{com.demo.adventure.domain.model.Thing.class}, container);
        String itemType = (String) invokeStatic("resolveType", new Class<?>[]{com.demo.adventure.domain.model.Thing.class}, nested);
        UUID owningPlotId = (UUID) invokeStatic(
                "owningPlotId",
                new Class<?>[]{com.demo.adventure.domain.model.Thing.class, KernelRegistry.class, Map.class},
                nested,
                registry,
                Map.of(plot.getId(), plot)
        );

        assertThat(plotType).isEqualTo("PLOT");
        assertThat(fixtureType).isEqualTo("FIXTURE");
        assertThat(itemType).isEqualTo("ITEM");
        assertThat(owningPlotId).isEqualTo(plot.getId());
    }

    @Test
    void contextSummariesSkipsTargetItem() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot plot = new PlotBuilder()
                .withLabel("Square")
                .withDescription("Town square")
                .withPlotRole("SQUARE")
                .build();
        Item target = new ItemBuilder()
                .withLabel("Statue")
                .withDescription("A tall statue")
                .withOwnerId(plot)
                .build();
        Item other = new ItemBuilder()
                .withLabel("Bench")
                .withDescription("A stone bench")
                .withOwnerId(plot)
                .build();
        registry.register(plot);
        registry.register(target);
        registry.register(other);

        @SuppressWarnings("unchecked")
        List<String> summaries = (List<String>) invokeStatic(
                "contextSummaries",
                new Class<?>[]{KernelRegistry.class, Map.class, UUID.class, UUID.class},
                registry,
                Map.of(plot.getId(), plot),
                plot.getId(),
                target.getId()
        );

        assertThat(summaries).anyMatch(s -> s.contains("Bench"));
        assertThat(summaries).noneMatch(s -> s.contains("Statue"));
    }

    @Test
    void buildUserPromptIncludesDirectionForGate() throws Exception {
        KernelRegistry registry = new KernelRegistry();
        Plot from = new PlotBuilder()
                .withLabel("Left")
                .withDescription("Left plot")
                .withPlotRole("LEFT")
                .build();
        Plot to = new PlotBuilder()
                .withLabel("Right")
                .withDescription("Right plot")
                .withPlotRole("RIGHT")
                .build();
        Gate gate = new GateBuilder()
                .withLabel("Left -> Right")
                .withDescription("A gate")
                .withPlotA(from)
                .withPlotB(to)
                .withDirection(Direction.E)
                .build();
        registry.register(from);
        registry.register(to);
        registry.register(gate);

        @SuppressWarnings("unchecked")
        Map<UUID, Plot> plots = (Map<UUID, Plot>) invokeStatic("plotsById", new Class<?>[]{KernelRegistry.class}, registry);
        @SuppressWarnings("unchecked")
        List<Object> targets = (List<Object>) invokeStatic("collectTargets", new Class<?>[]{KernelRegistry.class, Map.class}, registry, plots);
        Object gateTarget = null;
        for (Object target : targets) {
            if ("GATE".equals(invokeTarget(target, "type"))) {
                gateTarget = target;
                break;
            }
        }
        assertThat(gateTarget).isNotNull();

        OpenAiHttpFixtureDescriptionExpander expander = new OpenAiHttpFixtureDescriptionExpander();
        String prompt = (String) invokeInstance(expander, "buildUserPrompt", new Class<?>[]{gateTarget.getClass()}, gateTarget);

        assertThat(prompt).contains("direction:");
        assertThat(prompt).contains("toPlotName:");
    }

    @Test
    void resolveApiKeyUsesOverrideFile() throws Exception {
        Path temp = Files.createTempFile("openai-key", ".txt");
        Files.writeString(temp, "key-123");
        String originalProp = System.getProperty("openai.key.path");
        System.setProperty("openai.key.path", temp.toString());
        try {
            String key = (String) invokeStatic("resolveApiKey", new Class<?>[]{});
            assertThat(key).isEqualTo("key-123");
        } finally {
            if (originalProp == null) {
                System.clearProperty("openai.key.path");
            } else {
                System.setProperty("openai.key.path", originalProp);
            }
        }
    }

    @Test
    void jsonEscapeQuotesStrings() throws Exception {
        String escaped = (String) invokeStatic("jsonEscape", new Class<?>[]{String.class}, "Hello \"world\"");
        assertThat(escaped).isEqualTo("\"Hello \\\"world\\\"\"");
    }

    @Test
    void readKeyFileHandlesOpenAiPrefix() throws Exception {
        Path temp = Files.createTempFile("openai-key", ".txt");
        Files.writeString(temp, "OPENAI_API_KEY=abc123");

        String key = (String) invokeStatic("readKeyFile", new Class<?>[]{String.class}, temp.toString());

        assertThat(key).isEqualTo("abc123");
    }

    private static Object invokeStatic(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = OpenAiHttpFixtureDescriptionExpander.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Object invokeInstance(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = OpenAiHttpFixtureDescriptionExpander.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static Object invokeTarget(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }
}

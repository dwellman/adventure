package com.demo.adventure.ai.runtime.smart;

import com.demo.adventure.ai.runtime.TranslatorService;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.ActorBuilder;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.GateBuilder;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.PlotBuilder;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.engine.command.handlers.CommandHandlers;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.engine.runtime.SmartActorRuntime;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorRuntimeTest {

    @Test
    void resolveCommandTextFallsBackForBlankUtterance() throws Exception {
        Harness harness = new Harness();
        SmartActorSpec spec = harness.specWithPolicy(new SmartActorPolicy(List.of("LOOK"), 0, 0, 0));
        SmartActorWorldSnapshot snapshot = harness.snapshot();

        String result = (String) invoke(harness.runtime, "resolveCommandText",
                new Class<?>[]{SmartActorSpec.class, SmartActorWorldSnapshot.class, String.class, boolean.class},
                spec, snapshot, " ", false);

        assertThat(result).isEqualTo("look");
    }

    @Test
    void resolveCommandTextRejectsTooLongUtterance() throws Exception {
        Harness harness = new Harness();
        SmartActorSpec spec = harness.specWithPolicy(new SmartActorPolicy(List.of("LOOK"), 3, 0, 0));
        SmartActorWorldSnapshot snapshot = harness.snapshot();

        String result = (String) invoke(harness.runtime, "resolveCommandText",
                new Class<?>[]{SmartActorSpec.class, SmartActorWorldSnapshot.class, String.class, boolean.class},
                spec, snapshot, "look around", false);

        assertThat(result).isEqualTo("look");
    }

    @Test
    void resolveCommandTextFallsBackWhenVerbNotAllowed() throws Exception {
        Harness harness = new Harness();
        SmartActorSpec spec = harness.specWithPolicy(new SmartActorPolicy(List.of("TAKE"), 0, 0, 0));
        SmartActorWorldSnapshot snapshot = harness.snapshot();

        String result = (String) invoke(harness.runtime, "resolveCommandText",
                new Class<?>[]{SmartActorSpec.class, SmartActorWorldSnapshot.class, String.class, boolean.class},
                spec, snapshot, "look", false);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveCommandTextUsesTranslationFallback() throws Exception {
        Harness harness = new Harness();
        SmartActorSpec spec = harness.specWithPolicy(new SmartActorPolicy(List.of("LOOK"), 0, 0, 0));
        SmartActorWorldSnapshot snapshot = harness.snapshot();

        String result = (String) invoke(harness.runtime, "resolveCommandText",
                new Class<?>[]{SmartActorSpec.class, SmartActorWorldSnapshot.class, String.class, boolean.class},
                spec, snapshot, "dance", false);

        assertThat(result).isEqualTo("look");
    }

    @Test
    void allowsVerbRespectsCombatOnly() throws Exception {
        Harness harness = new Harness();
        SmartActorSpec spec = harness.specWithPolicy(new SmartActorPolicy(List.of(), 0, 0, 0));

        boolean allowedLook = (boolean) invoke(harness.runtime, "allowsVerb",
                new Class<?>[]{SmartActorSpec.class, CommandAction.class, boolean.class},
                spec, CommandAction.LOOK, true);
        boolean allowedAttack = (boolean) invoke(harness.runtime, "allowsVerb",
                new Class<?>[]{SmartActorSpec.class, CommandAction.class, boolean.class},
                spec, CommandAction.ATTACK, true);

        assertThat(allowedLook).isFalse();
        assertThat(allowedAttack).isTrue();
    }

    @Test
    void eligibleUsesCooldownTurns() throws Exception {
        Harness harness = new Harness();
        SmartActorSpec spec = harness.specWithPolicy(new SmartActorPolicy(List.of(), 0, 2, 0));

        setField(harness.runtime, "turnIndex", 5);
        @SuppressWarnings("unchecked")
        Map<UUID, Integer> lastActionTurn = (Map<UUID, Integer>) getField(harness.runtime, "lastActionTurn");
        lastActionTurn.put(harness.actorId, 4);

        boolean eligible = (boolean) invoke(harness.runtime, "eligible",
                new Class<?>[]{UUID.class, SmartActorSpec.class},
                harness.actorId, spec);

        assertThat(eligible).isFalse();
    }

    @Test
    void trimLinesRespectsLimit() throws Exception {
        Harness harness = new Harness();

        String trimmed = (String) invoke(harness.runtime, "trimLines",
                new Class<?>[]{String.class, int.class},
                "one\n\n two\nthree", 2);

        assertThat(trimmed).isEqualTo("one\ntwo");
    }

    @Test
    void exitsForCollectsVisibleGateDirections() throws Exception {
        Harness harness = new Harness();
        List<String> exits = (List<String>) invoke(harness.runtime, "exitsFor",
                new Class<?>[]{KernelRegistry.class, UUID.class},
                harness.registry, harness.plot.getId());

        assertThat(exits).contains("EAST");
    }

    @Test
    void recentReceiptsUsesTailWindow() throws Exception {
        Harness harness = new Harness();
        harness.registry.recordReceipt("one");
        harness.registry.recordReceipt("two");

        List<String> receipts = (List<String>) invoke(harness.runtime, "recentReceipts",
                new Class<?>[]{KernelRegistry.class},
                harness.registry);

        assertThat(receipts).contains("one", "two");
    }

    private Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static class Harness {
        private final KernelRegistry registry = new KernelRegistry();
        private final Plot plot;
        private final UUID actorId;
        private final SmartActorRuntime runtime;

        private Harness() {
            plot = new PlotBuilder()
                    .withLabel("Hall")
                    .withDescription("A long hall")
                    .withPlotRole("HALL")
                    .build();
            registry.register(plot);
            actorId = UUID.nameUUIDFromBytes(("actor:butler").getBytes(StandardCharsets.UTF_8));
            Actor actor = new ActorBuilder()
                    .withId(actorId)
                    .withLabel("Butler")
                    .withDescription("A quiet butler")
                    .withOwnerId(plot)
                    .build();
            registry.register(actor);
            registry.register(new GateBuilder()
                    .withLabel("Hall -> East")
                    .withDescription("A doorway")
                    .withPlotA(plot)
                    .withPlotB(new PlotBuilder().withLabel("East").withDescription("East room").withPlotRole("EAST").build())
                    .withDirection(Direction.E)
                    .build());

            SmartActorSpec spec = specWithPolicy(new SmartActorPolicy(List.of("LOOK"), 0, 0, 0));
            SmartActorRegistry smartRegistry = SmartActorRegistry.create(registry, List.of(spec));
            SmartActorPlanner planner = new SmartActorPlanner(false, "", false);
            TranslatorService translatorService = new TranslatorService(false, "");
            runtime = new SmartActorRuntime(
                    smartRegistry,
                    SmartActorTagIndex.empty(),
                    planner,
                    translatorService,
                    new CommandInterpreter(),
                    CommandHandlers.defaultHandlers(),
                    false
            );
        }

        private SmartActorSpec specWithPolicy(SmartActorPolicy policy) {
            return new SmartActorSpec(
                    "butler",
                    "smart-actor-system",
                    "",
                    Map.of(),
                    Map.of(),
                    List.of(),
                    null,
                    policy
            );
        }

        private SmartActorWorldSnapshot snapshot() {
            return new SmartActorWorldSnapshot(
                    "Butler",
                    "A quiet butler",
                    "Hall",
                    "A long hall",
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of(),
                    List.of("EAST"),
                    "",
                    List.of()
            );
        }
    }
}

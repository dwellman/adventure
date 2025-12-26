package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.*;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MovementGateTest {

    private KernelRegistry registry;
    private Plot start;
    private Plot lockedPlot;
    private Plot openPlot;
    private Gate lockedGate;
    private Gate openGate;
    private Actor actor;

    @BeforeEach
    void setUp() {
        registry = new KernelRegistry();

        start = new PlotBuilder()
                .withId(uuid("plot:start"))
                .withLabel("Start")
                .withDescription("Start plot")
                .withPlotRole("REGION")
                .withRegion("REGION")
                .withLocationX(0)
                .withLocationY(0)
                .build();
        lockedPlot = new PlotBuilder()
                .withId(uuid("plot:locked"))
                .withLabel("Locked")
                .withDescription("Locked plot")
                .withPlotRole("REGION")
                .withRegion("REGION")
                .withLocationX(1)
                .withLocationY(0)
                .build();
        openPlot = new PlotBuilder()
                .withId(uuid("plot:open"))
                .withLabel("Open")
                .withDescription("Open plot")
                .withPlotRole("REGION")
                .withRegion("REGION")
                .withLocationX(0)
                .withLocationY(1)
                .build();

        lockedGate = new GateBuilder()
                .withId(uuid("gate:locked"))
                .withLabel("Locked Gate")
                .withDescription("A locked gate")
                .withPlotA(start)
                .withPlotB(lockedPlot)
                .withDirection(Direction.E)
                .withVisible(true)
                .withKeyString("HAS(\"Key\")")
                .build();
        openGate = new GateBuilder()
                .withId(uuid("gate:open"))
                .withLabel("Open Gate")
                .withDescription("An open gate")
                .withPlotA(start)
                .withPlotB(openPlot)
                .withDirection(Direction.S)
                .withVisible(true)
                .withKeyString("true")
                .build();

        actor = new ActorBuilder()
                .withId(uuid("actor:player"))
                .withLabel("Player")
                .withDescription("Tester")
                .withOwnerId(start)
                .build();

        registry.register(start);
        registry.register(lockedPlot);
        registry.register(openPlot);
        registry.register(lockedGate);
        registry.register(openGate);
        registry.register(actor);
    }

    @Test
    void moveSucceedsThroughOpenGate() {
        UUID next = moveActor(Direction.S);
        assertThat(next).isEqualTo(openPlot.getId());
        assertThat(actor.getOwnerId()).isEqualTo(openPlot.getId());
    }

    @Test
    void moveFailsThroughLockedGateWithoutKey() {
        UUID next = moveActor(Direction.E);
        assertThat(next).isNull();
        assertThat(actor.getOwnerId()).isEqualTo(start.getId());
    }

    @Test
    void moveSucceedsThroughLockedGateWithKey() {
        // Give actor a Key item to satisfy HAS("Key")
        var key = new ItemBuilder()
                .withId(uuid("item:key"))
                .withLabel("Key")
                .withDescription("Opens things")
                .withOwnerId(actor)
                .build();
        key.setVisible(true);
        registry.register(key);

        UUID next = moveActor(Direction.E);
        assertThat(next).isEqualTo(lockedPlot.getId());
        assertThat(actor.getOwnerId()).isEqualTo(lockedPlot.getId());
    }

    @Test
    void moveFailsWhenGateHiddenOrInvisible() {
        openGate.setVisible(false);
        UUID next = moveActor(Direction.S);
        assertThat(next).isNull();
        assertThat(actor.getOwnerId()).isEqualTo(start.getId());
    }

    private UUID moveActor(Direction direction) {
        return registry.findGates(actor.getOwnerId(), direction).stream()
                .filter(g -> g.isVisible())
                .filter(g -> KeyExpressionEvaluator.evaluate(
                        g.getKeyString(),
                        KeyExpressionEvaluator.registryHasResolver(registry, actor.getId())
                ))
                .findFirst()
                .map(g -> {
                    UUID dest = g.otherSide(actor.getOwnerId());
                    if (dest != null) {
                        registry.moveOwnership(actor.getId(), dest);
                    }
                    return dest;
                })
                .orElse(null);
    }

    private static UUID uuid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}

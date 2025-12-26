package com.demo.adventure.domain.model;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.domain.kernel.KernelRegistry;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Kernel-level world state carrier (clock/loop counters and global flags).
 */
public final class WorldState extends Thing {
    public static final String LABEL = "World";
    public static final String CLOCK_CELL = "CLOCK";
    public static final String LOOP_CELL = "LOOP";
    public static final String TICK_RATE_CELL = "TICK_RATE";
    public static final String CAVE_WEB_BURNED_CELL = "CAVE_WEB_BURNED";
    public static final String CAVE_RETURN_CELL = "CAVE_RETURN";
    public static final String PLANE_WRECK_TURNS_CELL = "PLANE_WRECK_TURNS";
    public static final String SCRATCH_FREED_CELL = "SCRATCH_FREED";
    public static final String MONKEYS_FED_CELL = "MONKEYS_FED";

    private static final UUID WORLD_STATE_ID =
            UUID.nameUUIDFromBytes("world-state".getBytes(StandardCharsets.UTF_8));

    public WorldState(long clockCapacity) {
        super(WORLD_STATE_ID, ThingKind.WORLD, LABEL, "World state", KernelRegistry.MILIARIUM);
        setVisible(false);
        setKey("true");
        setCell(CLOCK_CELL, new Cell(Math.max(1, clockCapacity), 0L));
        setCell(LOOP_CELL, new Cell(9999L, 0L));
        setCell(TICK_RATE_CELL, new Cell(60L, 10L));
        setCell(CAVE_WEB_BURNED_CELL, new Cell(1L, 0L));
        setCell(CAVE_RETURN_CELL, new Cell(10L, 0L));
        setCell(PLANE_WRECK_TURNS_CELL, new Cell(20L, 0L));
        setCell(SCRATCH_FREED_CELL, new Cell(1L, 0L));
        setCell(MONKEYS_FED_CELL, new Cell(1L, 0L));
    }
}

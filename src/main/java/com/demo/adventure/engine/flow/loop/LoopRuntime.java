package com.demo.adventure.engine.flow.loop;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.WorldState;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.io.FootprintRule;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.domain.save.GameSave;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Loop runtime controller: advances clock, persists notebook memory, and rebuilds worlds on reset.
 */
public final class LoopRuntime {
    private final GameSave save;
    private final LoopConfig config;
    private final LoopState state;
    private final GameSaveAssembler assembler = new GameSaveAssembler();
    private final List<FootprintRule> footprintRules;
    private final Map<String, PersistentItemSnapshot> persistentItems = new HashMap<>();
    private WorldState worldState;

    public LoopRuntime(GameSave save, LoopConfig config) {
        this(save, config, List.of());
    }

    public LoopRuntime(GameSave save, LoopConfig config, List<FootprintRule> footprintRules) {
        if (save == null) {
            throw new IllegalArgumentException("save is required");
        }
        this.save = save;
        this.config = config == null ? LoopConfig.disabled() : config;
        this.state = new LoopState(this.config.maxTicks());
        this.footprintRules = footprintRules == null ? List.of() : footprintRules;
    }

    public LoopConfig config() {
        return config;
    }

    public LoopState state() {
        return state;
    }

    public boolean enabled() {
        return config.enabled();
    }

    public WorldBuildResult buildWorld() throws GameBuilderException {
        WorldBuildResult world = assembler.apply(save, footprintRules);
        if (enabled()) {
            installWorldState(world.registry());
        }
        return world;
    }

    public LoopResetReason advanceTurn(KernelRegistry registry) {
        if (!enabled()) {
            return null;
        }
        syncTickRateFromWorldState();
        LoopResetReason reason = state.advance();
        updateWorldStateCells(registry);
        return reason;
    }

    public LoopResetResult reset(KernelRegistry previous, LoopResetReason reason) throws GameBuilderException {
        if (!enabled()) {
            WorldBuildResult world = buildWorld();
            return new LoopResetResult(world, reason, state.loopCount(), "");
        }
        syncTickRateFromWorldState();
        capturePersistentItems(previous);
        state.reset();
        WorldBuildResult world = buildWorld();
        applyPersistentItems(world.registry());
        updateWorldStateCells(world.registry());
        return new LoopResetResult(world, reason, state.loopCount(), buildResetMessage(reason));
    }

    private void installWorldState(KernelRegistry registry) {
        worldState = new WorldState(state.maxTicks());
        if (registry != null) {
            registry.register(worldState);
        }
        updateWorldStateCells(registry);
    }

    private void updateWorldStateCells(KernelRegistry registry) {
        if (registry == null || worldState == null) {
            return;
        }
        recordMutation(registry, CellOps.setAmount(worldState, WorldState.CLOCK_CELL, state.clock()));
        recordMutation(registry, CellOps.setAmount(worldState, WorldState.LOOP_CELL, state.loopCount()));
        recordMutation(registry, CellOps.setAmount(worldState, WorldState.TICK_RATE_CELL, state.tickRate()));
    }

    private void syncTickRateFromWorldState() {
        if (worldState == null) {
            return;
        }
        Cell cell = worldState.getCell(WorldState.TICK_RATE_CELL);
        if (cell == null) {
            return;
        }
        long amount = cell.getAmount();
        if (amount <= 0) {
            return;
        }
        if (amount > Integer.MAX_VALUE) {
            state.setTickRate(Integer.MAX_VALUE);
        } else {
            state.setTickRate((int) amount);
        }
    }

    private void recordMutation(KernelRegistry registry, CellMutationReceipt receipt) {
        if (registry != null && receipt != null) {
            registry.recordCellMutation(receipt);
        }
    }

    private void capturePersistentItems(KernelRegistry registry) {
        if (registry == null || config.persistentItems().isEmpty()) {
            return;
        }
        for (String label : config.persistentItems()) {
            Item item = findItemByLabel(registry, label);
            if (item != null) {
                persistentItems.put(normalize(label), snapshot(item));
            }
        }
    }

    private void applyPersistentItems(KernelRegistry registry) {
        if (registry == null || persistentItems.isEmpty()) {
            return;
        }
        for (PersistentItemSnapshot snapshot : persistentItems.values()) {
            Item item = findItemByLabel(registry, snapshot.label());
            if (item == null) {
                continue;
            }
            item.setDescription(snapshot.description());
            item.setCells(copyCells(snapshot.cells()));
        }
    }

    private static PersistentItemSnapshot snapshot(Item item) {
        return new PersistentItemSnapshot(
                item.getLabel() == null ? "" : item.getLabel(),
                item.getDescription(),
                copyCells(item.getCells())
        );
    }

    private static Map<String, Cell> copyCells(Map<String, Cell> cells) {
        if (cells == null || cells.isEmpty()) {
            return Map.of();
        }
        Map<String, Cell> copy = new HashMap<>();
        for (Map.Entry<String, Cell> entry : cells.entrySet()) {
            Cell cell = entry.getValue();
            if (cell == null) {
                continue;
            }
            copy.put(entry.getKey(), new Cell(cell.getCapacity(), cell.getAmount()));
        }
        return copy;
    }

    private static Item findItemByLabel(KernelRegistry registry, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        String trimmed = label.trim();
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> item.getLabel() != null && item.getLabel().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }

    private static String normalize(String label) {
        return label == null ? "" : label.trim().toUpperCase(Locale.ROOT);
    }

    private String buildResetMessage(LoopResetReason reason) {
        if (reason == LoopResetReason.TIMEOUT) {
            return "The day resets. You remember.";
        }
        if (reason == LoopResetReason.DEATH) {
            return "You die. The day resets, but you remember.";
        }
        return "The day resets.";
    }

    private record PersistentItemSnapshot(String label, String description, Map<String, Cell> cells) {
    }
}

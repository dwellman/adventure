package com.demo.adventure.engine.flow.trigger;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionResult;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.model.ThingKind;
import com.demo.adventure.domain.model.WorldState;
import com.demo.adventure.engine.flow.loop.LoopResetReason;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class TriggerEngine {
    private final List<TriggerDefinition> triggers;

    public TriggerEngine(List<TriggerDefinition> triggers) {
        this.triggers = triggers == null ? List.of() : List.copyOf(triggers);
    }

    public TriggerOutcome fire(TriggerEvent event, TriggerContext context) {
        if (event == null || context == null || triggers.isEmpty()) {
            return TriggerOutcome.empty();
        }
        KernelRegistry registry = context.registry();
        if (registry == null) {
            return TriggerOutcome.empty();
        }
        List<String> messages = new ArrayList<>();
        LoopResetReason resetReason = null;
        String resetMessage = "";
        boolean endGame = false;
        for (TriggerDefinition trigger : triggers) {
            if (trigger == null || trigger.type() != event.type()) {
                continue;
            }
            if (!matchesLabel(trigger.target(), event.targetLabel())) {
                continue;
            }
            if (!matchesLabel(trigger.object(), event.objectLabel())) {
                continue;
            }
            if (!evaluateKey(trigger.key(), context)) {
                continue;
            }
            for (TriggerAction action : trigger.actions()) {
                if (action == null || action.type() == null) {
                    continue;
                }
                ActionOutcome outcome = applyAction(action, event, context);
                if (outcome.message() != null && !outcome.message().isBlank()) {
                    messages.add(outcome.message());
                }
                if (outcome.endGame()) {
                    endGame = true;
                }
                if (resetReason == null && outcome.resetReason() != null) {
                    resetReason = outcome.resetReason();
                    resetMessage = outcome.resetMessage();
                }
            }
        }
        if (messages.isEmpty() && resetReason == null && !endGame) {
            return TriggerOutcome.empty();
        }
        return new TriggerOutcome(messages, resetReason, resetMessage, endGame);
    }

    private boolean matchesLabel(String expected, String actual) {
        if (expected == null || expected.isBlank()) {
            return true;
        }
        if (actual == null || actual.isBlank()) {
            return false;
        }
        return expected.trim().equalsIgnoreCase(actual.trim());
    }

    private boolean evaluateKey(String key, TriggerContext context) {
        if (key == null || key.isBlank()) {
            return true;
        }
        KernelRegistry registry = context.registry();
        UUID playerId = context.playerId();
        UUID plotId = context.plotId();
        UUID worldId = context.worldId();
        KeyExpressionEvaluator.HasResolver hasResolver =
                KeyExpressionEvaluator.registryHasResolver(registry, playerId);
        KeyExpressionEvaluator.SearchResolver searchResolver =
                KeyExpressionEvaluator.registrySearchResolver(registry, plotId);
        KeyExpressionEvaluator.SkillResolver skillResolver =
                KeyExpressionEvaluator.registrySkillResolver(registry, playerId);
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, plotId, playerId, worldId);
        KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(
                key,
                hasResolver,
                searchResolver,
                skillResolver,
                attributeResolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
        return result.isSuccess() && result.value();
    }

    private ActionOutcome applyAction(TriggerAction action, TriggerEvent event, TriggerContext context) {
        return switch (action.type()) {
            case MESSAGE -> new ActionOutcome(action.text(), null, "", false);
            case REVEAL -> {
                Thing target = resolveTarget(action.target(), event, context);
                if (target != null) {
                    target.setVisible(true);
                }
                yield ActionOutcome.empty();
            }
            case HIDE -> {
                Thing target = resolveTarget(action.target(), event, context);
                if (target != null) {
                    target.setVisible(false);
                }
                yield ActionOutcome.empty();
            }
            case SET_VISIBLE -> {
                Thing target = resolveTarget(action.target(), event, context);
                if (target != null && action.visible() != null) {
                    target.setVisible(action.visible());
                }
                yield ActionOutcome.empty();
            }
            case SET_KEY -> {
                Thing target = resolveTarget(action.target(), event, context);
                if (target != null && action.key() != null && !action.key().isBlank()) {
                    target.setKey(action.key());
                }
                yield ActionOutcome.empty();
            }
            case SET_VISIBILITY_KEY -> {
                Thing target = resolveTarget(action.target(), event, context);
                if (target != null && action.visibilityKey() != null && !action.visibilityKey().isBlank()) {
                    target.setVisibilityKey(action.visibilityKey());
                }
                yield ActionOutcome.empty();
            }
            case MOVE_OWNER -> {
                Thing target = resolveTarget(action.target(), event, context);
                Thing owner = resolveOwner(action.owner(), event, context);
                if (target != null && owner != null) {
                    context.registry().moveOwnership(target.getId(), owner.getId());
                }
                yield ActionOutcome.empty();
            }
            case SET_CELL -> {
                Thing target = resolveTarget(action.target(), event, context);
                if (target != null && action.cell() != null && !action.cell().isBlank() && action.amount() != null) {
                    recordMutation(context.registry(), CellOps.setAmount(target, action.cell(), action.amount()));
                }
                yield ActionOutcome.empty();
            }
            case INCREMENT_CELL -> {
                Thing target = resolveTarget(action.target(), event, context);
                if (target != null && action.cell() != null && !action.cell().isBlank() && action.amount() != null) {
                    recordMutation(context.registry(), applyDelta(target, action.cell(), action.amount()));
                }
                yield ActionOutcome.empty();
            }
            case SET_DESCRIPTION -> {
                Thing target = resolveTarget(action.target(), event, context);
                if (target != null && action.description() != null && !action.description().isBlank()) {
                    int clock = resolveWorldClock(context.registry(), context.worldId());
                    if (clock >= 0) {
                        target.recordDescription(action.description(), clock);
                    } else {
                        target.setDescription(action.description());
                    }
                }
                yield ActionOutcome.empty();
            }
            case RESET_LOOP -> {
                LoopResetReason reason = parseResetReason(action.reason());
                String message = action.text();
                yield new ActionOutcome(null, reason, message == null ? "" : message, false);
            }
            case END_GAME -> new ActionOutcome(action.text(), null, "", true);
        };
    }

    private CellMutationReceipt applyDelta(Thing target, String cell, long delta) {
        if (delta >= 0) {
            return CellOps.replenish(target, cell, delta);
        }
        return CellOps.consume(target, cell, Math.abs(delta));
    }

    private void recordMutation(KernelRegistry registry, CellMutationReceipt receipt) {
        if (registry != null && receipt != null) {
            registry.recordCellMutation(receipt);
        }
    }

    private Thing resolveTarget(String target, TriggerEvent event, TriggerContext context) {
        if (context == null || context.registry() == null) {
            return null;
        }
        if (target == null || target.isBlank()) {
            Thing eventTarget = resolveEventTarget(event, context);
            if (eventTarget != null) {
                return eventTarget;
            }
            return findByLabel(context.registry(), event == null ? null : event.targetLabel());
        }
        String trimmed = target.trim();
        if (trimmed.startsWith("@")) {
            return resolveSpecial(trimmed, event, context);
        }
        return findByLabel(context.registry(), trimmed);
    }

    private Thing resolveOwner(String owner, TriggerEvent event, TriggerContext context) {
        if (owner == null || owner.isBlank()) {
            return null;
        }
        return resolveTarget(owner, event, context);
    }

    private Thing resolveEventTarget(TriggerEvent event, TriggerContext context) {
        if (event == null || event.targetId() == null) {
            return null;
        }
        return context.registry().get(event.targetId());
    }

    private Thing resolveEventObject(TriggerEvent event, TriggerContext context) {
        if (event == null || event.objectId() == null) {
            return null;
        }
        return context.registry().get(event.objectId());
    }

    private Thing resolveSpecial(String ref, TriggerEvent event, TriggerContext context) {
        String key = ref.trim().toUpperCase(Locale.ROOT);
        return switch (key) {
            case "@PLAYER" -> context.registry().get(context.playerId());
            case "@PLOT" -> context.registry().get(context.plotId());
            case "@WORLD" -> findWorldState(context.registry());
            case "@TARGET" -> resolveEventTarget(event, context);
            case "@OBJECT" -> resolveEventObject(event, context);
            default -> findByLabel(context.registry(), ref);
        };
    }

    private Thing findWorldState(KernelRegistry registry) {
        if (registry == null) {
            return null;
        }
        return registry.getEverything().values().stream()
                .filter(t -> t != null && t.getKind() == ThingKind.WORLD)
                .sorted(Comparator.comparing(Thing::getId))
                .findFirst()
                .orElse(null);
    }

    private Thing findByLabel(KernelRegistry registry, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        String target = label.trim();
        return registry.getEverything().values().stream()
                .filter(t -> t != null && t.getLabel() != null)
                .filter(t -> t.getLabel().equalsIgnoreCase(target))
                .sorted(Comparator.comparing(Thing::getId))
                .findFirst()
                .orElse(null);
    }

    private int resolveWorldClock(KernelRegistry registry, UUID worldId) {
        if (registry == null || worldId == null) {
            return -1;
        }
        Thing world = registry.get(worldId);
        if (world == null) {
            return -1;
        }
        Cell cell = world.getCell(WorldState.CLOCK_CELL);
        if (cell == null) {
            return -1;
        }
        long amount = cell.getAmount();
        if (amount < 0) {
            return -1;
        }
        if (amount > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) amount;
    }

    private LoopResetReason parseResetReason(String raw) {
        if (raw == null || raw.isBlank()) {
            return LoopResetReason.MANUAL;
        }
        try {
            return LoopResetReason.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return LoopResetReason.MANUAL;
        }
    }

    private record ActionOutcome(String message, LoopResetReason resetReason, String resetMessage, boolean endGame) {
        private static ActionOutcome empty() {
            return new ActionOutcome(null, null, "", false);
        }
    }
}

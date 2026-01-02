package com.demo.adventure.engine.mechanics.keyexpr;

import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellReferenceReceipt;
import com.demo.adventure.engine.mechanics.cells.CellReferenceStatus;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.AttributeResolutionContext;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.AttributeResolutionPolicy;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.AttributeResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.HasResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SearchResolver;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SkillResolver;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.support.exceptions.KeyExpressionEvaluationException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

final class KeyExpressionRegistryResolvers {
    private KeyExpressionRegistryResolvers() {
    }

    static HasResolver registryHasResolver(KernelRegistry registry, UUID ownerId) {
        return label -> registryContains(registry, ownerId, label);
    }

    static SearchResolver registrySearchResolver(KernelRegistry registry, UUID ownerId) {
        return label -> registryContains(registry, ownerId, label);
    }

    static SkillResolver registrySkillResolver(KernelRegistry registry, UUID actorId) {
        return tag -> {
            if (registry == null || actorId == null) {
                return false;
            }
            var thing = registry.get(actorId);
            if (!(thing instanceof Actor actor)) {
                return false;
            }
            return actor.getSkills().stream()
                    .anyMatch(s -> s.equalsIgnoreCase(tag));
        };
    }

    static AttributeResolver registryAttributeResolver(KernelRegistry registry, UUID... scopeOwnerIds) {
        List<UUID> scopes = new ArrayList<>();
        if (scopeOwnerIds != null) {
            for (UUID scope : scopeOwnerIds) {
                if (scope != null && !scopes.contains(scope)) {
                    scopes.add(scope);
                }
            }
        }
        return (access, context) -> resolveRegistryAttribute(registry, scopes, access, context);
    }

    private static Object resolveRegistryAttribute(
            KernelRegistry registry,
            List<UUID> scopes,
            com.demo.adventure.engine.mechanics.keyexpr.ast.AttributeAccessNode access,
            AttributeResolutionContext context
    ) {
        if (registry == null || access == null || scopes == null || scopes.isEmpty()) {
            return null;
        }
        Thing current = findThingByLabel(registry, scopes, access.root());
        if (current == null) {
            return null;
        }
        List<com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment> segments = access.segments();
        for (int index = 0; index < segments.size(); index++) {
            com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment segment = segments.get(index);
            boolean last = index == segments.size() - 1;
            if (segment instanceof com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment.FixtureSegment fixture) {
                Thing fixtureThing = findFixtureChild(registry, current, fixture.name());
                if (fixtureThing == null) {
                    return null;
                }
                current = fixtureThing;
                continue;
            }
            if (segment instanceof com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment.PropertySegment prop) {
                if (!last) {
                    if (index == segments.size() - 2
                            && segments.get(index + 1) instanceof com.demo.adventure.engine.mechanics.keyexpr.ast.AccessSegment.PropertySegment next) {
                        return resolveCellProperty(registry, current, prop.name(), next.name(), context);
                    }
                    return null;
                }
                return resolveProperty(current, prop.name(), context);
            }
        }
        return null;
    }

    private static Object resolveCellProperty(
            KernelRegistry registry,
            Thing thing,
            String cellName,
            String field,
            AttributeResolutionContext context
    ) {
        if (thing == null || cellName == null || field == null || context == null) {
            return null;
        }
        String key = Thing.normalizeCellKey(cellName);
        Cell cell = thing.getCell(key);
        String prop = field.trim().toLowerCase(Locale.ROOT);
        if (cell == null) {
            CellReferenceStatus status = context.attributePolicy() == AttributeResolutionPolicy.QUERY_STRICT
                    ? CellReferenceStatus.MISSING
                    : CellReferenceStatus.UNDEFINED;
            recordCellReference(registry, thing.getId(), key, prop, status);
            if (context.attributePolicy() == AttributeResolutionPolicy.QUERY_STRICT) {
                throw unknownReference("Unknown cell: " + key + "." + prop, context);
            }
            return fallbackCellValue(prop);
        }
        return switch (prop) {
            case "capacity" -> (double) cell.getCapacity();
            case "amount" -> (double) cell.getAmount();
            case "volume" -> cell.getVolume();
            case "name" -> key;
            default -> {
                CellReferenceStatus status = context.attributePolicy() == AttributeResolutionPolicy.QUERY_STRICT
                        ? CellReferenceStatus.MISSING
                        : CellReferenceStatus.UNDEFINED;
                recordCellReference(registry, thing.getId(), key, prop, status);
                if (context.attributePolicy() == AttributeResolutionPolicy.QUERY_STRICT) {
                    throw unknownReference("Unknown cell field: " + key + "." + prop, context);
                }
                yield fallbackCellValue(prop);
            }
        };
    }

    private static void recordCellReference(
            KernelRegistry registry,
            UUID thingId,
            String cellName,
            String field,
            CellReferenceStatus status
    ) {
        if (registry == null || thingId == null) {
            return;
        }
        registry.recordCellReference(new CellReferenceReceipt(thingId, cellName, field, status));
    }

    private static Object fallbackCellValue(String field) {
        if (field == null) {
            return 0.0;
        }
        return switch (field.trim().toLowerCase(Locale.ROOT)) {
            case "name" -> "";
            default -> 0.0;
        };
    }

    private static Thing findThingByLabel(KernelRegistry registry, List<UUID> scopes, String label) {
        if (registry == null || scopes == null || label == null) {
            return null;
        }
        for (UUID scope : scopes) {
            Thing match = findThingInScope(registry, scope, label);
            if (match != null) {
                return match;
            }
        }
        return null;
    }

    private static Thing findThingInScope(KernelRegistry registry, UUID ownerId, String label) {
        if (registry == null || ownerId == null || label == null) {
            return null;
        }
        Thing owner = registry.get(ownerId);
        if (owner != null && label.equalsIgnoreCase(owner.getLabel())) {
            return owner;
        }
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(ownerId);
        while (!stack.isEmpty()) {
            UUID currentOwner = stack.pop();
            if (!visited.add(currentOwner)) {
                continue;
            }
            for (Thing thing : registry.getEverything().values()) {
                if (thing == null) {
                    continue;
                }
                if (!currentOwner.equals(thing.getOwnerId())) {
                    continue;
                }
                if (label.equalsIgnoreCase(thing.getLabel())) {
                    return thing;
                }
                stack.push(thing.getId());
            }
        }
        return null;
    }

    private static Thing findFixtureChild(KernelRegistry registry, Thing owner, String label) {
        if (registry == null || owner == null || label == null) {
            return null;
        }
        for (Thing thing : registry.getEverything().values()) {
            if (thing == null || !(thing instanceof Item item)) {
                continue;
            }
            if (!owner.getId().equals(item.getOwnerId())) {
                continue;
            }
            if (!item.isFixture()) {
                continue;
            }
            if (label.equalsIgnoreCase(item.getLabel())) {
                return item;
            }
        }
        return null;
    }

    private static Object resolveProperty(Thing thing, String property, AttributeResolutionContext context) {
        if (thing == null || property == null) {
            return null;
        }
        String key = property.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "open" -> evaluateNested(thing.getKey(), context);
            case "visible" -> evaluateVisibility(thing, context);
            case "label" -> thing.getLabel();
            case "description" -> thing.getDescription();
            case "kind" -> thing.getKind() == null ? null : thing.getKind().name();
            case "id" -> thing.getId() == null ? null : thing.getId().toString();
            case "ownerid", "owner" -> thing.getOwnerId() == null ? null : thing.getOwnerId().toString();
            case "size" -> (double) thing.getSize();
            case "weight" -> (double) thing.getWeight();
            case "volume" -> (double) thing.getVolume();
            case "ttl" -> (double) thing.getTtl();
            case "fixture" -> (thing instanceof Item item) ? item.isFixture() : null;
            case "footprintwidth" -> (thing instanceof Item item) ? item.getFootprintWidth() : null;
            case "footprintheight" -> (thing instanceof Item item) ? item.getFootprintHeight() : null;
            case "capacitywidth" -> (thing instanceof Item item) ? item.getCapacityWidth() : null;
            case "capacityheight" -> (thing instanceof Item item) ? item.getCapacityHeight() : null;
            case "plotkind" -> (thing instanceof Plot plot && plot.getPlotKind() != null)
                    ? plot.getPlotKind().name()
                    : null;
            case "plotrole" -> (thing instanceof Plot plot) ? plot.getPlotRole() : null;
            case "region" -> (thing instanceof Plot plot) ? plot.getRegion() : null;
            case "locationx" -> (thing instanceof Plot plot) ? (double) plot.getLocationX() : null;
            case "locationy" -> (thing instanceof Plot plot) ? (double) plot.getLocationY() : null;
            case "direction" -> (thing instanceof Gate gate && gate.getDirection() != null)
                    ? gate.getDirection().name()
                    : null;
            case "plotaid" -> (thing instanceof Gate gate && gate.getPlotAId() != null)
                    ? gate.getPlotAId().toString()
                    : null;
            case "plotbid" -> (thing instanceof Gate gate && gate.getPlotBId() != null)
                    ? gate.getPlotBId().toString()
                    : null;
            default -> null;
        };
    }

    private static boolean evaluateNested(String expression, AttributeResolutionContext context) {
        KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(
                expression,
                context.hasResolver(),
                context.searchResolver(),
                context.skillResolver(),
                context.attributeResolver(),
                context.attributePolicy()
        );
        if (!result.isSuccess()) {
            throw new KeyExpressionEvaluationException(result.error());
        }
        return result.value();
    }

    private static boolean evaluateVisibility(Thing thing, AttributeResolutionContext context) {
        boolean baseVisible = thing.isVisibleFlag();
        if (!baseVisible) {
            return false;
        }
        return evaluateNested(thing.getVisibilityKey(), context);
    }

    private static UnknownReferenceException unknownReference(String message, AttributeResolutionContext ctx) {
        String input = ctx == null ? "" : ctx.input();
        return new UnknownReferenceException(new KeyExpressionError(
                KeyExpressionError.Phase.EVALUATE,
                message,
                input,
                -1
        ));
    }

    private static boolean registryContains(KernelRegistry registry, UUID ownerId, String label) {
        if (registry == null || ownerId == null || label == null) {
            return false;
        }

        Set<UUID> visited = new HashSet<>();
        Deque<UUID> stack = new ArrayDeque<>();
        stack.push(ownerId);

        while (!stack.isEmpty()) {
            UUID currentOwner = stack.pop();
            if (!visited.add(currentOwner)) {
                continue;
            }

            for (var thing : registry.getEverything().values()) {
                if (thing == null) {
                    continue;
                }
                if (!currentOwner.equals(thing.getOwnerId())) {
                    continue;
                }
                if (label.equalsIgnoreCase(thing.getLabel())) {
                    return true;
                }
                stack.push(thing.getId());
            }
        }

        return false;
    }
}

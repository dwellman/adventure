package com.demo.adventure.engine.runtime;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class RuntimeScene {
    private final GameRuntime runtime;

    RuntimeScene(GameRuntime runtime) {
        this.runtime = runtime;
    }

    List<String> visibleFixtureLabels() {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        if (registry == null || plotId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(Item::isVisible)
                .filter(item -> plotId.equals(item.getOwnerId()))
                .map(Item::getLabel)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    List<String> visibleItemLabels() {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        if (registry == null || plotId == null) {
            return List.of();
        }
        List<Item> plotItems = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> !item.isFixture())
                .filter(Item::isVisible)
                .filter(item -> plotId.equals(item.getOwnerId()))
                .toList();
        List<Item> fixtureItems = itemsInOpenFixturesAtPlot();
        return java.util.stream.Stream.concat(plotItems.stream(), fixtureItems.stream())
                .map(Item::getLabel)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    List<String> visibleActorLabels(UUID excludeActorId) {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        if (registry == null || plotId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(actor -> plotId.equals(actor.getOwnerId()))
                .filter(actor -> excludeActorId == null || !excludeActorId.equals(actor.getId()))
                .map(Actor::getLabel)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    void describe() {
        String rawScene = buildSceneSnapshot();
        if (rawScene.isBlank()) {
            runtime.narrate("(unknown location)");
            return;
        }
        runtime.narrate(rawScene);
        runtime.updateScene(rawScene);
    }

    void primeScene() {
        String rawScene = buildSceneSnapshot();
        if (!rawScene.isBlank()) {
            runtime.updateScene(rawScene);
        }
    }

    void lookDirectionOrThing(String arg) {
        Direction dir = runtime.parseDirection(arg);
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        UUID playerId = runtime.playerId();
        if (dir == null) {
            String target = arg == null ? "" : arg.trim();
            if (target.isBlank()) {
                runtime.narrate("Nothing else stands out.");
                return;
            }

            String targetLower = target.toLowerCase(Locale.ROOT);

            Item plotItem = registry.getEverything().values().stream()
                    .filter(Item.class::isInstance)
                    .map(Item.class::cast)
                    .filter(Item::isVisible)
                    .filter(i -> plotId.equals(i.getOwnerId()))
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (plotItem != null) {
                describeThing(plotItem);
                return;
            }

            Item fixtureItem = itemsInOpenFixturesAtPlot().stream()
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (fixtureItem != null) {
                describeThing(fixtureItem);
                return;
            }

            Item fixture = registry.getEverything().values().stream()
                    .filter(Item.class::isInstance)
                    .map(Item.class::cast)
                    .filter(Item::isFixture)
                    .filter(Item::isVisible)
                    .filter(i -> plotId.equals(i.getOwnerId()))
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (fixture != null) {
                describeThing(fixture);
                return;
            }

            Actor actor = registry.getEverything().values().stream()
                    .filter(Actor.class::isInstance)
                    .map(Actor.class::cast)
                    .filter(Actor::isVisible)
                    .filter(a -> plotId.equals(a.getOwnerId()))
                    .filter(a -> a.getLabel() != null && a.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (actor != null) {
                describeThing(actor);
                return;
            }

            Item carried = registry.getEverything().values().stream()
                    .filter(Item.class::isInstance)
                    .map(Item.class::cast)
                    .filter(i -> playerId.equals(i.getOwnerId()))
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (carried != null) {
                describeThing(carried);
                return;
            }

            Plot current = registry.get(plotId) instanceof Plot plot ? plot : null;
            if (current != null && current.getDescription() != null
                    && current.getDescription().toLowerCase(Locale.ROOT).contains(targetLower)) {
                runtime.narrate("You focus on the " + target + ". It stands out just as described.");
            } else {
                runtime.narrate("Nothing else stands out about " + target + ".");
            }
            return;
        }
        Gate gate = exits().stream()
                .filter(g -> dir.equals(g.directionFrom(plotId)))
                .findFirst()
                .orElse(null);
        if (gate == null) {
            runtime.narrate("Nothing special to the " + dir.toLongName().toLowerCase(Locale.ROOT) + ".");
            return;
        }
        String gateDesc = stripGateDestinationTag(gate.getDescriptionFrom(plotId));
        if (gateDesc != null && !gateDesc.isBlank()) {
            runtime.narrate(formatDirectionLook(dir, gateDesc));
        } else {
            runtime.narrate(formatDirectionLook(dir, "You see an exit."));
        }
    }

    void describeThing(Thing thing) {
        String label = thing.getLabel() == null ? "It" : thing.getLabel();
        String desc = thing.getDescription();
        if (desc == null || desc.isBlank()) {
            runtime.narrate(label + ": nothing unusual here.");
        } else {
            runtime.narrate(label + ": " + desc);
        }
    }

    List<Gate> exits() {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        if (registry == null || plotId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .filter(Gate::isVisible)
                .filter(g -> g.connects(plotId))
                .toList();
    }

    String firstExitDirection() {
        UUID plotId = runtime.currentPlotId();
        return exits().stream()
                .map(g -> g.directionFrom(plotId))
                .filter(Objects::nonNull)
                .map(Direction::toLongName)
                .sorted()
                .findFirst()
                .orElse("");
    }

    String ensurePeriod(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String t = text.trim();
        return t.endsWith(".") ? t : t + ".";
    }

    String stripGateDestinationTag(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }
        String trimmed = description.trim();
        int idx = trimmed.toLowerCase(Locale.ROOT).lastIndexOf(" to:");
        if (idx == -1) {
            return trimmed;
        }
        String before = trimmed.substring(0, idx).trim();
        String dest = trimmed.substring(idx + 4).trim();
        if (dest.isEmpty()) {
            return trimmed;
        }
        if (before.toLowerCase(Locale.ROOT).contains(dest.toLowerCase(Locale.ROOT))) {
            return before;
        }
        return trimmed;
    }

    String formatDirectionLook(Direction dir, String description) {
        String prefix = directionPrefix(dir);
        String desc = description == null ? "" : description.trim();
        if (desc.isEmpty()) {
            desc = "Nothing special.";
        }
        return prefix + ": " + desc;
    }

    private String directionPrefix(Direction dir) {
        if (dir == null) {
            return "There";
        }
        return switch (dir) {
            case UP -> "Up";
            case DOWN -> "Down";
            default -> "To the " + directionLabel(dir);
        };
    }

    private String directionLabel(Direction dir) {
        String raw = dir == null ? "" : dir.toLongName();
        return raw.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    List<Item> itemsInOpenFixturesAtPlot() {
        List<Item> openFixtures = openFixturesAtPlot();
        KernelRegistry registry = runtime.registry();
        if (openFixtures.isEmpty() || registry == null) {
            return List.of();
        }
        Set<UUID> openFixtureIds = openFixtures.stream()
                .map(Item::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (openFixtureIds.isEmpty()) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> !item.isFixture())
                .filter(Item::isVisible)
                .filter(item -> openFixtureIds.contains(item.getOwnerId()))
                .toList();
    }

    private List<Item> openFixturesAtPlot() {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        UUID playerId = runtime.playerId();
        if (registry == null || plotId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(Item::isVisible)
                .filter(item -> plotId.equals(item.getOwnerId()))
                .filter(item -> runtime.isThingOpen(item, registry, playerId, plotId))
                .toList();
    }

    private String buildSceneSnapshot() {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        UUID playerId = runtime.playerId();
        Plot plot = registry == null || plotId == null ? null : registry.get(plotId) instanceof Plot current ? current : null;
        if (plot == null) {
            return "";
        }
        StringBuilder snapshot = new StringBuilder();
        if (plot.getLabel() != null && !plot.getLabel().isBlank()) {
            snapshot.append("# ").append(plot.getLabel().trim()).append("\n");
        }
        if (plot.getDescription() != null && !plot.getDescription().isBlank()) {
            snapshot.append(plot.getDescription().trim()).append("\n");
        }

        List<Item> fixtures = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(Item::isVisible)
                .filter(item -> plotId.equals(item.getOwnerId()))
                .toList();
        if (!fixtures.isEmpty()) {
            snapshot.append("Fixtures:\n");
            fixtures.forEach(f -> snapshot.append("- ").append(f.getLabel()).append("\n"));
        }

        List<Item> plotItems = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> !item.isFixture())
                .filter(Item::isVisible)
                .filter(item -> plotId.equals(item.getOwnerId()))
                .toList();
        Map<UUID, Item> itemsById = new LinkedHashMap<>();
        plotItems.forEach(item -> itemsById.put(item.getId(), item));
        itemsInOpenFixturesAtPlot().forEach(item -> itemsById.putIfAbsent(item.getId(), item));
        List<Item> items = new ArrayList<>(itemsById.values());
        if (!items.isEmpty()) {
            snapshot.append("Items:\n");
            items.forEach(i -> snapshot.append("- ").append(i.getLabel()).append("\n"));
        }

        List<Actor> actors = registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(actor -> plotId.equals(actor.getOwnerId()))
                .filter(Actor::isVisible)
                .filter(actor -> !actor.getId().equals(playerId))
                .toList();
        if (!actors.isEmpty()) {
            snapshot.append("You see:\n");
            actors.forEach(a -> snapshot.append("- ").append(a.getLabel()).append("\n"));
        }

        List<Gate> exits = exits();
        if (!exits.isEmpty()) {
            List<String> dirList = exits.stream()
                    .map(g -> {
                        Direction d = g.directionFrom(plotId);
                        if (d == null) {
                            return null;
                        }
                        return d.toLongName();
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            if (!dirList.isEmpty()) {
                String separator = " \u2022 ";
                snapshot.append("Exits: ").append(String.join(separator, dirList)).append("\n");
            }
        }
        return snapshot.toString().trim();
    }
}

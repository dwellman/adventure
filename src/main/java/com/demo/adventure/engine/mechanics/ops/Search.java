package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Thing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class Search {
    private Search() {
    }

    public static String search(
            Thing target,
            Function<Thing, List<Thing>> fixtureChildrenProvider,
            Function<Thing, List<Look.ContainerEntry>> containerEntriesProvider
    ) {
        if (target == null) {
            return "";
        }

        ScanResult result = new ScanResult();
        Set<UUID> visited = new HashSet<>();
        dfs(target, fixtureChildrenProvider, containerEntriesProvider, visited, result);

        return render(target, result);
    }

    private static void dfs(
            Thing thing,
            Function<Thing, List<Thing>> fixtureChildrenProvider,
            Function<Thing, List<Look.ContainerEntry>> containerEntriesProvider,
            Set<UUID> visited,
            ScanResult result
    ) {
        if (thing == null) {
            return;
        }
        UUID id = thing.getId();
        if (id != null && !visited.add(id)) {
            return;
        }

        if (thing.isOpen()) {
            result.searchedAreas.add(thing);
            List<Look.ContainerEntry> entries = containerEntriesProvider == null ? List.of() : containerEntriesProvider.apply(thing);
            if (entries != null) {
                for (Look.ContainerEntry entry : entries) {
                    if (entry == null || entry.thing() == null) {
                        continue;
                    }
                    if (entry.thing().isVisible()) {
                        result.visibleMatches.add(entry);
                    } else {
                        result.hiddenMatches.add(entry);
                    }
                }
            }
        } else {
            result.skippedAreas.add(thing);
        }

        List<Thing> children = fixtureChildrenProvider == null ? List.of() : fixtureChildrenProvider.apply(thing);
        if (children == null) {
            return;
        }
        for (Thing child : children) {
            if (child != null && child.isVisible()) {
                dfs(child, fixtureChildrenProvider, containerEntriesProvider, visited, result);
            }
        }
    }

    private static String render(Thing target, ScanResult result) {
        StringBuilder sb = new StringBuilder();
        sb.append("You search ").append(target.getLabel() == null ? "" : target.getLabel()).append('.').append('\n');

        sb.append("Searched: ").append(result.searchedAreas.size()).append('.');
        if (!result.searchedAreas.isEmpty()) {
            sb.append(' ').append(sampleLabels(result.searchedAreas.stream()
                    .map(t -> t.getLabel() == null ? "" : t.getLabel())
                    .collect(Collectors.toList())));
        }
        sb.append('\n');

        sb.append("Skipped: ").append(result.skippedAreas.size()).append('.');
        if (!result.skippedAreas.isEmpty()) {
            sb.append(' ').append(sampleLabels(result.skippedAreas.stream()
                    .map(t -> t.getLabel() == null ? "" : t.getLabel())
                    .collect(Collectors.toList())));
        }
        sb.append('\n');

        List<Look.ContainerEntry> orderedVisible = orderEntries(result.visibleMatches);
        List<Look.ContainerEntry> orderedHidden = orderEntries(result.hiddenMatches);

        sb.append("Found: ")
                .append(orderedVisible.size())
                .append(" visible, ")
                .append(orderedHidden.size())
                .append(" hidden.");

        if (!orderedVisible.isEmpty()) {
            sb.append('\n');
            sb.append("Visible: ").append(sampleLabels(orderedVisible.stream()
                    .map(e -> e.thing().getLabel() == null ? "" : e.thing().getLabel())
                    .collect(Collectors.toList())));
        }

        if (!orderedHidden.isEmpty()) {
            sb.append('\n');
            sb.append("Hidden: ").append(sampleLabels(orderedHidden.stream()
                    .map(e -> e.thing().getLabel() == null ? "" : e.thing().getLabel())
                    .collect(Collectors.toList())));
        }

        return sb.toString();
    }

    private static List<Look.ContainerEntry> orderEntries(List<Look.ContainerEntry> entries) {
        List<Look.ContainerEntry> placed = entries.stream()
                .filter(e -> e != null && e.placement() != null)
                .sorted(Comparator
                        .comparingDouble((Look.ContainerEntry e) -> e.placement().y())
                        .thenComparingDouble(e -> e.placement().x()))
                .collect(Collectors.toList());
        List<Look.ContainerEntry> unplaced = entries.stream()
                .filter(e -> e != null && e.placement() == null)
                .sorted(Comparator.comparing(e -> e.thing().getId()))
                .collect(Collectors.toList());

        List<Look.ContainerEntry> ordered = new ArrayList<>(placed.size() + unplaced.size());
        ordered.addAll(placed);
        ordered.addAll(unplaced);
        return ordered;
    }

    private static String sampleLabels(List<String> labels) {
        if (labels.isEmpty()) {
            return "";
        }
        int limit = Math.min(3, labels.size());
        String base = String.join(", ", labels.subList(0, limit));
        if (labels.size() > limit) {
            return base + ", and " + (labels.size() - limit) + " more.";
        }
        return base + ".";
    }

    private static final class ScanResult {
        private final List<Thing> searchedAreas = new ArrayList<>();
        private final List<Thing> skippedAreas = new ArrayList<>();
        private final List<Look.ContainerEntry> visibleMatches = new ArrayList<>();
        private final List<Look.ContainerEntry> hiddenMatches = new ArrayList<>();
    }
}

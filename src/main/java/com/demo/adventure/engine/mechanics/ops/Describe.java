package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Thing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Describe {
    private Describe() {
    }

    public static String describe(Thing target, List<Thing> fixtures, List<Look.ContainerEntry> containerEntries) {
        if (target == null) {
            return "";
        }

        List<Thing> safeFixtures = fixtures == null ? List.of() : fixtures.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<Look.ContainerEntry> safeEntries = containerEntries == null ? List.of() : containerEntries.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(target.getLabel() == null ? "" : target.getLabel()).append('\n');
        sb.append(target.getDescription() == null ? "" : target.getDescription()).append('\n');

        appendFixtures(sb, safeFixtures);

        if (!target.isOpen()) {
            sb.append("Inside: (closed)");
            return sb.toString();
        }

        appendInside(sb, safeEntries);
        return sb.toString();
    }

    private static void appendFixtures(StringBuilder sb, List<Thing> fixtures) {
        List<Thing> visibleFixtures = fixtures.stream()
                .filter(Thing::isVisible)
                .collect(Collectors.toList());
        int count = visibleFixtures.size();

        sb.append("Fixtures: ").append(count).append('.').append('\n');
        if (count == 0) {
            return;
        }

        int limit = Math.min(3, count);
        for (int i = 0; i < limit; i++) {
            Thing fixture = visibleFixtures.get(i);
            String label = fixture.getLabel() == null ? "" : fixture.getLabel();
            String description = fixture.getDescription() == null ? "" : fixture.getDescription();
            sb.append("- ")
                    .append(label)
                    .append(fixture.isOpen() ? " (open)" : " (closed)")
                    .append(": ")
                    .append(description)
                    .append('\n');
        }

        if (count > limit) {
            sb.append("- and ").append(count - limit).append(" more.").append('\n');
        }
    }

    private static void appendInside(StringBuilder sb, List<Look.ContainerEntry> entries) {
        List<Look.ContainerEntry> visibleEntries = entries.stream()
                .filter(e -> e.thing() != null && e.thing().isVisible())
                .collect(Collectors.toList());

        List<Look.ContainerEntry> placedVisible = visibleEntries.stream()
                .filter(e -> e.placement() != null)
                .sorted(Comparator
                        .comparingDouble((Look.ContainerEntry e) -> e.placement().y())
                        .thenComparingDouble(e -> e.placement().x()))
                .collect(Collectors.toList());
        List<Look.ContainerEntry> unplacedVisible = visibleEntries.stream()
                .filter(e -> e.placement() == null)
                .sorted(Comparator.comparing(e -> e.thing().getId()))
                .collect(Collectors.toList());

        List<Look.ContainerEntry> orderedVisible = new ArrayList<>();
        orderedVisible.addAll(placedVisible);
        orderedVisible.addAll(unplacedVisible);

        int visibleCount = orderedVisible.size();
        double occupiedArea = entries.stream()
                .filter(e -> e.placement() != null)
                .mapToDouble(e -> e.placement().width() * e.placement().height())
                .sum();
        int fullnessPercent = (int) Math.round(occupiedArea * 100);

        sb.append("Inside: ").append(visibleCount).append(" items, ").append(fullnessPercent).append("% full.");
        if (visibleCount == 0) {
            return;
        }
        sb.append('\n');

        int limit = Math.min(3, visibleCount);
        for (int i = 0; i < limit; i++) {
            Look.ContainerEntry entry = orderedVisible.get(i);
            String label = entry.thing().getLabel() == null ? "" : entry.thing().getLabel();
            String description = entry.thing().getDescription() == null ? "" : entry.thing().getDescription();
            sb.append("- ")
                    .append(label)
                    .append(": ")
                    .append(description);
            if (i < limit - 1 || (visibleCount > limit)) {
                sb.append('\n');
            }
        }

        if (visibleCount > limit) {
            sb.append("- and ").append(visibleCount - limit).append(" more.");
        }
    }
}

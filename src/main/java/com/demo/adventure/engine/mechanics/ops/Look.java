package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.model.Thing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class Look {
    private Look() {
    }

    public static String look(Thing target, List<Thing> fixtures, List<ContainerEntry> containerEntries) {
        if (target == null) {
            return "";
        }
        List<Thing> safeFixtures = fixtures == null ? List.of() : fixtures.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<ContainerEntry> safeEntries = containerEntries == null ? List.of() : containerEntries.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append(target.getLabel() == null ? "" : target.getLabel()).append('\n');

        appendFixturesLine(sb, safeFixtures);

        if (!target.isOpen()) {
            sb.append("Inside: (closed)");
            return sb.toString();
        }

        appendInsideLine(sb, safeEntries);
        return sb.toString();
    }

    private static void appendFixturesLine(StringBuilder sb, List<Thing> fixtures) {
        List<Thing> visibleFixtures = fixtures.stream()
                .filter(Thing::isVisible)
                .collect(Collectors.toList());
        int count = visibleFixtures.size();
        sb.append("Fixtures: ");
        if (count == 0) {
            sb.append("0.").append('\n');
            return;
        }
        sb.append(count).append(". ");
        List<String> sample = visibleFixtures.stream()
                .limit(3)
                .map(f -> (f.getLabel() == null ? "" : f.getLabel()) + (f.isOpen() ? " (open)" : " (closed)"))
                .collect(Collectors.toList());
        sb.append(String.join(", ", sample));
        if (count > sample.size()) {
            sb.append(", and ").append(count - sample.size()).append(" more.");
        } else {
            sb.append(".");
        }
        sb.append('\n');
    }

    private static void appendInsideLine(StringBuilder sb, List<ContainerEntry> entries) {
        List<ContainerEntry> visibleEntries = entries.stream()
                .filter(e -> e.thing() != null && e.thing().isVisible())
                .collect(Collectors.toList());

        List<ContainerEntry> placedVisible = visibleEntries.stream()
                .filter(e -> e.placement() != null)
                .sorted(Comparator
                        .comparingDouble((ContainerEntry e) -> e.placement().y())
                        .thenComparingDouble(e -> e.placement().x()))
                .collect(Collectors.toList());
        List<ContainerEntry> unplacedVisible = visibleEntries.stream()
                .filter(e -> e.placement() == null)
                .sorted(Comparator.comparing(e -> e.thing().getId()))
                .collect(Collectors.toList());

        List<ContainerEntry> orderedVisible = new ArrayList<>();
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

        List<String> sample = orderedVisible.stream()
                .limit(3)
                .map(e -> e.thing().getLabel() == null ? "" : e.thing().getLabel())
                .collect(Collectors.toList());

        sb.append(' ').append(String.join(", ", sample));
        if (visibleCount > sample.size()) {
            sb.append(", and ").append(visibleCount - sample.size()).append(" more.");
        } else {
            sb.append(".");
        }
    }

    public record ContainerEntry(Thing thing, Rectangle2D placement) {
    }
}

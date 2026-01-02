package com.demo.adventure.engine.integrity;

import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Thing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

final class IntegritySimulationState {
    private IntegritySimulationState() {
    }

    static Set<String> inventoryLabels(GameRuntime runtime) {
        if (runtime == null) {
            return Set.of();
        }
        return runtime.inventoryLabels().stream()
                .map(IntegrityLabels::normalizeLabel)
                .filter(label -> !label.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    static String stateSignature(GameRuntime runtime) {
        if (runtime == null || runtime.registry() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("plot=").append(runtime.currentPlotId());
        Map<UUID, Thing> things = runtime.registry().getEverything();
        List<UUID> ids = new ArrayList<>(things.keySet());
        ids.sort(Comparator.comparing(UUID::toString));
        for (UUID id : ids) {
            Thing thing = things.get(id);
            if (thing == null) {
                continue;
            }
            sb.append("|").append(id);
            sb.append(":").append(thing.getKind());
            sb.append(":owner=").append(thing.getOwnerId());
            sb.append(":vis=").append(thing.isVisibleFlag());
            sb.append(":key=").append(thing.getKey());
            sb.append(":vkey=").append(thing.getVisibilityKey());
            if (thing instanceof Item item) {
                sb.append(":fixture=").append(item.isFixture());
            }
            if (thing.getCells() != null && !thing.getCells().isEmpty()) {
                List<String> cells = new ArrayList<>();
                for (Map.Entry<String, com.demo.adventure.engine.mechanics.cells.Cell> entry : thing.getCells().entrySet()) {
                    com.demo.adventure.engine.mechanics.cells.Cell cell = entry.getValue();
                    if (cell == null) {
                        continue;
                    }
                    cells.add(entry.getKey() + "=" + cell.getAmount() + "/" + cell.getCapacity());
                }
                Collections.sort(cells);
                for (String cell : cells) {
                    sb.append(":cell=").append(cell);
                }
            }
        }
        List<String> inv = new ArrayList<>(inventoryLabels(runtime));
        inv.sort(String.CASE_INSENSITIVE_ORDER);
        for (String item : inv) {
            sb.append(":inv=").append(item);
        }
        return sb.toString();
    }
}

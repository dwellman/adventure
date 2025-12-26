package com.demo.adventure.authoring.save.build;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pretty-printer for {@link WorldBillOfMaterials}.
 */
public final class WorldBillOfMaterialsFormatter {
    private WorldBillOfMaterialsFormatter() {
    }

    /**
     * Format a BOM with simple, human-readable sections.
     *
     * @param bom bill of materials to render
     * @return formatted string (empty when bom is null)
     */
    public static String format(WorldBillOfMaterials bom) {
        if (bom == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(bom.getTitle() == null ? "" : bom.getTitle()).append('\n');

        List<WorldBillOfMaterials.Section> sections = bom.getSections() == null ? List.of() : bom.getSections();
        for (int i = 0; i < sections.size(); i++) {
            WorldBillOfMaterials.Section section = sections.get(i);
            sb.append(section.title()).append('\n');
            List<WorldBillOfMaterials.Entry> entries = new ArrayList<>(
                    section.entries() == null ? List.of() : section.entries()
            );
            entries.sort(entryComparator());
            for (WorldBillOfMaterials.Entry entry : entries) {
                sb.append(entry.quantity())
                        .append('×')
                        .append(' ')
                        .append(entry.name() == null ? "" : entry.name());
                String notes = entry.notes();
                if (notes != null && !notes.isBlank()) {
                    sb.append(" (").append(notes).append(')');
                }
                sb.append('\n');
            }
            if (i < sections.size() - 1) {
                sb.append('\n');
            }
        }

        return sb.toString().trim();
    }

    private static Comparator<WorldBillOfMaterials.Entry> entryComparator() {
        return Comparator
                .comparing((WorldBillOfMaterials.Entry e) -> e.name() == null ? "" : e.name().toLowerCase(Locale.ROOT))
                .thenComparing(e -> e.notes() == null ? "" : e.notes().toLowerCase(Locale.ROOT));
    }
}

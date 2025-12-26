package com.demo.adventure.authoring.gardener;

import com.demo.adventure.domain.save.GameSave;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Validates gardener patches for safety.
 */
public final class GardenerPatchValidator {
    private GardenerPatchValidator() {
    }

    // Pattern: Verification
    // - Enforces caps, valid IDs, and safety rules before narration patches can be applied.
    public static ValidationResult validate(GameSave save, GardenerPatch patch, int plotTitleCap, int plotDescCap, int thingNameCap, int thingDescCap) {
        Set<String> problems = new HashSet<>();
        Set<String> warnings = new HashSet<>();
        if (save == null) {
            problems.add("Save is null");
            return new ValidationResult(false, problems, warnings);
        }
        if (patch == null) {
            problems.add("Patch is null");
            return new ValidationResult(false, problems, warnings);
        }

        Set<UUID> plotIds = new HashSet<>();
        save.plots().forEach(p -> plotIds.add(p.plotId()));
        Set<UUID> thingIds = new HashSet<>();
        save.fixtures().forEach(f -> thingIds.add(f.id()));
        save.items().forEach(i -> thingIds.add(i.id()));
        save.actors().forEach(a -> thingIds.add(a.id()));

        Map<UUID, GardenerPatch.PlotPatch> plotPatches = patch.plots() == null ? Map.of() : patch.plots();
        Map<UUID, GardenerPatch.ThingPatch> thingPatches = patch.things() == null ? Map.of() : patch.things();

        // Unknown IDs
        for (UUID id : plotPatches.keySet()) {
            if (!plotIds.contains(id)) {
                problems.add("Unknown plotId in patch: " + id);
            }
        }
        for (UUID id : thingPatches.keySet()) {
            if (!thingIds.contains(id)) {
                problems.add("Unknown thingId in patch: " + id);
            }
        }

        // Empty fields and caps
        Set<String> normalizedTitles = new HashSet<>();
        for (Map.Entry<UUID, GardenerPatch.PlotPatch> e : plotPatches.entrySet()) {
            GardenerPatch.PlotPatch p = e.getValue();
            if (p == null) {
                problems.add("Null plot patch for " + e.getKey());
                continue;
            }
            if (isBlank(p.displayTitle())) {
                problems.add("Empty plot title for " + e.getKey());
            } else if (p.displayTitle().length() > plotTitleCap) {
                problems.add("Plot title too long (" + p.displayTitle().length() + ") for " + e.getKey());
            }
            if (isBlank(p.description())) {
                problems.add("Empty plot description for " + e.getKey());
            } else if (p.description().length() > plotDescCap) {
                problems.add("Plot description too long (" + p.description().length() + ") for " + e.getKey());
            }
            String normTitle = normalizeTitle(p.displayTitle());
            if (!isBlank(normTitle)) {
                if (!normalizedTitles.add(normTitle)) {
                    problems.add("Duplicate plot title: " + p.displayTitle());
                }
            }
        }

        for (Map.Entry<UUID, GardenerPatch.ThingPatch> e : thingPatches.entrySet()) {
            GardenerPatch.ThingPatch t = e.getValue();
            if (t == null) {
                problems.add("Null thing patch for " + e.getKey());
                continue;
            }
            if (isBlank(t.displayName())) {
                problems.add("Empty thing name for " + e.getKey());
            } else if (t.displayName().length() > thingNameCap) {
                problems.add("Thing name too long (" + t.displayName().length() + ") for " + e.getKey());
            }
            if (isBlank(t.description())) {
                problems.add("Empty thing description for " + e.getKey());
            } else if (t.description().length() > thingDescCap) {
                problems.add("Thing description too long (" + t.description().length() + ") for " + e.getKey());
            }
        }

        boolean ok = problems.isEmpty();
        // Banned tokens lint (warn only)
        String[] banned = new String[]{"spine", "branch", "zone", "demo", "builder", "plot"};
        for (GardenerPatch.PlotPatch p : plotPatches.values()) {
            if (p == null) {
                continue;
            }
            String title = p.displayTitle() == null ? "" : p.displayTitle().toLowerCase(java.util.Locale.ROOT);
            String desc = p.description() == null ? "" : p.description().toLowerCase(java.util.Locale.ROOT);
            for (String token : banned) {
                if ((title.contains(token)) || (desc.contains(token))) {
                    warnings.add("Banned token '" + token + "' in plot patch title/description");
                }
            }
        }

        return new ValidationResult(ok, problems, warnings);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String normalizeTitle(String title) {
        if (isBlank(title)) {
            return "";
        }
        String trimmed = title.trim();
        // Strip trailing punctuation commonly used in titles.
        trimmed = trimmed.replaceAll("[\\p{Punct}]+$", "");
        // Collapse internal whitespace.
        trimmed = trimmed.replaceAll("\\s+", " ");
        return trimmed.toLowerCase(java.util.Locale.ROOT);
    }

    public record ValidationResult(boolean ok, Set<String> problems, Set<String> warnings) {
    }
}

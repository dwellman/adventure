package com.demo.adventure.engine.integrity;

import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;

import java.util.Locale;

final class IntegrityLabels {
    private IntegrityLabels() {
    }

    static String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.trim().toUpperCase(Locale.ROOT);
    }

    static boolean isSpecialTarget(String value) {
        if (value == null) {
            return false;
        }
        String key = value.trim().toUpperCase(Locale.ROOT);
        return key.equals("@PLAYER")
                || key.equals("@PLOT")
                || key.equals("@WORLD")
                || key.equals("@TARGET")
                || key.equals("@OBJECT");
    }

    static String reverseGateLabel(String label) {
        if (label == null) {
            return "";
        }
        String[] parts = label.split("->", -1);
        if (parts.length != 2) {
            return "";
        }
        String left = parts[0].trim();
        String right = parts[1].trim();
        if (left.isBlank() || right.isBlank()) {
            return "";
        }
        return right + " -> " + left;
    }

    static String resolveRevealTarget(String target, String triggerTarget, String triggerObject) {
        if (target == null || target.isBlank()) {
            return normalizeLabel(triggerTarget);
        }
        String key = target.trim().toUpperCase(Locale.ROOT);
        if (key.equals("@TARGET")) {
            return normalizeLabel(triggerTarget);
        }
        if (key.equals("@OBJECT")) {
            return normalizeLabel(triggerObject);
        }
        if (key.startsWith("@")) {
            return "";
        }
        return normalizeLabel(target);
    }

    static boolean isOwnedByPlot(KernelRegistry registry, Item item) {
        if (registry == null || item == null || item.getOwnerId() == null) {
            return false;
        }
        Thing owner = registry.get(item.getOwnerId());
        return owner instanceof Plot;
    }
}

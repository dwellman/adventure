package com.demo.adventure.authoring.save.io;

import java.util.Locale;

public record FootprintRule(String contains, double width, double height) {
    public boolean matches(String label) {
        if (label == null || label.isBlank()) {
            return false;
        }
        if (contains == null || contains.isBlank()) {
            return false;
        }
        return label.toLowerCase(Locale.ROOT).contains(contains.toLowerCase(Locale.ROOT));
    }
}

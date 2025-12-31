package com.demo.adventure.buui;

public record BuuiStyle(boolean bold, boolean italic, AnsiColor color) {

    public static BuuiStyle none() {
        return new BuuiStyle(false, false, null);
    }

    public boolean isEmpty() {
        return !bold && !italic && color == null;
    }
}

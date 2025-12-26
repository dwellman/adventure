package com.demo.adventure.buui;

public class BuuiConsole {

    public static void setOutputSuppressed(boolean suppressed) {
        ConsolePrinter.setMuted(suppressed);
    }

    public static boolean isOutputSuppressed() {
        return ConsolePrinter.isMuted();
    }

    protected static void print(String text) {
        if (text == null) {
            return;
        }
        ConsolePrinter.print(text);
    }

    protected static void printText(String text) {
        if (text == null) {
            return;
        }
        if (ConsolePrinter.isMuted()) {
            return;
        }
        System.out.println(text);
    }

    protected static void printBlank() {
        if (ConsolePrinter.isMuted()) {
            return;
        }
        System.out.println();
    }
}

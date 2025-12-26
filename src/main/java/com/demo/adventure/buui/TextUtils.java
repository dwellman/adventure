package com.demo.adventure.buui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TextUtils {

    public static List<String> wrap(String text, int maxWidth) {
        if (text == null) {
            return List.of("");
        }
        if (maxWidth <= 0) {
            return List.of("");
        }

        var result = new ArrayList<String>();
        var rawLines = text.split("\\R", -1);
        for (var rawLine : rawLines) {
            if (rawLine.length() <= maxWidth) {
                result.add(rawLine);
                continue;
            }
            result.addAll(wrapLine(rawLine, maxWidth));
        }
        return result;
    }

    private static List<String> wrapLine(String line, int maxWidth) {
        var result = new ArrayList<String>();
        var words = new ArrayList<>(Arrays.asList(line.split(" ")));
        var current = new StringBuilder();
        while (!words.isEmpty()) {
            var word = words.remove(0);
            if (word.length() > maxWidth) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                }
                result.add(word.substring(0, maxWidth));
                words.add(0, word.substring(maxWidth));
                continue;
            }
            var separator = current.length() == 0 ? 0 : 1;
            if (current.length() + separator + word.length() > maxWidth) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                }
            }
            if (current.length() > 0) {
                current.append(" ");
            }
            current.append(word);
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    public static String truncate(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (maxWidth <= 0) {
            return "";
        }
        if (text.length() <= maxWidth) {
            return text;
        }
        if (maxWidth <= 3) {
            return text.substring(0, maxWidth);
        }
        return text.substring(0, maxWidth - 3) + "...";
    }

    public static String pad(String text, int width, Alignment alignment) {
        if (text == null) {
            text = "";
        }
        var length = text.length();
        if (length >= width) {
            return text;
        }
        var remaining = width - length;
        return switch (alignment) {
            case LEFT -> text + " ".repeat(remaining);
            case RIGHT -> " ".repeat(remaining) + text;
            case CENTER -> {
                var left = remaining / 2;
                var right = remaining - left;
                yield " ".repeat(left) + text + " ".repeat(right);
            }
        };
    }
}

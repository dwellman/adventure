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
            if (visibleLength(rawLine) <= maxWidth) {
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
        int currentLength = 0;
        while (!words.isEmpty()) {
            var word = words.remove(0);
            int wordLength = visibleLength(word);
            if (wordLength > maxWidth) {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                    currentLength = 0;
                }
                var split = splitLongWord(word, maxWidth);
                if (!split.isEmpty()) {
                    result.addAll(split.subList(0, split.size() - 1));
                    current.append(split.get(split.size() - 1));
                    currentLength = visibleLength(current.toString());
                }
                continue;
            }
            var separator = current.isEmpty() ? 0 : 1;
            if (currentLength + separator + wordLength > maxWidth) {
                if (!current.isEmpty()) {
                    result.add(current.toString());
                    current.setLength(0);
                    currentLength = 0;
                }
            }
            if (!current.isEmpty()) {
                current.append(" ");
                currentLength += 1;
            }
            current.append(word);
            currentLength += wordLength;
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
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
        int visible = visibleLength(text);
        if (visible <= maxWidth) {
            return text;
        }
        if (maxWidth <= 3) {
            String clipped = sliceVisible(text, maxWidth);
            return finalizeTruncation(clipped, AnsiStyle.containsAnsi(clipped));
        }
        String clipped = sliceVisible(text, maxWidth - 3);
        String result = clipped + "...";
        return finalizeTruncation(result, AnsiStyle.containsAnsi(clipped));
    }

    public static String trimWhitespace(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        int start = 0;
        int end = text.length();
        while (start < end) {
            char c = text.charAt(start);
            if (c == ' ' || c == '\t') {
                start++;
                continue;
            }
            break;
        }
        while (end > start) {
            char c = text.charAt(end - 1);
            if (c == ' ' || c == '\t') {
                end--;
                continue;
            }
            break;
        }
        if (start == 0 && end == text.length()) {
            return text;
        }
        return text.substring(start, end);
    }

    public static String pad(String text, int width, Alignment alignment) {
        if (text == null) {
            text = "";
        }
        var length = visibleLength(text);
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

    public static int visibleLength(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int length = 0;
        int i = 0;
        while (i < text.length()) {
            int ansiEnd = AnsiStyle.consumeAnsiSequence(text, i);
            if (ansiEnd != -1) {
                i = ansiEnd;
                continue;
            }
            length++;
            i++;
        }
        return length;
    }

    private static String sliceVisible(String text, int maxWidth) {
        if (text == null || maxWidth <= 0) {
            return "";
        }
        StringBuilder out = new StringBuilder(text.length());
        int visible = 0;
        int i = 0;
        while (i < text.length() && visible < maxWidth) {
            int ansiEnd = AnsiStyle.consumeAnsiSequence(text, i);
            if (ansiEnd != -1) {
                out.append(text, i, ansiEnd);
                i = ansiEnd;
                continue;
            }
            out.append(text.charAt(i));
            visible++;
            i++;
        }
        return out.toString();
    }

    private static String finalizeTruncation(String text, boolean hasAnsi) {
        if (!hasAnsi) {
            return text;
        }
        return text + AnsiStyle.reset();
    }

    private static List<String> splitLongWord(String word, int maxWidth) {
        var result = new ArrayList<String>();
        if (word == null || word.isEmpty()) {
            result.add("");
            return result;
        }
        StringBuilder current = new StringBuilder();
        int visible = 0;
        int i = 0;
        while (i < word.length()) {
            int ansiEnd = AnsiStyle.consumeAnsiSequence(word, i);
            if (ansiEnd != -1) {
                current.append(word, i, ansiEnd);
                i = ansiEnd;
                continue;
            }
            current.append(word.charAt(i));
            visible++;
            i++;
            if (visible >= maxWidth) {
                result.add(current.toString());
                current.setLength(0);
                visible = 0;
            }
        }
        if (!current.isEmpty()) {
            result.add(current.toString());
        }
        return result;
    }
}

package com.demo.adventure.buui;

public class BorderCharacters {
    public final String topLeft;
    public final String topRight;
    public final String bottomLeft;
    public final String bottomRight;
    public final String bottomCenter;
    public final String horizontal;
    public final String vertical;
    public final String headerSeparator;
    public final String titleSeparator;
    public final String intersection;
    public final String leftSeparator;
    public final String rightSeparator;

    public static BorderCharacters ascii() {
        return new BorderCharacters("+", "+", "+", "+", "+", "-", "|", "+", "+", "+", "+", "+");
    }

    public static BorderCharacters boxDrawing() {
        return new BorderCharacters("┌", "┐", "└", "┘", "┴", "─", "│", "┬", "┼", "┼", "├", "┤");
    }

    public BorderCharacters(String topLeft,
                            String topRight,
                            String bottomLeft,
                            String bottomRight,
                            String bottomCenter,
                            String horizontal,
                            String vertical,
                            String headerSeparator,
                            String titleSeparator,
                            String intersection,
                            String leftSeparator,
                            String rightSeparator) {
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
        this.bottomCenter = bottomCenter;
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.headerSeparator = headerSeparator;
        this.titleSeparator = titleSeparator;
        this.intersection = intersection;
        this.leftSeparator = leftSeparator;
        this.rightSeparator = rightSeparator;
    }
}

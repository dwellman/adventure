package com.demo.adventure.buui;

public class RenderStyle {
    private final BorderCharacters border;
    private final int cellPadding;
    private final int maxWidth;
    private final boolean showHeader;
    private final boolean showRowSeparators;
    private final boolean showRowNumbers;
    private final boolean showTitle;
    private final boolean wrapCells;

    public RenderStyle(
            BorderCharacters border,
            int cellPadding,
            int maxWidth,
            boolean showHeader,
            boolean showRowSeparators,
            boolean showRowNumbers,
            boolean showTitle,
            boolean wrapCells
    ) {
        this.border = border;
        this.cellPadding = cellPadding;
        this.maxWidth = maxWidth;
        this.showHeader = showHeader;
        this.showRowSeparators = showRowSeparators;
        this.showRowNumbers = showRowNumbers;
        this.showTitle = showTitle;
        this.wrapCells = wrapCells;
    }

    public static RenderStyle defaults() {
        return new RenderStyle(
                BorderCharacters.boxDrawing(), 1, BuuiLayout.columns(), true, true,
                false, true, true
        );
    }

    public BorderCharacters border() {
        return border;
    }

    public int cellPadding() {
        return cellPadding;
    }

    public int maxWidth() {
        return maxWidth;
    }

    public boolean showHeader() {
        return showHeader;
    }

    public boolean showRowSeparators() {
        return showRowSeparators;
    }

    public boolean showRowNumbers() {
        return showRowNumbers;
    }

    public boolean showTitle() {
        return showTitle;
    }

    public boolean wrapCells() {
        return wrapCells;
    }

    public RenderStyle withShowHeader(boolean showHeader) {
        return new RenderStyle(
                border, cellPadding, maxWidth, showHeader, showRowSeparators, showRowNumbers, showTitle, wrapCells
        );
    }

    public RenderStyle withShowRowSeparators(boolean showRowSeparators) {
        return new RenderStyle(
                border, cellPadding, maxWidth, showHeader, showRowSeparators, showRowNumbers, showTitle, wrapCells
        );
    }

    public RenderStyle withShowRowNumbers(boolean showRowNumbers) {
        return new RenderStyle(
                border, cellPadding, maxWidth, showHeader, showRowSeparators, showRowNumbers, showTitle, wrapCells
        );
    }

    public RenderStyle withShowTitle(boolean showTitle) {
        return new RenderStyle(
                border, cellPadding, maxWidth, showHeader, showRowSeparators, showRowNumbers, showTitle, wrapCells
        );
    }

    public RenderStyle withWrapCells(boolean wrapCells) {
        return new RenderStyle(
                border, cellPadding, maxWidth, showHeader, showRowSeparators, showRowNumbers, showTitle, wrapCells
        );
    }

    public RenderStyle withCellPadding(int padding) {
        return new RenderStyle(
                border, padding, maxWidth, showHeader, showRowSeparators, showRowNumbers, showTitle, wrapCells
        );
    }

    public RenderStyle withBorder(BorderCharacters border) {
        return new RenderStyle(
                border, cellPadding, maxWidth, showHeader, showRowSeparators, showRowNumbers, showTitle, wrapCells
        );
    }

    public RenderStyle withMaxWidth(int maxWidth) {
        int safeMaxWidth = Math.max(0, maxWidth);
        return new RenderStyle(
                border, cellPadding, safeMaxWidth, showHeader, showRowSeparators, showRowNumbers, showTitle, wrapCells
        );
    }
}

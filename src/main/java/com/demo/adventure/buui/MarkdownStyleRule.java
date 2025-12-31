package com.demo.adventure.buui;

public final class MarkdownStyleRule {

    private Boolean bold;
    private Boolean italic;
    private AnsiColor color;
    private boolean colorSet;

    public Boolean bold() {
        return bold;
    }

    public void setBold(Boolean bold) {
        this.bold = bold;
    }

    public Boolean italic() {
        return italic;
    }

    public void setItalic(Boolean italic) {
        this.italic = italic;
    }

    public AnsiColor color() {
        return color;
    }

    public boolean colorSet() {
        return colorSet;
    }

    public void setColor(AnsiColor color) {
        this.color = color;
        this.colorSet = true;
    }

    public void merge(MarkdownStyleRule other) {
        if (other == null) {
            return;
        }
        if (other.bold != null) {
            this.bold = other.bold;
        }
        if (other.italic != null) {
            this.italic = other.italic;
        }
        if (other.colorSet) {
            this.color = other.color;
            this.colorSet = true;
        }
    }
}

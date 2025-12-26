package com.demo.adventure.buui;

import java.util.Objects;

public class Column {
    private final String name;
    private final String title;
    private final Alignment alignment;
    private final int minWidth;
    private final int maxWidth;
    private final boolean wrap;
    private final Formatter formatter;

    private Column(Builder builder) {
        this.name = builder.name;
        this.title = builder.title;
        this.alignment = builder.alignment;
        this.minWidth = builder.minWidth;
        this.maxWidth = builder.maxWidth;
        this.wrap = builder.wrap;
        this.formatter = builder.formatter;
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public String name() {
        return name;
    }

    public String title() {
        return title;
    }

    public Alignment alignment() {
        return alignment;
    }

    public int minWidth() {
        return minWidth;
    }

    public int maxWidth() {
        return maxWidth;
    }

    /**
     * Convenience for columns with fixed width (min == max).
     */
    public boolean isFixedWidth() {
        return minWidth == maxWidth;
    }

    public boolean wrap() {
        return wrap;
    }

    public Formatter formatter() {
        return formatter;
    }

    public static class Builder {
        private final String name;
        private String title;
        private Alignment alignment = Alignment.LEFT;
        private int minWidth = 4;
        private int maxWidth = 40;
        private boolean wrap = true;
        private Formatter formatter;

        private Builder(String name) {
            this.name = Objects.requireNonNull(name, "name");
            this.title = name;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder alignment(Alignment alignment) {
            this.alignment = alignment;
            return this;
        }

        public Builder minWidth(int minWidth) {
            this.minWidth = minWidth;
            return this;
        }

        public Builder maxWidth(int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }

        /**
         * Set a fixed width (min and max) for this column.
         */
        public Builder width(int width) {
            this.minWidth = width;
            this.maxWidth = width;
            return this;
        }

        public Builder wrap(boolean wrap) {
            this.wrap = wrap;
            return this;
        }

        public Builder formatter(Formatter formatter) {
            this.formatter = formatter;
            return this;
        }

        public Column build() {
            return new Column(this);
        }
    }
}

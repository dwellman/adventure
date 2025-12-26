package com.demo.adventure.domain.model;

public record Rectangle2D(double x, double y, double width, double height) {

    public double right() {
        return x + width;
    }

    public double bottom() {
        return y + height;
    }

    public double area() {
        return width * height;
    }

    public boolean hasPositiveArea(double epsilon) {
        return width > epsilon && height > epsilon;
    }

    public boolean intersects(Rectangle2D other, double epsilon) {
        double intersectionWidth = Math.min(this.right(), other.right()) - Math.max(this.x, other.x);
        double intersectionHeight = Math.min(this.bottom(), other.bottom()) - Math.max(this.y, other.y);
        return intersectionWidth > epsilon && intersectionHeight > epsilon;
    }

    public Rectangle2D intersection(Rectangle2D other, double epsilon) {
        double left = Math.max(this.x, other.x);
        double top = Math.max(this.y, other.y);
        double right = Math.min(this.right(), other.right());
        double bottom = Math.min(this.bottom(), other.bottom());

        double intersectionWidth = right - left;
        double intersectionHeight = bottom - top;

        if (intersectionWidth <= epsilon || intersectionHeight <= epsilon) {
            return null;
        }
        return new Rectangle2D(left, top, intersectionWidth, intersectionHeight);
    }

    public boolean contains(Rectangle2D other, double epsilon) {
        return other.x + epsilon >= this.x
                && other.y + epsilon >= this.y
                && other.right() <= this.right() + epsilon
                && other.bottom() <= this.bottom() + epsilon;
    }
}

package com.demo.adventure.domain.kernel;

import com.demo.adventure.domain.model.Rectangle2D;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ContainerPacker {

    private static final double EPSILON = 1e-9;

    private ContainerPacker() {}

    public record Placement(double x, double y, double width, double height, boolean rotated) {
        public Rectangle2D asRectangle() {
            return new Rectangle2D(x, y, width, height);
        }
    }

    public static Optional<Placement> place(double width, double height, List<Rectangle2D> occupiedRectangles) {
        if (width <= EPSILON || height <= EPSILON) {
            return Optional.empty();
        }
        if (width > 1.0 + EPSILON || height > 1.0 + EPSILON) {
            return Optional.empty();
        }

        List<Rectangle2D> freeRectangles = new ArrayList<>();
        freeRectangles.add(new Rectangle2D(0.0, 0.0, 1.0, 1.0));

        List<Rectangle2D> sortedOccupied = new ArrayList<>(occupiedRectangles);
        sortedOccupied.sort(Comparator
                .comparingDouble(Rectangle2D::y)
                .thenComparingDouble(Rectangle2D::x)
                .thenComparingDouble(Rectangle2D::height)
                .thenComparingDouble(Rectangle2D::width));

        for (Rectangle2D occupied : sortedOccupied) {
            freeRectangles = subtractOccupied(freeRectangles, occupied);
            freeRectangles = pruneContained(freeRectangles);
        }

        Candidate bestCandidate = null;

        // Orientation 1: not rotated
        bestCandidate = chooseBestCandidate(bestCandidate, freeRectangles, width, height, false);

        // Orientation 2: rotated (swap)
        if (Math.abs(width - height) > EPSILON) {
            bestCandidate = chooseBestCandidate(bestCandidate, freeRectangles, height, width, true);
        }

        if (bestCandidate == null) {
            return Optional.empty();
        }

        return Optional.of(new Placement(
                bestCandidate.placementX,
                bestCandidate.placementY,
                bestCandidate.placementWidth,
                bestCandidate.placementHeight,
                bestCandidate.rotated
        ));
    }

    private static Candidate chooseBestCandidate(
            Candidate currentBest,
            List<Rectangle2D> freeRectangles,
            double candidateWidth,
            double candidateHeight,
            boolean rotated
    ) {
        for (Rectangle2D free : freeRectangles) {
            if (free.width() + EPSILON < candidateWidth || free.height() + EPSILON < candidateHeight) {
                continue;
            }

            // Fixed anchor: top-left of the free rectangle
            double placementX = free.x();
            double placementY = free.y();

            double leftoverArea = free.area() - (candidateWidth * candidateHeight);
            double shortSideFit = Math.min(free.width() - candidateWidth, free.height() - candidateHeight);

            Candidate candidate = new Candidate(
                    placementX, placementY,
                    candidateWidth, candidateHeight,
                    rotated,
                    leftoverArea,
                    shortSideFit
            );

            if (currentBest == null || candidate.isBetterThan(currentBest)) {
                currentBest = candidate;
            }
        }
        return currentBest;
    }

    private static List<Rectangle2D> subtractOccupied(List<Rectangle2D> freeRectangles, Rectangle2D occupied) {
        List<Rectangle2D> next = new ArrayList<>();

        for (Rectangle2D free : freeRectangles) {
            Rectangle2D intersection = free.intersection(occupied, EPSILON);
            if (intersection == null) {
                next.add(free);
                continue;
            }

            // Split free by removing intersection (no merge, only split)
            // Top strip
            Rectangle2D top = new Rectangle2D(
                    free.x(),
                    free.y(),
                    free.width(),
                    intersection.y() - free.y()
            );
            addIfPositive(next, top);

            // Bottom strip
            Rectangle2D bottom = new Rectangle2D(
                    free.x(),
                    intersection.bottom(),
                    free.width(),
                    free.bottom() - intersection.bottom()
            );
            addIfPositive(next, bottom);

            // Left strip
            Rectangle2D left = new Rectangle2D(
                    free.x(),
                    intersection.y(),
                    intersection.x() - free.x(),
                    intersection.height()
            );
            addIfPositive(next, left);

            // Right strip
            Rectangle2D right = new Rectangle2D(
                    intersection.right(),
                    intersection.y(),
                    free.right() - intersection.right(),
                    intersection.height()
            );
            addIfPositive(next, right);
        }

        return next;
    }

    private static void addIfPositive(List<Rectangle2D> rectangles, Rectangle2D rectangle) {
        if (rectangle.hasPositiveArea(EPSILON)) {
            rectangles.add(rectangle);
        }
    }

    private static List<Rectangle2D> pruneContained(List<Rectangle2D> rectangles) {
        List<Rectangle2D> pruned = new ArrayList<>(rectangles);

        for (int i = 0; i < pruned.size(); i++) {
            Rectangle2D a = pruned.get(i);
            for (int j = pruned.size() - 1; j >= 0; j--) {
                if (i == j) {
                    continue;
                }
                Rectangle2D b = pruned.get(j);
                if (b.contains(a, EPSILON)) {
                    // remove a (contained in b)
                    pruned.remove(i);
                    i--;
                    break;
                }
            }
        }

        return pruned;
    }

    private static final class Candidate {
        private final double placementX;
        private final double placementY;
        private final double placementWidth;
        private final double placementHeight;
        private final boolean rotated;
        private final double leftoverArea;
        private final double shortSideFit;

        private Candidate(
                double placementX,
                double placementY,
                double placementWidth,
                double placementHeight,
                boolean rotated,
                double leftoverArea,
                double shortSideFit
        ) {
            this.placementX = placementX;
            this.placementY = placementY;
            this.placementWidth = placementWidth;
            this.placementHeight = placementHeight;
            this.rotated = rotated;
            this.leftoverArea = leftoverArea;
            this.shortSideFit = shortSideFit;
        }

        private boolean isBetterThan(Candidate other) {
            int compareLeftover = Double.compare(this.leftoverArea, other.leftoverArea);
            if (compareLeftover != 0) {
                return compareLeftover < 0;
            }

            int compareShortSide = Double.compare(this.shortSideFit, other.shortSideFit);
            if (compareShortSide != 0) {
                return compareShortSide < 0;
            }

            int compareY = Double.compare(this.placementY, other.placementY);
            if (compareY != 0) {
                return compareY < 0;
            }

            int compareX = Double.compare(this.placementX, other.placementX);
            if (compareX != 0) {
                return compareX < 0;
            }

            // Final tie-breaker: prefer not rotated
            if (this.rotated != other.rotated) {
                return !this.rotated;
            }

            return false;
        }
    }
}

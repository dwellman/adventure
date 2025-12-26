package com.demo.adventure.pack;

import com.demo.adventure.domain.kernel.ContainerPacker;
import com.demo.adventure.domain.model.Rectangle2D;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerPackerDeskSimulationTest {

    @Test
    void packsDeskItemsInInsertionOrder() {
        List<Rectangle2D> occupied = new ArrayList<>();

        // laptop 0.55 x 0.36
        occupied.add(placeOrFail(occupied, 0.55, 0.36).asRectangle());

        // notebook 0.25 x 0.33
        occupied.add(placeOrFail(occupied, 0.25, 0.33).asRectangle());

        // keyboard 0.65 x 0.12
        occupied.add(placeOrFail(occupied, 0.65, 0.12).asRectangle());

        // mouse 0.12 x 0.08 (rotation allowed)
        occupied.add(placeOrFail(occupied, 0.12, 0.08).asRectangle());

        // mug 0.10 x 0.12
        occupied.add(placeOrFail(occupied, 0.10, 0.12).asRectangle());

        // pen cup 0.12 x 0.12
        occupied.add(placeOrFail(occupied, 0.12, 0.12).asRectangle());

        // phone 0.10 x 0.20 (rotation allowed)
        occupied.add(placeOrFail(occupied, 0.10, 0.20).asRectangle());

        // paper stack 0.20 x 0.15
        occupied.add(placeOrFail(occupied, 0.20, 0.15).asRectangle());

        assertNoOverlaps(occupied);
    }

    private static ContainerPacker.Placement placeOrFail(List<Rectangle2D> occupied, double width, double height) {
        return ContainerPacker.place(width, height, occupied)
                .orElseThrow(() -> new AssertionError("Expected placement, got NO_FIT for " + width + "x" + height));
    }

    private static void assertNoOverlaps(List<Rectangle2D> rectangles) {
        for (int i = 0; i < rectangles.size(); i++) {
            for (int j = i + 1; j < rectangles.size(); j++) {
                Rectangle2D a = rectangles.get(i);
                Rectangle2D b = rectangles.get(j);
                assertFalse(a.intersects(b, 1e-9), "Rectangles overlap: " + a + " vs " + b);
            }
        }
    }
}

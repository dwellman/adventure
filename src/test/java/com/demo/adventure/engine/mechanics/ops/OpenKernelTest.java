package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenKernelTest {

    @Test
    void opensClosedThing() {
        Item drawer = item("Drawer", "A drawer");
        drawer.setKey("false");

        String result = Open.open(drawer);

        assertEquals("You open Drawer.", result);
        assertTrue(drawer.isOpen());
        assertEquals("true", drawer.getKey());
    }

    @Test
    void scenarioDeskDrawer() {
        Item desk = item("Desk", "Top of the desk");
        desk.setKey("true");

        Item drawer = item("Center drawer", "A shallow drawer");
        drawer.setKey("false");
        drawer.setVisible(true);

        Item key = item("Key", "A small key");
        key.setVisible(true);
        Rectangle2D placement = new Rectangle2D(0.0, 0.0, 0.1, 0.1);

        String lookDesk = Look.look(desk, List.of(drawer), List.of());
        assertEquals(String.join("\n",
                "Desk",
                "Fixtures: 1. Center drawer (closed).",
                "Inside: 0 items, 0% full."), lookDesk);

        String describeDesk = Describe.describe(desk, List.of(drawer), List.of());
        assertEquals(String.join("\n",
                "Desk",
                "Top of the desk",
                "Fixtures: 1.",
                "- Center drawer (closed): A shallow drawer",
                "Inside: 0 items, 0% full."), describeDesk);

        String openDrawer = Open.open(drawer);
        assertEquals("You open Center drawer.", openDrawer);
        assertTrue(drawer.isOpen());

        String lookDrawer = Look.look(
                drawer,
                List.of(),
                List.of(new Look.ContainerEntry(key, placement))
        );
        assertEquals(String.join("\n",
                "Center drawer",
                "Fixtures: 0.",
                "Inside: 1 items, 1% full. Key."), lookDrawer);
    }

    private static Item item(String label, String description) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(description)
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
    }
}

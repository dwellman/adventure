package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class CloseTest {

    @Test
    void closesOpenThing() {
        Item drawer = item("Drawer", "A drawer");
        drawer.setKey("true");

        String result = Close.close(drawer);

        assertEquals("You close Drawer.", result);
        assertFalse(drawer.isOpen());
        assertEquals("false", drawer.getKey());
    }

    @Test
    void scenarioCloseHidesContentsInLook() {
        Item drawer = item("Center drawer", "A shallow drawer");
        drawer.setKey("true");
        drawer.setVisible(true);

        Item key = item("Key", "A small key");
        key.setVisible(true);
        Rectangle2D placement = new Rectangle2D(0.0, 0.0, 0.1, 0.1);

        String lookOpen = Look.look(
                drawer,
                List.of(),
                List.of(new Look.ContainerEntry(key, placement))
        );
        assertEquals(String.join("\n",
                "Center drawer",
                "Fixtures: 0.",
                "Inside: 1 items, 1% full. Key."), lookOpen);

        String closeResult = Close.close(drawer);
        assertEquals("You close Center drawer.", closeResult);
        assertFalse(drawer.isOpen());

        String lookClosed = Look.look(
                drawer,
                List.of(),
                List.of(new Look.ContainerEntry(key, placement))
        );
        assertEquals(String.join("\n",
                "Center drawer",
                "Fixtures: 0.",
                "Inside: (closed)"), lookClosed);
    }

    private static Item item(String label, String description) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(description)
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
    }
}

package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SearchTest {

    @Test
    void targetClosed() {
        Item target = item("Chest");
        target.setKey("false");

        String search = Search.search(target, t -> List.of(), t -> List.of());

        assertEquals(String.join("\n",
                "You search Chest.",
                "Searched: 0.",
                "Skipped: 1. Chest.",
                "Found: 0 visible, 0 hidden."), search);
    }

    @Test
    void fixtureRecursionDepthFirst() {
        Item target = item("Cabinet");
        target.setKey("true");
        Item shelf = item("Shelf");
        shelf.setKey("true");
        Item box = item("Box");
        box.setKey("true");

        Map<Thing, List<Thing>> fixtures = new HashMap<>();
        fixtures.put(target, List.of(shelf));
        fixtures.put(shelf, List.of(box));
        fixtures.put(box, List.of());

        String search = Search.search(target, t -> fixtures.getOrDefault(t, List.of()), t -> List.of());

        assertEquals(String.join("\n",
                "You search Cabinet.",
                "Searched: 3. Cabinet, Shelf, Box.",
                "Skipped: 0.",
                "Found: 0 visible, 0 hidden."), search);
    }

    @Test
    void hiddenFixtureSkippedEntirely() {
        Item target = item("Table");
        target.setKey("true");
        Item hiddenDrawer = item("Hidden Drawer");
        hiddenDrawer.setVisible(false);
        hiddenDrawer.setKey("true");

        Map<Thing, List<Thing>> fixtures = new HashMap<>();
        fixtures.put(target, List.of(hiddenDrawer));

        Map<Thing, List<Look.ContainerEntry>> entries = new HashMap<>();
        entries.put(hiddenDrawer, List.of(
                new Look.ContainerEntry(new ItemBuilder()
                        .withLabel("Coin")
                        .withDescription("Coin")
                        .withOwnerId(KernelRegistry.MILIARIUM)
                        .build(), new Rectangle2D(0.1, 0.1, 0.1, 0.1))
        ));

        String search = Search.search(target, t -> fixtures.getOrDefault(t, List.of()), t -> entries.getOrDefault(t, List.of()));

        assertEquals(String.join("\n",
                "You search Table.",
                "Searched: 1. Table.",
                "Skipped: 0.",
                "Found: 0 visible, 0 hidden."), search);
    }

    @Test
    void closedFixtureRecordedSkippedNoInspection() {
        Item target = item("Desk");
        target.setKey("true");
        Item drawer = item("Drawer");
        drawer.setKey("false");

        Map<Thing, List<Thing>> fixtures = new HashMap<>();
        fixtures.put(target, List.of(drawer));

        Map<Thing, List<Look.ContainerEntry>> entries = new HashMap<>();
        entries.put(drawer, List.of(
                new Look.ContainerEntry(item("Key"), new Rectangle2D(0.1, 0.1, 0.1, 0.1))
        ));

        String search = Search.search(target, t -> fixtures.getOrDefault(t, List.of()), t -> entries.getOrDefault(t, List.of()));

        assertEquals(String.join("\n",
                "You search Desk.",
                "Searched: 1. Desk.",
                "Skipped: 1. Drawer.",
                "Found: 0 visible, 0 hidden."), search);
    }

    @Test
    void visibleAndHiddenResultsCounted() {
        Item target = item("Locker");
        target.setKey("true");
        Item bin = item("Bin");
        bin.setKey("true");

        Item visibleItem = item("Gem");
        Item hiddenItem = item("Dust");
        hiddenItem.setVisible(false);

        Map<Thing, List<Thing>> fixtures = new HashMap<>();
        fixtures.put(target, List.of(bin));
        fixtures.put(bin, List.of());

        Map<Thing, List<Look.ContainerEntry>> entries = new HashMap<>();
        entries.put(bin, List.of(
                new Look.ContainerEntry(visibleItem, new Rectangle2D(0.2, 0.2, 0.1, 0.1)),
                new Look.ContainerEntry(hiddenItem, null)
        ));

        String search = Search.search(target, t -> fixtures.getOrDefault(t, List.of()), t -> entries.getOrDefault(t, List.of()));

        assertEquals(String.join("\n",
                "You search Locker.",
                "Searched: 2. Locker, Bin.",
                "Skipped: 0.",
                "Found: 1 visible, 1 hidden.",
                "Visible: Gem.",
                "Hidden: Dust."), search);
    }

    @Test
    void truncatesSamples() {
        Item root = item("Root");
        root.setKey("true");
        Item a = item("A");
        a.setKey("true");
        Item b = item("B");
        b.setKey("true");
        Item c = item("C");
        c.setKey("true");
        Item d = item("D");
        d.setKey("true");

        Map<Thing, List<Thing>> fixtures = new HashMap<>();
        fixtures.put(root, List.of(a, b));
        fixtures.put(a, List.of(c, d));
        fixtures.put(b, List.of());
        fixtures.put(c, List.of());
        fixtures.put(d, List.of());

        Item i1 = item("Item1");
        Item i2 = item("Item2");
        Item i3 = item("Item3");
        Item i4 = item("Item4");
        Item i5 = item("Item5");

        Map<Thing, List<Look.ContainerEntry>> entries = new HashMap<>();
        entries.put(root, List.of(
                new Look.ContainerEntry(i1, new Rectangle2D(0.1, 0.1, 0.01, 0.01)),
                new Look.ContainerEntry(i2, new Rectangle2D(0.2, 0.1, 0.01, 0.01)),
                new Look.ContainerEntry(i3, new Rectangle2D(0.3, 0.1, 0.01, 0.01)),
                new Look.ContainerEntry(i4, new Rectangle2D(0.4, 0.1, 0.01, 0.01)),
                new Look.ContainerEntry(i5, new Rectangle2D(0.5, 0.1, 0.01, 0.01))
        ));

        String search = Search.search(root, t -> fixtures.getOrDefault(t, List.of()), t -> entries.getOrDefault(t, List.of()));

        assertEquals(String.join("\n",
                "You search Root.",
                "Searched: 5. Root, A, C, and 2 more.",
                "Skipped: 0.",
                "Found: 5 visible, 0 hidden.",
                "Visible: Item1, Item2, Item3, and 2 more."), search);
    }

    @Test
    void cycleDetectionStopsRevisiting() {
        Item a = item("A");
        a.setKey("true");
        Item b = item("B");
        b.setKey("true");

        Map<Thing, List<Thing>> fixtures = new HashMap<>();
        fixtures.put(a, List.of(b));
        fixtures.put(b, List.of(a));

        String search = Search.search(a, t -> fixtures.getOrDefault(t, List.of()), t -> List.of());

        assertEquals(String.join("\n",
                "You search A.",
                "Searched: 2. A, B.",
                "Skipped: 0.",
                "Found: 0 visible, 0 hidden."), search);
    }

    private static Item item(String label) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(label + " description")
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
    }
}

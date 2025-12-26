package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DescribeTest {

    @Test
    void includesLabelAndDescription() {
        Item target = item("Chest", "A sturdy chest.");
        target.setKey("true");

        String describe = Describe.describe(target, List.of(), List.of());

        assertEquals(String.join("\n",
                "Chest",
                "A sturdy chest.",
                "Fixtures: 0.",
                "Inside: 0 items, 0% full."), describe);
    }

    @Test
    void closedTargetHidesContents() {
        Item target = item("Locker", "Metal locker");
        target.setKey("false");

        Item fixture = item("Handle", "A small handle");
        fixture.setKey("false");

        Item inside = item("Secret Note", "Top secret");

        String describe = Describe.describe(
                target,
                List.of(fixture),
                List.of(new Look.ContainerEntry(inside, new Rectangle2D(0.1, 0.1, 0.1, 0.1)))
        );

        assertEquals(String.join("\n",
                "Locker",
                "Metal locker",
                "Fixtures: 1.",
                "- Handle (closed): A small handle",
                "Inside: (closed)"), describe);
    }

    @Test
    void hiddenContainedIgnoredButCountsTowardFullness() {
        Item target = item("Bin", "Storage bin");
        target.setKey("true");

        Item hidden = item("Hidden Stone", "Not visible");
        hidden.setVisible(false);
        Item visible = item("Apple", "Fresh");

        String describe = Describe.describe(
                target,
                List.of(),
                List.of(
                        new Look.ContainerEntry(hidden, new Rectangle2D(0.0, 0.0, 0.5, 0.5)),
                        new Look.ContainerEntry(visible, null)
                )
        );

        assertEquals(String.join("\n",
                "Bin",
                "Storage bin",
                "Fixtures: 0.",
                "Inside: 1 items, 25% full.",
                "- Apple: Fresh"), describe);
    }

    @Test
    void truncatesFixturesAndItemsAndOrdersPlacedByCoordinates() {
        Item target = item("Workbench", "A busy bench");
        target.setKey("true");

        Item fixtureA = item("Drawer A", "Left drawer");
        fixtureA.setKey("true");
        Item fixtureB = item("Drawer B", "Middle drawer");
        fixtureB.setKey("false");
        Item fixtureC = item("Drawer C", "Right drawer");
        fixtureC.setKey("true");
        Item fixtureD = item("Drawer D", "Extra drawer");
        Item fixtureE = item("Drawer E", "Spare drawer");

        Item first = item("First", "Placed first by y/x");
        Item second = item("Second", "Placed second by y/x");
        Item third = item("Third", "Placed third by y/x");
        Item loose = item("Loose", "Unplaced");

        String describe = Describe.describe(
                target,
                List.of(fixtureA, fixtureB, fixtureC, fixtureD, fixtureE),
                List.of(
                        new Look.ContainerEntry(second, new Rectangle2D(0.2, 0.2, 0.05, 0.05)),
                        new Look.ContainerEntry(first, new Rectangle2D(0.8, 0.1, 0.05, 0.05)),
                        new Look.ContainerEntry(third, new Rectangle2D(0.9, 0.2, 0.05, 0.05)),
                        new Look.ContainerEntry(loose, null)
                )
        );

        assertEquals(String.join("\n",
                "Workbench",
                "A busy bench",
                "Fixtures: 5.",
                "- Drawer A (open): Left drawer",
                "- Drawer B (closed): Middle drawer",
                "- Drawer C (open): Right drawer",
                "- and 2 more.",
                "Inside: 4 items, 1% full.",
                "- First: Placed first by y/x",
                "- Second: Placed second by y/x",
                "- Third: Placed third by y/x",
                "- and 1 more."), describe);
    }

    private static Item item(String label, String description) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(description)
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
    }
}

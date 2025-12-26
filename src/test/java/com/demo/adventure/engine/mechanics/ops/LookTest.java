package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LookTest {

    @Test
    void labelOnlyDoesNotUseDescription() {
        Item target = item("Mystery Box");
        target.setDescription("This should not appear");
        target.setKey("false");

        String look = Look.look(target, List.of(), List.of());

        assertEquals(String.join("\n",
                "Mystery Box",
                "Fixtures: 0.",
                "Inside: (closed)"), look);
    }

    @Test
    void closedTargetHidesContentsEvenWhenPresent() {
        Item target = item("Crate");
        target.setKey("false");

        Item lid = item("Lid");
        lid.setKey("true");

        Item hiddenFixture = item("Secret Panel");
        hiddenFixture.setVisible(false);

        Item hiddenNote = item("Hidden Note");
        hiddenNote.setVisible(false);

        String look = Look.look(
                target,
                List.of(lid, hiddenFixture),
                List.of(new Look.ContainerEntry(hiddenNote, new Rectangle2D(0.0, 0.0, 0.2, 0.2))));

        assertEquals(String.join("\n",
                "Crate",
                "Fixtures: 1. Lid (open).",
                "Inside: (closed)"), look);
    }

    @Test
    void hiddenContainedNotListedButCountsTowardFullness() {
        Item target = item("Bin");
        target.setKey("true");

        Item hidden = item("Hidden Stone");
        hidden.setVisible(false);
        Item visible = item("Apple");

        String look = Look.look(
                target,
                List.of(),
                List.of(
                        new Look.ContainerEntry(hidden, new Rectangle2D(0.0, 0.0, 0.5, 0.5)),
                        new Look.ContainerEntry(visible, null)
                ));

        assertEquals(String.join("\n",
                "Bin",
                "Fixtures: 0.",
                "Inside: 1 items, 25% full. Apple."), look);
    }

    @Test
    void truncatesAndOrdersPlacedThenUnplaced() {
        Item target = item("Workbench");
        target.setKey("true");

        Item fixtureA = item("Drawer A");
        fixtureA.setKey("true");
        Item fixtureB = item("Drawer B");
        fixtureB.setKey("false");
        Item fixtureC = item("Drawer C");
        fixtureC.setKey("true");
        Item fixtureD = item("Drawer D");
        Item fixtureE = item("Drawer E");

        Item top = item("Top");
        Item northWest = item("NorthWest");
        Item northEast = item("NorthEast");
        Item south = item("South");
        Item loose = item("Loose");

        String look = Look.look(
                target,
                List.of(fixtureA, fixtureB, fixtureC, fixtureD, fixtureE),
                List.of(
                        new Look.ContainerEntry(top, new Rectangle2D(0.9, 0.05, 0.05, 0.05)),
                        new Look.ContainerEntry(northWest, new Rectangle2D(0.1, 0.1, 0.05, 0.05)),
                        new Look.ContainerEntry(northEast, new Rectangle2D(0.2, 0.1, 0.05, 0.05)),
                        new Look.ContainerEntry(south, new Rectangle2D(0.5, 0.4, 0.1, 0.1)),
                        new Look.ContainerEntry(loose, null)
                ));

        assertEquals(String.join("\n",
                "Workbench",
                "Fixtures: 5. Drawer A (open), Drawer B (closed), Drawer C (open), and 2 more.",
                "Inside: 5 items, 2% full. Top, NorthWest, NorthEast, and 2 more."), look);
    }

    private static Item item(String label) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(label + " description")
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
    }
}

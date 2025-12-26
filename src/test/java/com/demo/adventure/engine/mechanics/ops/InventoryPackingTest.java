package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InventoryPackingTest {

    @Test
    void takeIntoHandSucceedsWhenItFits() {
        Item actor = item("Actor");
        Item hand = item("Hand");
        hand.setKey("true");
        hand.withCapacity(1.0, 1.0, 1.0);
        Item apple = item("Apple").withSize(0.2, 0.2);

        Map<Thing, List<Thing>> fixturesByOwner = Map.of(actor, List.of(hand));
        Map<Thing, List<Look.ContainerEntry>> entries = new HashMap<>();
        entries.put(hand, new ArrayList<>());
        Item table = item("Table");
        table.setKey("true");
        entries.put(table, new ArrayList<>(List.of(new Look.ContainerEntry(apple, null))));

        String result = Move.moveToHand(apple, table, actor, fixturesByOwner::get, entries);

        assertThat(result).isEqualTo("You move Apple.");
        assertThat(entries.get(hand)).hasSize(1);
        assertThat(entries.get(hand).get(0).placement()).isNotNull();
        // Debug helper: Describe.describe(hand, List.of(), entries.get(hand));
    }

    @Test
    void moveFromHandToBackpackRespectsCapacity() {
        Item hand = item("Hand").withCapacity(1.0, 1.0, 1.0);
        hand.setKey("true");
        Item backpack = item("Backpack").withCapacity(0.3, 0.3, 0.3);
        backpack.setKey("true");
        Item coin = item("Coin").withSize(0.1, 0.1);
        Item brick = item("Brick").withSize(0.3, 0.3);

        Map<Thing, List<Look.ContainerEntry>> entries = new HashMap<>();
        entries.put(hand, new ArrayList<>(List.of(new Look.ContainerEntry(coin, null), new Look.ContainerEntry(brick, null))));
        entries.put(backpack, new ArrayList<>());

        String first = Move.move(coin, hand, backpack, entries);
        assertThat(first).isEqualTo("You move Coin.");
        assertThat(entries.get(backpack)).hasSize(1);
        // Debug helper: Describe.describe(backpack, List.of(), entries.get(backpack));

        String second = Move.move(brick, hand, backpack, entries);
        assertThat(second).isEqualTo("It doesn't fit in the Backpack.");
        assertThat(entries.get(backpack)).hasSize(1);
        // Debug helper: Describe.describe(backpack, List.of(), entries.get(backpack));
    }

    @Test
    void twoHandsCanHoldTwoItems() {
        Item actor = item("Actor");
        Item handLeft = item("Left Hand").withCapacity(1.0, 1.0, 1.0);
        handLeft.setKey("true");
        Item handRight = item("Right Hand").withCapacity(1.0, 1.0, 1.0);
        handRight.setKey("true");
        Item book = item("Book").withSize(0.2, 0.2);
        Item lantern = item("Lantern").withSize(0.2, 0.2);
        Item shelf = item("Shelf");
        shelf.setKey("true");

        Map<Thing, List<Thing>> fixtures = new HashMap<>();
        fixtures.put(actor, List.of(handLeft, handRight));
        Map<Thing, List<Look.ContainerEntry>> entries = new HashMap<>();
        entries.put(shelf, new ArrayList<>(List.of(new Look.ContainerEntry(book, null), new Look.ContainerEntry(lantern, null))));
        entries.put(handLeft, new ArrayList<>());
        entries.put(handRight, new ArrayList<>());

        String first = Move.moveToHand(book, shelf, actor, fixtures::get, entries);
        // swap order so second item goes to the other hand
        fixtures.put(actor, List.of(handRight, handLeft));
        String second = Move.moveToHand(lantern, shelf, actor, fixtures::get, entries);

        assertThat(first).isEqualTo("You move Book.");
        assertThat(second).isEqualTo("You move Lantern.");
        assertThat(entries.get(handLeft)).hasSize(1);
        assertThat(entries.get(handRight)).hasSize(1);
        // Debug helper: Describe.describe(handLeft, List.of(), entries.get(handLeft));
        // Debug helper: Describe.describe(handRight, List.of(), entries.get(handRight));
    }

    private static Item item(String label) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(label)
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
    }
}

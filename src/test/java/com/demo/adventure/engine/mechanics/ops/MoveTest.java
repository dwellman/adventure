package com.demo.adventure.engine.mechanics.ops;

import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.ItemBuilder;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class MoveTest {

    @Test
    void unlockingRightDrawerWithKeyExpression() {
        Item desk = item("Desk", "Top of the desk");
        desk.setKey("true");

        Item leftDrawer = item("Left drawer", "Left drawer");
        leftDrawer.setKey("false");
        leftDrawer.setVisible(true);

        Item centerDrawer = item("Center drawer", "Center drawer");
        centerDrawer.setKey("false");
        centerDrawer.setVisible(true);

        Item rightDrawer = item("Right drawer", "Right drawer");
        rightDrawer.setKey("has(\"key\")");
        rightDrawer.setVisible(true);

        Item key = item("Key", "Small key");
        key.setVisible(true);

        Item book = item("Book", "A plain book");
        book.setVisible(true);
        book.setKey("false");

        Item actor = item("Actor", "Actor");
        actor.setKey("true");
        actor.setVisible(true);
        Item hand = item("Hand", "Hand");
        hand.setKey("true");
        hand.setVisible(true);

        Map<Thing, List<Thing>> fixturesByOwner = new HashMap<>();
        fixturesByOwner.put(actor, List.of(hand));

        Map<Thing, List<Look.ContainerEntry>> entriesByOwner = new HashMap<>();
        entriesByOwner.put(leftDrawer, new ArrayList<>(List.of(new Look.ContainerEntry(key, null))));
        entriesByOwner.put(centerDrawer, new ArrayList<>(List.of(new Look.ContainerEntry(book, null))));
        entriesByOwner.put(rightDrawer, new ArrayList<>());
        entriesByOwner.put(hand, new ArrayList<>());
        entriesByOwner.put(desk, new ArrayList<>());

        KeyExpressionEvaluator.HasResolver resolver = label -> hasItemWithLabel(entriesByOwner.get(hand), label);
        KeyExpressionEvaluator.HasResolver previous = KeyExpressionEvaluator.getDefaultHasResolver();
        KeyExpressionEvaluator.setDefaultHasResolver(resolver);
        try {
            assertFalse(rightDrawer.isOpen(), "Right drawer should start locked");

            Open.open(leftDrawer);
            assertTrue(leftDrawer.isOpen(), "Left drawer should open");

            String movedKey = Move.moveToHand(key, leftDrawer, actor, fixturesByOwner::get, entriesByOwner);
            assertEquals("You move Key.", movedKey);
            assertTrue(hasItemWithLabel(entriesByOwner.get(hand), "Key"));

            assertTrue(rightDrawer.isOpen(), "Right drawer should unlock when the key is held");

            Open.open(centerDrawer);
            String movedBook = Move.move(book, centerDrawer, desk, entriesByOwner);
            assertEquals("You move Book.", movedBook);
            Open.open(book);
            assertTrue(rightDrawer.isOpen());
            assertTrue(book.isOpen());
            assertOwnedBy(book, desk, entriesByOwner, leftDrawer, centerDrawer, rightDrawer, hand, desk);
        } finally {
            KeyExpressionEvaluator.setDefaultHasResolver(previous);
        }
    }

    @Test
    void cannotMoveFromClosedDrawer() {
        Item drawer = item("Center drawer", "A shallow drawer");
        drawer.setKey("false");
        Item book = item("Book", "A book");
        book.setVisible(true);
        Item hand = item("Hand", "Hand");
        hand.setKey("true");

        Map<Thing, List<Look.ContainerEntry>> entriesByOwner = new HashMap<>();
        entriesByOwner.put(drawer, new ArrayList<>(List.of(
                new Look.ContainerEntry(book, new Rectangle2D(0.0, 0.0, 0.1, 0.1))
        )));
        entriesByOwner.put(hand, new ArrayList<>());

        String result = Move.move(book, drawer, hand, entriesByOwner);

        assertEquals("The Center drawer is closed.", result);
        assertEquals(1, entriesByOwner.get(drawer).size());
        assertTrue(entriesByOwner.get(hand).isEmpty());
        assertFalse(drawer.isOpen());
    }

    @Test
    void scenarioDeskDrawerMoveToHand() {
        Item desk = item("Desk", "Top of the desk");
        desk.setKey("true");

        Item drawer = item("Center drawer", "A shallow drawer");
        drawer.setKey("false");
        drawer.setVisible(true);

        Item book = item("Book", "A plain book");
        book.setVisible(true);
        book.setKey("false");

        Item actor = item("Actor", "Actor");
        actor.setKey("true");
        actor.setVisible(true);
        Item hand = item("Hand", "Hand");
        hand.setKey("true");
        hand.setVisible(true);

        Rectangle2D placement = new Rectangle2D(0.0, 0.0, 0.1, 0.1);

        Map<Thing, List<Thing>> fixturesByOwner = new HashMap<>();
        fixturesByOwner.put(actor, List.of(hand));

        Map<Thing, List<Look.ContainerEntry>> entriesByOwner = new HashMap<>();
        entriesByOwner.put(drawer, new ArrayList<>(List.of(new Look.ContainerEntry(book, placement))));
        entriesByOwner.put(desk, new ArrayList<>());
        Item note = item("Note", "A handwritten note: \"Don’t trust the green shimmer.\"");
        note.setVisible(true);
        entriesByOwner.put(book, new ArrayList<>(List.of(new Look.ContainerEntry(note, new Rectangle2D(0.0, 0.0, 0.1, 0.1)))));
        entriesByOwner.put(hand, new ArrayList<>());

        List<String> transcript = new ArrayList<>();

        String lookActor = Look.look(actor, fixturesByOwner.get(actor), List.of());
        assertEquals(String.join("\n",
                "Actor",
                "Fixtures: 1. Hand (open).",
                "Inside: 0 items, 0% full."), lookActor);
        transcript.add("== A) LOOK actor ==\n" + lookActor);

        String lookDesk = Look.look(desk, List.of(drawer), List.of());
        assertEquals(String.join("\n",
                "Desk",
                "Fixtures: 1. Center drawer (closed).",
                "Inside: 0 items, 0% full."), lookDesk);
        transcript.add("== B) LOOK desk ==\n" + lookDesk);

        String describeDesk = Describe.describe(desk, List.of(drawer), List.of());
        assertEquals(String.join("\n",
                "Desk",
                "Top of the desk",
                "Fixtures: 1.",
                "- Center drawer (closed): A shallow drawer",
                "Inside: 0 items, 0% full."), describeDesk);
        transcript.add("== C) DESCRIBE desk ==\n" + describeDesk);
        assertOwnedBy(book, drawer, entriesByOwner, drawer, hand, desk);

        String moveClosed = Move.moveToHand(book, drawer, actor, fixturesByOwner::get, entriesByOwner);
        assertEquals("The Center drawer is closed.", moveClosed);
        transcript.add("== D) MOVE to hand (drawer closed) ==\n" + moveClosed);
        assertOwnedBy(book, drawer, entriesByOwner, drawer, hand, desk);

        String openDrawer = Open.open(drawer);
        assertEquals("You open Center drawer.", openDrawer);
        assertTrue(drawer.isOpen());
        transcript.add("== E) OPEN drawer ==\n" + openDrawer);

        String lookDrawer = Look.look(drawer, List.of(), entriesByOwner.get(drawer));
        assertEquals(String.join("\n",
                "Center drawer",
                "Fixtures: 0.",
                "Inside: 1 items, 1% full. Book."), lookDrawer);
        transcript.add("== F) LOOK drawer ==\n" + lookDrawer);

        String moveResult = Move.moveToHand(book, drawer, actor, fixturesByOwner::get, entriesByOwner);
        assertEquals("You move Book.", moveResult);
        assertTrue(entriesByOwner.get(drawer).isEmpty());
        assertEquals(1, entriesByOwner.get(hand).size());
        assertEquals(book, entriesByOwner.get(hand).get(0).thing());
        assertNull(entriesByOwner.get(hand).get(0).placement());
        transcript.add("== G) MOVE to hand ==\n" + moveResult);
        assertOwnedBy(book, hand, entriesByOwner, drawer, hand, desk);

        String lookHand = Look.look(hand, List.of(), entriesByOwner.get(hand));
        assertEquals(String.join("\n",
                "Hand",
                "Fixtures: 0.",
                "Inside: 1 items, 0% full. Book."), lookHand);
        transcript.add("== H) LOOK hand ==\n" + lookHand);

        String moveToDesk = Move.move(book, hand, desk, entriesByOwner);
        assertEquals("You move Book.", moveToDesk);
        assertTrue(entriesByOwner.get(hand).isEmpty());
        assertEquals(1, entriesByOwner.get(desk).size());
        assertEquals(book, entriesByOwner.get(desk).get(0).thing());
        assertNull(entriesByOwner.get(desk).get(0).placement());
        transcript.add("== I) MOVE hand -> desk ==\n" + moveToDesk);
        assertOwnedBy(book, desk, entriesByOwner, drawer, hand, desk);

        String lookDeskAfter = Look.look(desk, List.of(drawer), entriesByOwner.get(desk));
        assertEquals(String.join("\n",
                "Desk",
                "Fixtures: 1. Center drawer (open).",
                "Inside: 1 items, 0% full. Book."), lookDeskAfter);
        transcript.add("== J) LOOK desk after ==\n" + lookDeskAfter);

        String describeBookClosed = Describe.describe(book, List.of(), entriesByOwner.get(book));
        assertEquals(String.join("\n",
                "Book",
                "A plain book",
                "Fixtures: 0.",
                "Inside: (closed)"), describeBookClosed);
        transcript.add("== K) DESCRIBE book (closed) ==\n" + describeBookClosed);

        String openBook = Open.open(book);
        assertEquals("You open Book.", openBook);
        assertTrue(book.isOpen());
        transcript.add("== L) OPEN book ==\n" + openBook);
        assertOwnedBy(book, desk, entriesByOwner, drawer, hand, desk);

        String describeBookOpen = Describe.describe(book, List.of(), entriesByOwner.get(book));
        assertEquals(String.join("\n",
                "Book",
                "A plain book",
                "Fixtures: 0.",
                "Inside: 1 items, 1% full.",
                "- Note: A handwritten note: \"Don’t trust the green shimmer.\""), describeBookOpen);
        transcript.add("== M) DESCRIBE book (open) ==\n" + describeBookOpen);

        String lookBook = Look.look(book, List.of(), entriesByOwner.get(book));
        assertEquals(String.join("\n",
                "Book",
                "Fixtures: 0.",
                "Inside: 1 items, 1% full. Note."), lookBook);
        transcript.add("== N) LOOK book ==\n" + lookBook);

        String transcriptText = String.join("\n\n", transcript) + "\n";
        // Debug helper: System.out.println(transcriptText);
        try {
            Files.createDirectories(Path.of("logs", "pow"));
            Files.writeString(
                    Path.of("logs", "pow", "POW-2025-12-13-Move-Desk-CenterDrawer-Hand-Book-Open-Note.txt"),
                    transcriptText
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Item item(String label, String description) {
        return new ItemBuilder()
                .withLabel(label)
                .withDescription(description)
                .withOwnerId(KernelRegistry.MILIARIUM)
                .build();
    }

    private static void assertOwnedBy(Thing thing, Thing expectedOwner, Map<Thing, List<Look.ContainerEntry>> entriesByOwner, Thing... candidateOwners) {
        assertAppearsInExactlyOneOwner(thing, entriesByOwner, candidateOwners);
        boolean found = false;
        List<Look.ContainerEntry> entries = entriesByOwner.get(expectedOwner);
        if (entries != null) {
            for (Look.ContainerEntry entry : entries) {
                if (entry != null && entry.thing() != null && sameThing(entry.thing(), thing)) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found, "Expected " + thing.getLabel() + " to be owned by " + label(expectedOwner));
    }

    private static void assertAppearsInExactlyOneOwner(Thing thing, Map<Thing, List<Look.ContainerEntry>> entriesByOwner, Thing... candidateOwners) {
        int count = 0;
        for (Thing owner : candidateOwners) {
            List<Look.ContainerEntry> entries = entriesByOwner.get(owner);
            if (entries == null) {
                continue;
            }
            for (Look.ContainerEntry entry : entries) {
                if (entry != null && entry.thing() != null && sameThing(entry.thing(), thing)) {
                    count++;
                    break;
                }
            }
        }
        assertEquals(1, count, "Expected exactly one owner for " + thing.getLabel());
    }

    private static boolean sameThing(Thing a, Thing b) {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return false;
    }

    private static boolean hasItemWithLabel(List<Look.ContainerEntry> entries, String label) {
        if (entries == null || label == null) {
            return false;
        }
        return entries.stream()
                .filter(Objects::nonNull)
                .map(Look.ContainerEntry::thing)
                .filter(Objects::nonNull)
                .anyMatch(t -> label.equalsIgnoreCase(t.getLabel()));
    }

    private static String label(Thing thing) {
        return thing == null ? "null" : thing.getLabel();
    }
}

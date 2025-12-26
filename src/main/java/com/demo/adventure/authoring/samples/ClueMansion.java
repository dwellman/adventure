package com.demo.adventure.authoring.samples;

import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.save.ActorRecipeBuilder;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.ItemRecipeBuilder;
import com.demo.adventure.domain.save.WorldRecipe;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static java.util.Map.entry;

/**
 * Deterministic definitions for the classic Clue mansion board.
 */
public final class ClueMansion {
    public static final long SEED = 1949L;
    public static final String PREAMBLE = "You are the detective in the Hall.";

    public static final UUID STUDY = id("plot:study");
    public static final UUID HALL = id("plot:hall");
    public static final UUID LOUNGE = id("plot:lounge");
    public static final UUID LIBRARY = id("plot:library");
    public static final UUID BILLIARD_ROOM = id("plot:billiard-room");
    public static final UUID DINING_ROOM = id("plot:dining-room");
    public static final UUID CONSERVATORY = id("plot:conservatory");
    public static final UUID BALLROOM = id("plot:ballroom");
    public static final UUID KITCHEN = id("plot:kitchen");
    public static final UUID CELLAR = id("plot:cellar");

    public static final UUID STUDY_HALL_CORRIDOR = id("plot:corridor-study-hall");
    public static final UUID HALL_LOUNGE_CORRIDOR = id("plot:corridor-hall-lounge");
    public static final UUID STUDY_LIBRARY_CORRIDOR = id("plot:corridor-study-library");
    public static final UUID HALL_BILLIARD_CORRIDOR = id("plot:corridor-hall-billiard");
    public static final UUID LOUNGE_DINING_CORRIDOR = id("plot:corridor-lounge-dining");
    public static final UUID LIBRARY_BILLIARD_CORRIDOR = id("plot:corridor-library-billiard");
    public static final UUID BILLIARD_DINING_CORRIDOR = id("plot:corridor-billiard-dining");
    public static final UUID LIBRARY_CONSERVATORY_CORRIDOR = id("plot:corridor-library-conservatory");
    public static final UUID BILLIARD_BALLROOM_CORRIDOR = id("plot:corridor-billiard-ballroom");
    public static final UUID DINING_KITCHEN_CORRIDOR = id("plot:corridor-dining-kitchen");
    public static final UUID CONSERVATORY_BALLROOM_CORRIDOR = id("plot:corridor-conservatory-ballroom");
    public static final UUID BALLROOM_KITCHEN_CORRIDOR = id("plot:corridor-ballroom-kitchen");

    public static final UUID STUDY_DESK = id("fixture:study-desk");
    public static final UUID STUDY_DESK_DRAWER = id("fixture:study-desk-drawer-1");

    public static final UUID DETECTIVE = id("actor:detective");

    public static final UUID REVOLVER = id("item:revolver");
    public static final UUID ROPE = id("item:rope");
    public static final UUID KNIFE = id("item:knife");
    public static final UUID CANDLESTICK = id("item:candlestick");
    public static final UUID LEAD_PIPE = id("item:lead-pipe");
    public static final UUID WRENCH = id("item:wrench");
    public static final UUID BASEMENT_KEY = id("item:basement-key");
    public static final UUID POCKET_WATCH = id("item:pocket-watch");

    private static final String REGION = "MANSION";

    private static final Map<UUID, String> PLOT_NAMES = Map.ofEntries(
            Map.entry(STUDY, "Study"),
            Map.entry(HALL, "Hall"),
            Map.entry(LOUNGE, "Lounge"),
            Map.entry(LIBRARY, "Library"),
            Map.entry(BILLIARD_ROOM, "Billiard Room"),
            Map.entry(DINING_ROOM, "Dining Room"),
            Map.entry(CONSERVATORY, "Conservatory"),
            Map.entry(BALLROOM, "Ballroom"),
            Map.entry(KITCHEN, "Kitchen"),
            Map.entry(CELLAR, "Cellar"),
            Map.entry(STUDY_HALL_CORRIDOR, "Corridor Study-Hall"),
            Map.entry(HALL_LOUNGE_CORRIDOR, "Corridor Hall-Lounge"),
            Map.entry(STUDY_LIBRARY_CORRIDOR, "Corridor Study-Library"),
            Map.entry(HALL_BILLIARD_CORRIDOR, "Corridor Hall-Billiard"),
            Map.entry(LOUNGE_DINING_CORRIDOR, "Corridor Lounge-Dining"),
            Map.entry(LIBRARY_BILLIARD_CORRIDOR, "Corridor Library-Billiard"),
            Map.entry(BILLIARD_DINING_CORRIDOR, "Corridor Billiard-Dining"),
            Map.entry(LIBRARY_CONSERVATORY_CORRIDOR, "Corridor Library-Conservatory"),
            Map.entry(BILLIARD_BALLROOM_CORRIDOR, "Corridor Billiard-Ballroom"),
            Map.entry(DINING_KITCHEN_CORRIDOR, "Corridor Dining-Kitchen"),
            Map.entry(CONSERVATORY_BALLROOM_CORRIDOR, "Corridor Conservatory-Ballroom"),
            Map.entry(BALLROOM_KITCHEN_CORRIDOR, "Corridor Ballroom-Kitchen")
    );

    private static final Map<UUID, Coord> PLOT_COORDS = Map.ofEntries(
            entry(STUDY, new Coord(0, 0)),
            entry(HALL, new Coord(2, 0)),
            entry(LOUNGE, new Coord(4, 0)),
            entry(STUDY_HALL_CORRIDOR, new Coord(1, 0)),
            entry(HALL_LOUNGE_CORRIDOR, new Coord(3, 0)),
            entry(LIBRARY, new Coord(0, 2)),
            entry(BILLIARD_ROOM, new Coord(2, 2)),
            entry(DINING_ROOM, new Coord(4, 2)),
            entry(STUDY_LIBRARY_CORRIDOR, new Coord(0, 1)),
            entry(HALL_BILLIARD_CORRIDOR, new Coord(2, 1)),
            entry(LOUNGE_DINING_CORRIDOR, new Coord(4, 1)),
            entry(LIBRARY_BILLIARD_CORRIDOR, new Coord(1, 2)),
            entry(BILLIARD_DINING_CORRIDOR, new Coord(3, 2)),
            entry(CONSERVATORY, new Coord(0, 4)),
            entry(BALLROOM, new Coord(2, 4)),
            entry(KITCHEN, new Coord(4, 4)),
            entry(LIBRARY_CONSERVATORY_CORRIDOR, new Coord(0, 3)),
            entry(BILLIARD_BALLROOM_CORRIDOR, new Coord(2, 3)),
            entry(DINING_KITCHEN_CORRIDOR, new Coord(4, 3)),
            entry(CONSERVATORY_BALLROOM_CORRIDOR, new Coord(1, 4)),
            entry(BALLROOM_KITCHEN_CORRIDOR, new Coord(3, 4)),
            entry(CELLAR, new Coord(2, 6))
    );

    private ClueMansion() {
    }

    /**
     * Deterministic world recipe for the Clue mansion board.
     */
    public static WorldRecipe worldRecipe() {
        List<WorldRecipe.PlotSpec> plots = List.of(
                plot(STUDY),
                plot(HALL),
                plot(LOUNGE),
                plot(LIBRARY),
                plot(BILLIARD_ROOM),
                plot(DINING_ROOM),
                plot(CONSERVATORY),
                plot(BALLROOM),
                plot(KITCHEN),
                plot(CELLAR),
                plot(STUDY_HALL_CORRIDOR),
                plot(HALL_LOUNGE_CORRIDOR),
                plot(STUDY_LIBRARY_CORRIDOR),
                plot(HALL_BILLIARD_CORRIDOR),
                plot(LOUNGE_DINING_CORRIDOR),
                plot(LIBRARY_BILLIARD_CORRIDOR),
                plot(BILLIARD_DINING_CORRIDOR),
                plot(LIBRARY_CONSERVATORY_CORRIDOR),
                plot(BILLIARD_BALLROOM_CORRIDOR),
                plot(DINING_KITCHEN_CORRIDOR),
                plot(CONSERVATORY_BALLROOM_CORRIDOR),
                plot(BALLROOM_KITCHEN_CORRIDOR)
        );

        List<WorldRecipe.GateSpec> gates = new ArrayList<>();
        // Corridor connections (explicitly bidirectional)
        addBidirectionalOpenGate(gates, STUDY, Direction.E, STUDY_HALL_CORRIDOR);
        addBidirectionalOpenGate(gates, HALL, Direction.W, STUDY_HALL_CORRIDOR);

        addBidirectionalOpenGate(gates, HALL, Direction.E, HALL_LOUNGE_CORRIDOR);
        addBidirectionalOpenGate(gates, LOUNGE, Direction.W, HALL_LOUNGE_CORRIDOR);

        addBidirectionalOpenGate(gates, STUDY, Direction.S, STUDY_LIBRARY_CORRIDOR);
        addBidirectionalOpenGate(gates, LIBRARY, Direction.N, STUDY_LIBRARY_CORRIDOR);

        addBidirectionalOpenGate(gates, HALL, Direction.S, HALL_BILLIARD_CORRIDOR);
        addBidirectionalOpenGate(gates, BILLIARD_ROOM, Direction.N, HALL_BILLIARD_CORRIDOR);

        addBidirectionalOpenGate(gates, LOUNGE, Direction.S, LOUNGE_DINING_CORRIDOR);
        addBidirectionalOpenGate(gates, DINING_ROOM, Direction.N, LOUNGE_DINING_CORRIDOR);

        addBidirectionalOpenGate(gates, LIBRARY, Direction.E, LIBRARY_BILLIARD_CORRIDOR);
        addBidirectionalOpenGate(gates, BILLIARD_ROOM, Direction.W, LIBRARY_BILLIARD_CORRIDOR);

        addBidirectionalOpenGate(gates, BILLIARD_ROOM, Direction.E, BILLIARD_DINING_CORRIDOR);
        addBidirectionalOpenGate(gates, DINING_ROOM, Direction.W, BILLIARD_DINING_CORRIDOR);

        addBidirectionalOpenGate(gates, LIBRARY, Direction.S, LIBRARY_CONSERVATORY_CORRIDOR);
        addBidirectionalOpenGate(gates, CONSERVATORY, Direction.N, LIBRARY_CONSERVATORY_CORRIDOR);

        addBidirectionalOpenGate(gates, BILLIARD_ROOM, Direction.S, BILLIARD_BALLROOM_CORRIDOR);
        addBidirectionalOpenGate(gates, BALLROOM, Direction.N, BILLIARD_BALLROOM_CORRIDOR);

        addBidirectionalOpenGate(gates, DINING_ROOM, Direction.S, DINING_KITCHEN_CORRIDOR);
        addBidirectionalOpenGate(gates, KITCHEN, Direction.N, DINING_KITCHEN_CORRIDOR);

        addBidirectionalOpenGate(gates, CONSERVATORY, Direction.E, CONSERVATORY_BALLROOM_CORRIDOR);
        addBidirectionalOpenGate(gates, BALLROOM, Direction.W, CONSERVATORY_BALLROOM_CORRIDOR);

        addBidirectionalOpenGate(gates, BALLROOM, Direction.E, BALLROOM_KITCHEN_CORRIDOR);
        addBidirectionalOpenGate(gates, KITCHEN, Direction.W, BALLROOM_KITCHEN_CORRIDOR);

        // Secret passages (explicitly bidirectional)
        addBidirectionalHiddenGate(gates, STUDY, Direction.SE, KITCHEN);
        addBidirectionalHiddenGate(gates, LOUNGE, Direction.SW, CONSERVATORY);

        // Cellar stair (locked, explicitly bidirectional)
        addBidirectionalLockedGate(gates, BILLIARD_ROOM, Direction.DOWN, CELLAR, "HAS(\"Basement Key\")");

        List<WorldRecipe.FixtureSpec> fixtures = List.of(
                new WorldRecipe.FixtureSpec(STUDY_DESK, "Study Desk", "An antique writing desk.", STUDY, true, java.util.Map.of()),
                new WorldRecipe.FixtureSpec(STUDY_DESK_DRAWER, "Study Desk Drawer 1", "Primary drawer for the desk.", STUDY_DESK, true, java.util.Map.of())
        );

        return new WorldRecipe(SEED, HALL, plots, gates, fixtures);
    }

    /**
     * Full game save (map + fixtures + items + actor) for the Clue mansion.
     */
    public static GameSave gameSave() {
        WorldRecipe recipe = worldRecipe();
        return new GameSave(SEED, HALL, PREAMBLE, recipe.plots(), recipe.gates(), recipe.fixtures(), items(), actors());
    }

    /**
     * Weapon and key placement for the mansion.
     */
    public static List<GameSave.ItemRecipe> items() {
        return List.of(
                item(REVOLVER, "Revolver", STUDY),
                item(ROPE, "Rope", LOUNGE),
                item(KNIFE, "Knife", KITCHEN),
                item(CANDLESTICK, "Candlestick", DINING_ROOM),
                item(LEAD_PIPE, "Lead Pipe", BILLIARD_ROOM),
                item(WRENCH, "Wrench", BALLROOM),
                item(BASEMENT_KEY, "Basement Key", STUDY_DESK_DRAWER),
                item(POCKET_WATCH, "Pocket Watch", DETECTIVE)
        );
    }

    /**
     * Actor roster for the mansion.
     */
    public static List<GameSave.ActorRecipe> actors() {
        return List.of(new ActorRecipeBuilder()
                .withId(DETECTIVE)
                .withName("Detective")
                .withDescription(PREAMBLE)
                .withOwnerId(HALL)
                .withVisible(true)
                .build());
    }

    private static WorldRecipe.PlotSpec plot(UUID id) {
        Coord coord = PLOT_COORDS.get(id);
        String name = PLOT_NAMES.get(id);
        return new WorldRecipe.PlotSpec(id, name, REGION, coord.x(), coord.y(), name + " plot");
    }

    private static WorldRecipe.GateSpec openGate(UUID from, Direction direction, UUID to) {
        return gate(from, direction, to, true, "true");
    }

    private static void addBidirectionalOpenGate(
            List<WorldRecipe.GateSpec> gates,
            UUID from,
            Direction direction,
            UUID to
    ) {
        gates.add(openGate(from, direction, to));
        gates.add(openGate(to, Direction.oppositeOf(direction), from));
    }

    private static WorldRecipe.GateSpec hiddenGate(UUID from, Direction direction, UUID to) {
        return gate(from, direction, to, false, "true");
    }

    private static void addBidirectionalHiddenGate(
            List<WorldRecipe.GateSpec> gates,
            UUID from,
            Direction direction,
            UUID to
    ) {
        gates.add(hiddenGate(from, direction, to));
        gates.add(hiddenGate(to, Direction.oppositeOf(direction), from));
    }

    private static WorldRecipe.GateSpec lockedGate(UUID from, Direction direction, UUID to, String keyString) {
        return gate(from, direction, to, true, keyString);
    }

    private static void addBidirectionalLockedGate(
            List<WorldRecipe.GateSpec> gates,
            UUID from,
            Direction direction,
            UUID to,
            String keyString
    ) {
        gates.add(lockedGate(from, direction, to, keyString));
        gates.add(lockedGate(to, Direction.oppositeOf(direction), from, keyString));
    }

    private static WorldRecipe.GateSpec gate(
            UUID from,
            Direction direction,
            UUID to,
            boolean visible,
            String keyString
    ) {
        String label = PLOT_NAMES.get(from) + " -> " + PLOT_NAMES.get(to);
        String description = "Path between " + PLOT_NAMES.get(from) + " and " + PLOT_NAMES.get(to);
        return new WorldRecipe.GateSpec(from, direction, to, visible, keyString, label, description);
    }

    private static GameSave.ItemRecipe item(UUID id, String label, UUID ownerId) {
        return new ItemRecipeBuilder()
                .withId(id)
                .withName(label)
                .withDescription(label)
                .withOwnerId(ownerId)
                .withVisible(true)
                .withFixture(false)
                .withKeyString("true")
                .withFootprint(0.1, 0.1)
                .withCapacity(0.0, 0.0)
                .withWeaponDamage(0L)
                .withArmorMitigation(0L)
                .withCells(java.util.Map.of())
                .build();
    }

    private static UUID id(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private record Coord(int x, int y) {
    }
}

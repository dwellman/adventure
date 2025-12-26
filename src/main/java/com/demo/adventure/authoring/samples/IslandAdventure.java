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
 * Deterministic save definition for the core Island adventure loop.
 */
public final class IslandAdventure {
    public static final long SEED = 20240615L;
    public static final String PREAMBLE = "You wake on the beach with a backpack.";

    // Plots
    public static final UUID WRECK_BEACH = id("plot:wreck-beach");
    public static final UUID TIDEPOOL_ROCKS = id("plot:tidepool-rocks");
    public static final UUID TREEHOUSE = id("plot:treehouse");
    public static final UUID BAMBOO_FOREST = id("plot:bamboo-forest");
    public static final UUID MONKEY_GROVE = id("plot:monkey-grove");
    public static final UUID CAVE_WEB = id("plot:cave-mouth");
    public static final UUID CAVE_BACK_CHAMBER = id("plot:cave-back-chamber");
    public static final UUID WATERFALL_POOL = id("plot:waterfall-pool");
    public static final UUID VOLCANO_PATH = id("plot:volcano-path");
    public static final UUID VOLCANO_ALTAR = id("plot:volcano-altar");
    public static final UUID PLANE_WRECK = id("plot:plane-wreck");

    // Fixtures
    public static final UUID TREEHOUSE_SKELETON = id("fixture:treehouse-skeleton");
    public static final UUID CAVE_WEB_BARRIER = id("fixture:spider-webs");
    public static final UUID CAVE_WALL_MAP = id("fixture:cave-wall-map");
    public static final UUID TIME_STONE_PEDESTAL = id("fixture:time-stone-pedestal");

    // Actors
    public static final UUID CASTAWAY = id("actor:castaway");
    public static final UUID MONKEY_TROOP = id("actor:chaos-monkey-troop");
    public static final UUID SCRATCH_GHOST = id("actor:scratch-ghost");

    // Items
    public static final UUID BACKPACK = id("item:canvas-backpack");
    public static final UUID DIGITAL_WATCH = id("item:digital-watch");
    public static final UUID DEAD_IPAD = id("item:dead-ipad");
    public static final UUID NOTEBOOK = id("item:notebook");
    public static final UUID PEN = id("item:pen");
    public static final UUID HATCHET = id("item:hatchet");
    public static final UUID LIT_TORCH = id("item:lit-torch");
    public static final UUID FLINT = id("item:flint");
    public static final UUID RIVER_STONE = id("item:river-stone");
    public static final UUID VINE_ROPE = id("item:vine-rope");
    public static final UUID PARACHUTE = id("item:parachute");
    public static final UUID CAVE_MAP_RUBBING = id("item:cave-map-rubbing");
    public static final UUID TIME_STONE = id("item:time-stone");
    public static final UUID BANANA_1 = id("item:banana-1");
    public static final UUID BANANA_2 = id("item:banana-2");
    public static final UUID BANANA_3 = id("item:banana-3");
    public static final UUID BANANA_4 = id("item:banana-4");
    public static final UUID BANANA_5 = id("item:banana-5");
    public static final UUID STICK = id("item:stick");
    public static final UUID RAGS = id("item:rags");
    public static final UUID KEROSENE = id("item:kerosene");

    private static final String REGION = "ISLAND";

    private static final Map<UUID, String> PLOT_NAMES = Map.ofEntries(
            entry(WRECK_BEACH, "Wreck Beach"),
            entry(TIDEPOOL_ROCKS, "Tidepool Rocks"),
            entry(TREEHOUSE, "Treehouse"),
            entry(BAMBOO_FOREST, "Bamboo Forest"),
            entry(MONKEY_GROVE, "Monkey Grove"),
            entry(CAVE_WEB, "Cave Mouth"),
            entry(CAVE_BACK_CHAMBER, "Cave Back Chamber"),
            entry(WATERFALL_POOL, "Waterfall Pool"),
            entry(VOLCANO_PATH, "Volcano Path"),
            entry(VOLCANO_ALTAR, "Volcano Altar"),
            entry(PLANE_WRECK, "Plane Wreck")
    );

    private static final Map<UUID, Coord> PLOT_COORDS = Map.ofEntries(
            entry(WRECK_BEACH, new Coord(0, 0)),
            entry(TIDEPOOL_ROCKS, new Coord(1, 0)),
            entry(TREEHOUSE, new Coord(0, 1)),
            entry(BAMBOO_FOREST, new Coord(1, 1)),
            entry(MONKEY_GROVE, new Coord(2, 1)),
            entry(CAVE_WEB, new Coord(0, 2)),
            entry(CAVE_BACK_CHAMBER, new Coord(0, 3)),
            entry(WATERFALL_POOL, new Coord(1, 2)),
            entry(VOLCANO_PATH, new Coord(2, 2)),
            entry(VOLCANO_ALTAR, new Coord(2, 3)),
            entry(PLANE_WRECK, new Coord(3, 2))
    );

    private IslandAdventure() {
    }

    /**
     * Deterministic world recipe for the Island escape scenario.
     */
    public static WorldRecipe worldRecipe() {
        // Grounding: canonical structured recipe for the island adventure; used by tests and exporters.
        List<WorldRecipe.PlotSpec> plots = List.of(
                plot(WRECK_BEACH),
                plot(TIDEPOOL_ROCKS),
                plot(TREEHOUSE),
                plot(BAMBOO_FOREST),
                plot(MONKEY_GROVE),
                plot(CAVE_WEB),
                plot(CAVE_BACK_CHAMBER),
                plot(WATERFALL_POOL),
                plot(VOLCANO_PATH),
                plot(VOLCANO_ALTAR),
                plot(PLANE_WRECK)
        );

        List<WorldRecipe.GateSpec> gates = new ArrayList<>();
        gates.add(openGate(WRECK_BEACH, Direction.E, TIDEPOOL_ROCKS));
        gates.add(openGate(TIDEPOOL_ROCKS, Direction.W, WRECK_BEACH));

        gates.add(openGate(WRECK_BEACH, Direction.N, TREEHOUSE));
        gates.add(openGate(TREEHOUSE, Direction.S, WRECK_BEACH));

        gates.add(openGate(TIDEPOOL_ROCKS, Direction.N, BAMBOO_FOREST));
        gates.add(openGate(BAMBOO_FOREST, Direction.S, TIDEPOOL_ROCKS));

        gates.add(openGate(TREEHOUSE, Direction.E, BAMBOO_FOREST));
        gates.add(openGate(BAMBOO_FOREST, Direction.W, TREEHOUSE));

        gates.add(openGate(TREEHOUSE, Direction.N, CAVE_WEB));
        gates.add(openGate(CAVE_WEB, Direction.S, TREEHOUSE));

        gates.add(lockedGate(CAVE_WEB, Direction.N, CAVE_BACK_CHAMBER, "HAS(\"Lit Torch\")"));
        gates.add(lockedGate(CAVE_BACK_CHAMBER, Direction.S, CAVE_WEB, "HAS(\"Lit Torch\")"));

        gates.add(openGate(CAVE_WEB, Direction.E, WATERFALL_POOL));
        gates.add(openGate(WATERFALL_POOL, Direction.W, CAVE_WEB));

        gates.add(openGate(BAMBOO_FOREST, Direction.E, MONKEY_GROVE));
        gates.add(openGate(MONKEY_GROVE, Direction.W, BAMBOO_FOREST));

        gates.add(openGate(MONKEY_GROVE, Direction.N, VOLCANO_PATH));
        gates.add(openGate(VOLCANO_PATH, Direction.S, MONKEY_GROVE));

        gates.add(openGate(WATERFALL_POOL, Direction.E, VOLCANO_PATH));
        gates.add(openGate(VOLCANO_PATH, Direction.W, WATERFALL_POOL));

        gates.add(openGate(VOLCANO_PATH, Direction.N, VOLCANO_ALTAR));
        gates.add(openGate(VOLCANO_ALTAR, Direction.S, VOLCANO_PATH));

        gates.add(openGate(VOLCANO_PATH, Direction.E, PLANE_WRECK));
        gates.add(openGate(PLANE_WRECK, Direction.W, VOLCANO_PATH));

        List<WorldRecipe.FixtureSpec> fixtures = List.of(
                fixture(TREEHOUSE_SKELETON, "Treehouse Skeleton", "The long-dead scientist slumped in the corner.", TREEHOUSE),
                fixture(CAVE_WEB_BARRIER, "Spider Webs", "Thick webs block the back chamber.", CAVE_WEB),
                fixture(CAVE_WALL_MAP, "Cave Wall Map", "Pirate markings carved into the stone.", CAVE_BACK_CHAMBER),
                fixture(TIME_STONE_PEDESTAL, "Time Stone Pedestal", "A black altar deep in the volcano.", VOLCANO_ALTAR)
        );

        return new WorldRecipe(SEED, WRECK_BEACH, plots, gates, fixtures);
    }

    /**
     * Full game save (map + fixtures + items + actors) for the Island loop.
     */
    public static GameSave gameSave() {
        WorldRecipe recipe = worldRecipe();
        return new GameSave(SEED, WRECK_BEACH, PREAMBLE, recipe.plots(), recipe.gates(), recipe.fixtures(), items(), actors());
    }

    /**
     * Item placement for the Island loop.
     */
    public static List<GameSave.ItemRecipe> items() {
        return List.of(
                item(BACKPACK, "Canvas Backpack", "A small canvas backpack.", CASTAWAY, true, false, "true"),
                item(DIGITAL_WATCH, "Digital Watch", "A modern watch with a subtle green blink.", CASTAWAY),
                item(DEAD_IPAD, "Dead iPad", "A completely drained tablet.", CASTAWAY),
                item(NOTEBOOK, "Notebook", "Loop-stable notebook.", CASTAWAY),
                item(PEN, "Pen", "Simple ballpoint pen.", CASTAWAY),
                item(HATCHET, "Hatchet", "Weathered hatchet under the treehouse bones.", TREEHOUSE),
                item(FLINT, "Flint", "Sharp flint shard for sparks.", WATERFALL_POOL),
                item(RIVER_STONE, "River Stone", "Smooth stone for striking flint.", WATERFALL_POOL),
                item(STICK, "Stick", "Sturdy stick for a torch spine.", WRECK_BEACH),
                item(RAGS, "Rags", "Torn cloth strips for wrapping a torch head.", WRECK_BEACH),
                item(KEROSENE, "Kerosene", "Pungent fuel for soaking a torch.", CAVE_WEB),
                item(BANANA_1, "Banana 1", "Fresh banana from the grove.", BAMBOO_FOREST),
                item(BANANA_2, "Banana 2", "Fresh banana from the grove.", BAMBOO_FOREST),
                item(BANANA_3, "Banana 3", "Fresh banana from the grove.", BAMBOO_FOREST),
                item(BANANA_4, "Banana 4", "Fresh banana from the grove.", BAMBOO_FOREST),
                item(BANANA_5, "Banana 5", "Fresh banana from the grove.", BAMBOO_FOREST),
                item(VINE_ROPE, "Vine Rope", "A coil of strong vines guarded by monkeys.", MONKEY_GROVE, false, false, "false"),
                item(CAVE_MAP_RUBBING, "Cave Map Rubbing", "Copied poem and map from the back wall.", CAVE_BACK_CHAMBER),
                item(PARACHUTE, "Parachute", "Silk parachute that can serve as a sail.", PLANE_WRECK),
                item(TIME_STONE, "Time Stone", "A cold green gem pulsing at the altar.", VOLCANO_ALTAR, false, false, "false")
        );
    }

    /**
     * Actor roster for the Island loop.
     */
    public static List<GameSave.ActorRecipe> actors() {
        return List.of(
                new ActorRecipeBuilder()
                        .withId(CASTAWAY)
                        .withName("Castaway")
                        .withDescription(PREAMBLE)
                        .withOwnerId(WRECK_BEACH)
                        .withVisible(true)
                        .withSkills(List.of())
                        .build(),
                new ActorRecipeBuilder()
                        .withId(MONKEY_TROOP)
                        .withName("Chaos Monkey Troop")
                        .withDescription("Five monkeys with green-stained teeth.")
                        .withOwnerId(MONKEY_GROVE)
                        .withVisible(true)
                        .withSkills(List.of())
                        .build(),
                new ActorRecipeBuilder()
                        .withId(SCRATCH_GHOST)
                        .withName("Scratch (Ghost)")
                        .withDescription("Faint green shimmer around the old scientist.")
                        .withOwnerId(TREEHOUSE)
                        .withVisible(true)
                        .withSkills(List.of())
                        .build()
        );
    }

    private static WorldRecipe.PlotSpec plot(UUID id) {
        Coord coord = PLOT_COORDS.get(id);
        String name = PLOT_NAMES.get(id);
        return new WorldRecipe.PlotSpec(id, name, REGION, coord.x(), coord.y(), name + " plot");
    }

    private static WorldRecipe.GateSpec openGate(UUID from, Direction direction, UUID to) {
        return gate(from, direction, to, true, "true");
    }

    private static WorldRecipe.GateSpec lockedGate(UUID from, Direction direction, UUID to, String keyString) {
        return gate(from, direction, to, true, keyString);
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

    private static WorldRecipe.FixtureSpec fixture(UUID id, String name, String description, UUID ownerId) {
        return new WorldRecipe.FixtureSpec(id, name, description, ownerId, true, java.util.Map.of());
    }

    private static GameSave.ItemRecipe item(UUID id, String label, UUID ownerId) {
        return item(id, label, label, ownerId, true, false, "true");
    }

    private static GameSave.ItemRecipe item(UUID id, String label, String description, UUID ownerId) {
        return item(id, label, description, ownerId, true, false, "true");
    }

    private static GameSave.ItemRecipe item(
            UUID id,
            String label,
            String description,
            UUID ownerId,
            boolean visible,
            boolean fixture,
            String keyString
    ) {
        return new ItemRecipeBuilder()
                .withId(id)
                .withName(label)
                .withDescription(description)
                .withOwnerId(ownerId)
                .withVisible(visible)
                .withFixture(fixture)
                .withKeyString(keyString)
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

package com.demo.adventure.authoring.save.io;

import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.save.ActorRecipeBuilder;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.ItemRecipeBuilder;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

final class GameSaveYamlAssembler {
    private static final int AUTO_GRID_WIDTH = 6;

    private GameSaveYamlAssembler() {
    }

    static GameSave assemble(GameSaveYamlInputs inputs) {
        Objects.requireNonNull(inputs, "inputs");

        List<WorldRecipe.PlotSpec> plots = new ArrayList<>();
        Map<String, Coord> autoCoords = deriveCoordinates(inputs.plots());
        for (PlotInput input : inputs.plots()) {
            Coord coord = autoCoords.get(input.key());
            int locationX = input.locationX() != null ? input.locationX() : coord.x();
            int locationY = input.locationY() != null ? input.locationY() : coord.y();
            plots.add(new WorldRecipe.PlotSpec(
                    input.id(),
                    input.name(),
                    input.region(),
                    locationX,
                    locationY,
                    input.description()
            ));
        }

        UUID startPlotId = requirePlot(inputs.startPlotKey(), inputs.plotKeys());

        List<WorldRecipe.GateSpec> gates = inputs.gates().stream()
                .map(input -> new WorldRecipe.GateSpec(
                        requirePlot(input.fromKey(), inputs.plotKeys()),
                        Direction.parse(input.direction()),
                        requirePlot(input.toKey(), inputs.plotKeys()),
                        input.visible(),
                        input.keyString(),
                        input.label(),
                        input.description()
                ))
                .toList();

        List<WorldRecipe.FixtureSpec> fixtures = inputs.fixtures().stream()
                .map(input -> new WorldRecipe.FixtureSpec(
                        input.id(),
                        input.name(),
                        input.description(),
                        resolveOwner(input.ownerKey(), inputs.plotKeys(), inputs.thingKeys(), "fixtures.ownerKey"),
                        input.visible(),
                        input.cells()
                ))
                .toList();

        List<GameSave.ActorRecipe> actors = inputs.actors().stream()
                .map(input -> new ActorRecipeBuilder()
                        .withId(input.id())
                        .withName(input.name())
                        .withDescription(input.description())
                        .withOwnerId(resolveOwner(input.ownerKey(), inputs.plotKeys(), inputs.thingKeys(), "actors.ownerKey"))
                        .withVisible(input.visible())
                        .withSkills(input.skills())
                        .withEquippedMainHandItemId(resolveOptionalThing(input.equippedMainHandItemKey(), inputs.thingKeys(), "actors.equippedMainHandItemId"))
                        .withEquippedBodyItemId(resolveOptionalThing(input.equippedBodyItemKey(), inputs.thingKeys(), "actors.equippedBodyItemId"))
                        .withCells(input.cells())
                        .build())
                .toList();

        List<GameSave.ItemRecipe> items = inputs.items().stream()
                .map(input -> new ItemRecipeBuilder()
                        .withId(input.id())
                        .withName(input.name())
                        .withDescription(input.description())
                        .withOwnerId(resolveOwner(input.ownerKey(), inputs.plotKeys(), inputs.thingKeys(), "items.ownerKey"))
                        .withVisible(input.visible())
                        .withFixture(input.fixture())
                        .withKeyString(input.keyString())
                        .withFootprint(input.footprintWidth(), input.footprintHeight())
                        .withCapacity(input.capacityWidth(), input.capacityHeight())
                        .withWeaponDamage(input.weaponDamage())
                        .withArmorMitigation(input.armorMitigation())
                        .withCells(input.cells())
                        .build())
                .toList();

        String resolvedPreamble = inputs.preamble().isBlank() ? actorPreamble(startPlotId, actors) : inputs.preamble();
        return new GameSave(inputs.seed(), startPlotId, resolvedPreamble, plots, gates, fixtures, items, actors);
    }

    private static String actorPreamble(UUID startPlotId, List<GameSave.ActorRecipe> actors) {
        if (startPlotId == null) {
            return "";
        }
        return actors.stream()
                .filter(actor -> startPlotId.equals(actor.ownerId()))
                .map(GameSave.ActorRecipe::description)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse("");
    }

    private static UUID resolveOwner(String keyValue, Map<String, UUID> plotKeys, Map<String, UUID> thingKeys, String field) {
        String key = GameSaveYamlValues.key(keyValue, null, null, field);
        UUID found = thingKeys.get(key);
        if (found != null) {
            return found;
        }
        found = plotKeys.get(key);
        if (found != null) {
            return found;
        }
        throw new IllegalArgumentException("Owner not found: " + key);
    }

    private static UUID resolveOptionalThing(String key, Map<String, UUID> thingKeys, String field) {
        if (key == null || key.isBlank()) {
            return null;
        }
        UUID found = thingKeys.get(key);
        if (found != null) {
            return found;
        }
        throw new IllegalArgumentException("Thing not found for " + field + ": " + key);
    }

    private static UUID requirePlot(String key, Map<String, UUID> plotKeys) {
        UUID id = plotKeys.get(key);
        if (id == null) {
            throw new IllegalArgumentException("Plot not found: " + key);
        }
        return id;
    }

    private static Map<String, Coord> deriveCoordinates(List<PlotInput> plots) {
        Map<String, Coord> coords = new HashMap<>();
        List<PlotInput> sorted = plots.stream()
                .sorted(Comparator.comparing(PlotInput::region)
                        .thenComparing(PlotInput::key))
                .toList();
        int index = 0;
        for (PlotInput input : sorted) {
            int x = index % AUTO_GRID_WIDTH;
            int y = index / AUTO_GRID_WIDTH;
            coords.put(input.key(), new Coord(x, y));
            index++;
        }
        return coords;
    }

    private record Coord(int x, int y) {
    }
}

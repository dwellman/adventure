package com.demo.adventure.authoring.save.io;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.bool;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.description;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.key;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.keyStringOrDefault;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.list;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.map;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.optionalDouble;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.optionalInt;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.optionalKey;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.optionalKeyOrNull;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.optionalLong;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.optionalString;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.parseCells;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.parseLong;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.str;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.stringList;
import static com.demo.adventure.authoring.save.io.GameSaveYamlValues.uuid;

final class GameSaveYamlInputCollector {
    private GameSaveYamlInputCollector() {
    }

    @SuppressWarnings("unchecked")
    static GameSaveYamlInputs collect(Map<String, Object> root) {
        long seed = parseLong(root.get("seed"), "seed");
        String preamble = optionalString(root.get("preamble"), "preamble");
        Map<String, UUID> plotKeys = new HashMap<>();
        Map<String, UUID> thingKeys = new HashMap<>();

        List<PlotInput> plotInputs = new ArrayList<>();
        List<GateInput> gateInputs = new ArrayList<>();
        List<FixtureInput> fixtureInputs = new ArrayList<>();
        List<ItemInput> itemInputs = new ArrayList<>();
        List<ActorInput> actorInputs = new ArrayList<>();
        for (Object entry : list(root.get("plots"), "plots")) {
            Map<String, Object> map = map(entry, "plot");
            String plotName = str(map.containsKey("name") ? map.get("name") : map.get("plot"), "plots.name");
            String plotKey = key(map.get("plotKey"), map.get("plot"), map.get("name"), "plots.plotKey");
            UUID plotId = uuid("plot", plotKey);
            if (plotKeys.put(plotKey, plotId) != null) {
                throw new IllegalArgumentException("Duplicate plot key: " + plotKey);
            }
            Integer locationX = optionalInt(map.get("locationX"), "plots.locationX");
            Integer locationY = optionalInt(map.get("locationY"), "plots.locationY");
            if ((locationX == null) != (locationY == null)) {
                throw new IllegalArgumentException("plots.locationX and plots.locationY must both be set or both be omitted for plotKey: " + plotKey);
            }
            plotInputs.add(new PlotInput(
                    plotKey,
                    plotId,
                    plotName,
                    str(map.get("region"), "plots.region"),
                    description(map.get("description"), "plots.description"),
                    locationX,
                    locationY
            ));

            for (Object gateObj : list(map.get("gates"), "plots.gates")) {
                Map<String, Object> gateMap = map(gateObj, "plots.gates.gate");
                Map<String, Object> plotA = gateMap.get("plotA") instanceof Map<?, ?> rawA ? (Map<String, Object>) rawA : null;
                String fromKey = plotA == null ? plotKey : key(plotA.get("fromPlot"), null, null, "plots.gates.plotA.fromPlot");
                String toKey = plotA == null ? key(gateMap.get("toPlot"), gateMap.get("toPlotKey"), null, "plots.gates.toPlot")
                        : key(plotA.get("toPlot"), null, null, "plots.gates.plotA.toPlot");
                gateInputs.add(new GateInput(
                        fromKey,
                        toKey,
                        str(gateMap.get("direction"), "plots.gates.direction"),
                        plotA == null ? bool(gateMap.get("visible")) : bool(plotA.get("visible")),
                        plotA == null ? keyStringOrDefault(gateMap.get("keyString"), "plots.gates.keyString")
                                : keyStringOrDefault(plotA.get("keyString"), "plots.gates.plotA.keyString"),
                        str(gateMap.get("label"), "plots.gates.label"),
                        description(plotA == null ? gateMap.get("description") : plotA.get("description"), "plots.gates.description")
                ));
            }

            for (Object fixtureObj : list(map.get("fixtures"), "plots.fixtures")) {
                Map<String, Object> fixtureMap = map(fixtureObj, "plots.fixtures.fixture");
                String key = key(fixtureMap.get("key"), null, null, "plots.fixtures.key");
                UUID id = uuid("fixture", key);
                if (thingKeys.put(key, id) != null) {
                    throw new IllegalArgumentException("Duplicate fixture key: " + key);
                }
                fixtureInputs.add(new FixtureInput(
                        key,
                        id,
                        str(fixtureMap.get("name"), "plots.fixtures.name"),
                        description(fixtureMap.get("description"), "plots.fixtures.description"),
                        optionalKey(fixtureMap.get("ownerKey"), null, plotKey),
                        bool(fixtureMap.get("visible")),
                        parseCells(fixtureMap.get("cells"), "plots.fixtures.cells")
                ));
            }

            for (Object itemObj : list(map.get("items"), "plots.items")) {
                Map<String, Object> itemMap = map(itemObj, "plots.items.item");
                String key = key(itemMap.get("key"), null, null, "plots.items.key");
                UUID id = uuid("item", key);
                if (thingKeys.put(key, id) != null) {
                    throw new IllegalArgumentException("Duplicate item key: " + key);
                }
                itemInputs.add(new ItemInput(
                        key,
                        id,
                        str(itemMap.get("name"), "plots.items.name"),
                        description(itemMap.get("description"), "plots.items.description"),
                        optionalKey(itemMap.get("ownerKey"), null, plotKey),
                        bool(itemMap.get("visible")),
                        bool(itemMap.get("fixture")),
                        keyStringOrDefault(itemMap.get("keyString"), "plots.items.keyString"),
                        optionalDouble(itemMap.get("footprintWidth"), 0.1),
                        optionalDouble(itemMap.get("footprintHeight"), 0.1),
                        optionalDouble(itemMap.get("capacityWidth"), 0.0),
                        optionalDouble(itemMap.get("capacityHeight"), 0.0),
                        optionalLong(itemMap.get("weaponDamage"), 0L),
                        optionalLong(itemMap.get("armorMitigation"), 0L),
                        parseCells(itemMap.get("cells"), "plots.items.cells")
                ));
            }

            for (Object actorObj : list(map.get("actors"), "plots.actors")) {
                Map<String, Object> actorMap = map(actorObj, "plots.actors.actor");
                String key = key(actorMap.get("key"), null, null, "plots.actors.key");
                UUID id = uuid("actor", key);
                if (thingKeys.put(key, id) != null) {
                    throw new IllegalArgumentException("Duplicate actor key: " + key);
                }
                actorInputs.add(new ActorInput(
                        key,
                        id,
                        str(actorMap.get("name"), "plots.actors.name"),
                        description(actorMap.get("description"), "plots.actors.description"),
                        optionalKey(actorMap.get("ownerKey"), null, plotKey),
                        bool(actorMap.get("visible")),
                        stringList(actorMap.get("skills"), "plots.actors.skills"),
                        optionalKeyOrNull(actorMap.get("equippedMainHandItemId"), "plots.actors.equippedMainHandItemId"),
                        optionalKeyOrNull(actorMap.get("equippedBodyItemId"), "plots.actors.equippedBodyItemId"),
                        parseCells(actorMap.get("cells"), "plots.actors.cells")
                ));
            }
        }

        String startPlotKey = key(root.get("startPlotKey"), root.get("startPlot"), null, "startPlotKey");

        return new GameSaveYamlInputs(
                seed,
                preamble,
                startPlotKey,
                plotKeys,
                thingKeys,
                plotInputs,
                gateInputs,
                fixtureInputs,
                itemInputs,
                actorInputs
        );
    }
}

package com.demo.adventure.authoring.save.io;

import com.demo.adventure.engine.mechanics.cells.CellSpec;

import java.util.List;
import java.util.Map;
import java.util.UUID;

record GameSaveYamlInputs(
        long seed,
        String preamble,
        String startPlotKey,
        Map<String, UUID> plotKeys,
        Map<String, UUID> thingKeys,
        List<PlotInput> plots,
        List<GateInput> gates,
        List<FixtureInput> fixtures,
        List<ItemInput> items,
        List<ActorInput> actors
) {
}

record PlotInput(
        String key,
        UUID id,
        String name,
        String region,
        String description,
        Integer locationX,
        Integer locationY
) {
}

record GateInput(
        String fromKey,
        String toKey,
        String direction,
        boolean visible,
        String keyString,
        String label,
        String description
) {
}

record FixtureInput(
        String key,
        UUID id,
        String name,
        String description,
        String ownerKey,
        boolean visible,
        Map<String, CellSpec> cells
) {
}

record ItemInput(
        String key,
        UUID id,
        String name,
        String description,
        String ownerKey,
        boolean visible,
        boolean fixture,
        String keyString,
        double footprintWidth,
        double footprintHeight,
        double capacityWidth,
        double capacityHeight,
        long weaponDamage,
        long armorMitigation,
        Map<String, CellSpec> cells
) {
}

record ActorInput(
        String key,
        UUID id,
        String name,
        String description,
        String ownerKey,
        boolean visible,
        List<String> skills,
        String equippedMainHandItemKey,
        String equippedBodyItemKey,
        Map<String, CellSpec> cells
) {
}

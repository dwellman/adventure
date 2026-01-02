package com.demo.adventure.authoring.save.io;

import com.demo.adventure.authoring.gardener.GardenResult;
import com.demo.adventure.domain.save.GameSave;

import java.util.LinkedHashMap;
import java.util.Map;

final class GameSaveYamlDocumentBuilder {
    private GameSaveYamlDocumentBuilder() {
    }

    static Map<String, Object> toDocument(GameSave save) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("seed", save.seed());
        root.put("startPlot", GameSaveYamlWriterSupport.resolveName(save.startPlotId(), save));
        root.put("startPlotKey", GameSaveYamlWriterSupport.resolveKey(save.startPlotId(), save));
        root.put("preamble", GameSaveYamlWriterSupport.preamble(save));
        root.put("plots", GameSaveYamlPlotBuilder.structuredPlots(save));
        return root;
    }

    static Map<String, Object> toDocument(GardenResult result) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("seed", result.seed());
        root.put("startPlot", GameSaveYamlWriterSupport.resolveName(result.startPlotId(), result.registry()));
        root.put("startPlotKey", GameSaveYamlWriterSupport.resolveKey(result.startPlotId(), result.registry()));
        root.put("preamble", GameSaveYamlWriterSupport.preamble(result));
        root.put("plots", GameSaveYamlPlotBuilder.structuredPlots(result));
        return root;
    }
}

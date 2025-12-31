package com.demo.adventure.authoring.gardener.ai;

import com.demo.adventure.ai.client.AiChatClient;
import com.demo.adventure.ai.client.AiChatMessage;
import com.demo.adventure.ai.client.AiChatRequest;
import com.demo.adventure.ai.client.AiChatResponse;
import com.demo.adventure.ai.client.OpenAiChatClient;
import com.demo.adventure.ai.runtime.AiPromptPrinter;
import com.demo.adventure.authoring.gardener.FixtureDescriptionExpander;
import com.demo.adventure.authoring.gardener.GardenerDescriptionPatch;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.UUID;

/**
 * Description expander that calls OpenAI's chat API directly for plots, fixtures, and items.
 */
public final class OpenAiHttpFixtureDescriptionExpander implements FixtureDescriptionExpander {
    private static final String MODEL = "gpt-4o-mini";
    private static final Duration TIMEOUT = Duration.ofSeconds(Long.getLong("gardener.ai.timeout.seconds", 120L));
    private static final String SYSTEM_PROMPT = loadPrompt();
    private static final AiChatClient CHAT_CLIENT = new OpenAiChatClient();
    private final String voice;
    private final String overview;

    public OpenAiHttpFixtureDescriptionExpander() {
        this("", "");
    }

    public OpenAiHttpFixtureDescriptionExpander(String voice, String overview) {
        this.voice = voice == null ? "" : voice;
        this.overview = overview == null ? "" : overview;
    }

    @Override
    public List<GardenerDescriptionPatch> expand(KernelRegistry registry) {
        String apiKey = resolveApiKey();
        if (apiKey == null) {
            System.out.println("[Gardener] OpenAI key not found; skipping AI expansion.");
            return List.of();
        }
        Map<UUID, Plot> plots = plotsById(registry);
        if ((plots == null || plots.isEmpty()) && (registry == null || registry.getEverything().isEmpty())) {
            return List.of();
        }

        List<TargetInput> allTargets = collectTargets(registry, plots);
        List<GardenerDescriptionPatch> patches = new ArrayList<>();
        patches.addAll(processTargets(allTargets, t -> "PLOT".equals(t.type()), registry, apiKey));
        patches.addAll(processTargets(allTargets, t -> "GATE".equals(t.type()), registry, apiKey));
        patches.addAll(processTargets(allTargets, t -> !"PLOT".equals(t.type()) && !"GATE".equals(t.type()), registry, apiKey));
        return patches;
    }

    private static String callOpenAi(String apiKey, String systemPrompt, String userPrompt) throws Exception {
        AiPromptPrinter.printChatPrompt("gardener", systemPrompt, userPrompt, false);
        AiChatRequest request = AiChatRequest.builder()
                .model(MODEL)
                .messages(List.of(
                        AiChatMessage.system(systemPrompt),
                        AiChatMessage.user(userPrompt)
                ))
                .temperature(0.7)
                .responseFormat(AiChatRequest.ResponseFormat.jsonObject())
                .timeout(TIMEOUT)
                .build();
        AiChatResponse response = CHAT_CLIENT.chat(apiKey, request);
        return response == null ? null : response.content();
    }

    @SuppressWarnings("unchecked")
    private List<GardenerDescriptionPatch> applyResponseForTarget(TargetInput target, String response, KernelRegistry registry) {
        List<GardenerDescriptionPatch> patches = new ArrayList<>();
        if (response == null || response.isBlank() || target == null) {
            return patches;
        }
        String content = parseContent(response);
        if (content == null || content.isBlank()) {
            return patches;
        }
        Object llmParsed = new Yaml().load(content);
        List<?> llmList = llmParsed instanceof List<?> l ? l : null;
        if (llmList == null && llmParsed instanceof Map<?, ?> map && map.values().stream().allMatch(List.class::isInstance)) {
            llmList = map.values().stream().findFirst().map(List.class::cast).orElse(null);
        }
        if (llmList == null && llmParsed instanceof Map<?, ?> singleMap && singleMap.containsKey("thingId") && singleMap.containsKey("description")) {
            llmList = List.of(singleMap);
        }
        if (llmList == null) {
            return patches;
        }
        for (Object entry : llmList) {
            if (!(entry instanceof Map<?, ?> raw)) {
                continue;
            }
            Map<String, Object> map = (Map<String, Object>) raw;
            Object idVal = map.get("thingId");
            Object descVal = map.get("description");
            if (idVal == null || descVal == null) {
                continue;
            }
            UUID id;
            try {
                id = UUID.fromString(idVal.toString());
            } catch (Exception ex) {
                continue;
            }
            if (!Objects.equals(id, target.id())) {
                continue;
            }
            Thing thing = registry.get(id);
            if (thing == null) {
                continue;
            }
            String updated = Objects.toString(descVal, "").trim();
            if (updated.isBlank()) {
                continue;
            }
            if (thing instanceof Gate gate) {
                String original = gate.getDescriptionFrom(target.fromPlotId());
                String updatedForGate = updated;
                String destination = nullToEmpty(target.toPlotName());
                if (!destination.isEmpty() && !updated.toLowerCase().contains("to:")) {
                    updatedForGate = updated + " to: " + destination;
                }
                updated = updatedForGate;
                if (!original.equals(updated)) {
                    List<?> history = gate.getDescriptionHistoryFrom(target.fromPlotId());
                    int historySize = history == null ? 0 : history.size();
                    if (historySize == 0) {
                        gate.recordDescriptionFrom(target.fromPlotId(), original, 0);
                    }
                    int nextClock = historySize == 0 ? 1 : historySize + 1;
                    gate.recordDescriptionFrom(target.fromPlotId(), updated, nextClock);
                    patches.add(new GardenerDescriptionPatch(id, original, updated, "ai-gardener"));
                }
            } else {
                String original = thing.getDescription();
                if (!original.equals(updated)) {
                    int historySize = thing.getDescriptionHistory().size();
                    if (historySize == 0) {
                        thing.recordDescription(original, 0);
                    }
                    int nextClock = historySize == 0 ? 1 : historySize + 1;
                    thing.recordDescription(updated, nextClock);
                    patches.add(new GardenerDescriptionPatch(id, original, updated, "ai-gardener"));
                }
            }
        }
        return patches;
    }

    private static String parseContent(Object contentObj) {
        if (contentObj == null) {
            return null;
        }
        String content = contentObj.toString().trim();
        if (content.startsWith("```")) {
            int first = content.indexOf('\n');
            int last = content.lastIndexOf("```");
            if (first >= 0 && last > first) {
                content = content.substring(first + 1, last).trim();
            }
        }
        return content;
    }

    private static List<TargetInput> collectTargets(KernelRegistry registry, Map<UUID, Plot> plots) {
        List<TargetInput> targets = new ArrayList<>();
        if (registry == null) {
            return targets;
        }
        for (Thing thing : registry.getEverything().values()) {
            if (thing instanceof Plot plot) {
                List<String> history = new ArrayList<>();
                plot.getDescriptionHistory().forEach(h -> history.add(h.text()));
                if (history.isEmpty()) {
                    history.add(plot.getDescription());
                }
                targets.add(new TargetInput(plot.getId(), plot.getId(), plot.getLabel(), "PLOT", plot.getLabel(), plot.getDescription(), "", "", "", history, contextSummaries(registry, plots, plot.getId(), plot.getId())));
            } else if (thing instanceof Gate gate) {
                UUID plotA = gate.getPlotAId();
                UUID plotB = gate.getPlotBId();
                Plot fromA = plots.get(plotA);
                Plot fromB = plots.get(plotB);
                // From plot A toward plot B
                if (plotA != null && fromA != null) {
                    List<String> historyA = new ArrayList<>();
                    gate.getDescriptionHistoryFrom(plotA).forEach(h -> historyA.add(h.text()));
                    if (historyA.isEmpty()) {
                        historyA.add(gate.getDescriptionFrom(plotA));
                    }
                    targets.add(new TargetInput(
                            gate.getId(),
                            plotA,
                            gate.getLabel(),
                            "GATE",
                            nullToEmpty(fromA.getLabel()),
                            nullToEmpty(fromA.getDescription()),
                            plotB == null ? "" : nullToEmpty(fromB == null ? "" : fromB.getLabel()),
                            plotB == null ? "" : nullToEmpty(fromB == null ? "" : fromB.getDescription()),
                            gate.getDirection() == null ? "" : gate.getDirection().toLongName(),
                            historyA,
                            contextSummaries(registry, plots, plotA, gate.getId())
                    ));
                }
                // From plot B toward plot A
                if (plotB != null && fromB != null) {
                    List<String> historyB = new ArrayList<>();
                    gate.getDescriptionHistoryFrom(plotB).forEach(h -> historyB.add(h.text()));
                    if (historyB.isEmpty()) {
                        historyB.add(gate.getDescriptionFrom(plotB));
                    }
                    targets.add(new TargetInput(
                            gate.getId(),
                            plotB,
                            gate.getLabel(),
                            "GATE",
                            nullToEmpty(fromB.getLabel()),
                            nullToEmpty(fromB.getDescription()),
                            plotA == null ? "" : nullToEmpty(fromA == null ? "" : fromA.getLabel()),
                            plotA == null ? "" : nullToEmpty(fromA == null ? "" : fromA.getDescription()),
                            gate.getDirection() == null ? "" : Direction.oppositeOf(gate.getDirection()).toLongName(),
                            historyB,
                            contextSummaries(registry, plots, plotB, gate.getId())
                    ));
                }
            } else {
                String type = resolveType(thing);
                if (type == null) {
                    continue;
                }
                UUID plotId = owningPlotId(thing, registry, plots);
                String plotName = plotId == null ? "" : nullToEmpty(plots.get(plotId) == null ? "" : plots.get(plotId).getLabel());
                String plotDescription = plotId == null ? "" : nullToEmpty(plots.get(plotId) == null ? "" : plots.get(plotId).getDescription());
                List<String> context = contextSummaries(registry, plots, plotId, thing.getId());
                List<String> history = new ArrayList<>();
                thing.getDescriptionHistory().forEach(h -> history.add(h.text()));
                if (history.isEmpty()) {
                    history.add(thing.getDescription());
                }
                targets.add(new TargetInput(thing.getId(), plotId, thing.getLabel(), type, plotName, plotDescription, "", "", "", history, context));
            }
        }
        return targets;
    }

    private static String resolveType(Thing thing) {
        if (thing instanceof Plot) {
            return "PLOT";
        }
        if (thing instanceof Gate) {
            return "GATE";
        }
        if (thing instanceof Item item && item.isFixture()) {
            return "FIXTURE";
        }
        if (thing instanceof Item) {
            return "ITEM";
        }
        return null;
    }

    private static UUID owningPlotId(Thing thing, KernelRegistry registry, Map<UUID, Plot> plots) {
        if (thing == null || registry == null) {
            return null;
        }
        UUID current = thing.getOwnerId();
        java.util.Set<UUID> visited = new java.util.HashSet<>();
        while (current != null && visited.add(current)) {
            if (plots.containsKey(current)) {
                return current;
            }
            Thing owner = registry.get(current);
            current = owner == null ? null : owner.getOwnerId();
        }
        return null;
    }

    private static List<String> contextSummaries(
            KernelRegistry registry,
            Map<UUID, Plot> plots,
            UUID plotId,
            UUID targetId
    ) {
        if (registry == null || plotId == null) {
            return List.of();
        }
        List<String> summaries = new ArrayList<>();
        for (Thing t : registry.getEverything().values()) {
            if (t == null || Objects.equals(t.getId(), targetId)) {
                continue;
            }
            UUID ownerPlot = owningPlotId(t, registry, plots);
            if (!Objects.equals(ownerPlot, plotId)) {
                continue;
            }
            String type = resolveType(t);
            String label = nullToEmpty(t.getLabel());
            String desc = nullToEmpty(t.getDescription());
            summaries.add(type + ": " + label + " — " + desc);
        }
        return summaries;
    }

    private String buildUserPrompt(TargetInput target) {
        StringBuilder sb = new StringBuilder();
        sb.append("NarrativeVoice: ").append(nullToEmpty(System.getProperty("gardener.voice", voice))).append('\n');
        sb.append("GameOverview: ").append(nullToEmpty(System.getProperty("gardener.overview", overview))).append('\n');
        sb.append("Instruction: You are given exactly one thing. Return a JSON array with one entry containing thingId and description. If the thing is a GATE, write a short directional travel description that ties plotName to toPlotName; weave the direction into the movement and include a trailing token \" to: ").append(nullToEmpty(target.toPlotName())).append("\" to make the destination explicit. Do not change mechanics. Do not add extra fields.\n");
        sb.append("StoryContext:\n");
        sb.append("- backstoryId: unknown\n");
        sb.append("- theme: exploration\n");
        sb.append("- greenMotif: false\n");
        sb.append("Target:\n");
        sb.append("  thingId: ").append(target.id()).append('\n');
        sb.append("  thingName: ").append(nullToEmpty(target.label())).append('\n');
        sb.append("  type: ").append(target.type()).append('\n');
        sb.append("  plotName: ").append(nullToEmpty(target.plotName())).append('\n');
        sb.append("  plotDescription: ").append(nullToEmpty(target.plotDescription())).append('\n');
        if (target.toPlotName() != null && !target.toPlotName().isEmpty()) {
            sb.append("  toPlotName: ").append(nullToEmpty(target.toPlotName())).append('\n');
        }
        if (target.toPlotDescription() != null && !target.toPlotDescription().isEmpty()) {
            sb.append("  toPlotDescription: ").append(nullToEmpty(target.toPlotDescription())).append('\n');
        }
        if (target.direction() != null && !target.direction().isEmpty()) {
            sb.append("  direction: ").append(nullToEmpty(target.direction())).append('\n');
        }
        sb.append("  description: ").append(target.history().isEmpty() ? "" : nullToEmpty(target.history().get(target.history().size() - 1))).append('\n');
        sb.append("  history:\n");
        for (String h : target.history()) {
            sb.append("    - ").append(nullToEmpty(h)).append('\n');
        }
        sb.append("  context:\n");
        for (String c : target.context()) {
            sb.append("    - ").append(nullToEmpty(c)).append('\n');
        }
        return sb.toString();
    }

    private static String resolveApiKey() {
        String overridePath = System.getProperty("openai.key.path");
        if (overridePath != null) {
            String fromOverride = readKeyFile(overridePath);
            if (fromOverride != null) {
                return fromOverride;
            }
            return null; // explicit override points to missing/empty file; treat as absent
        }

        String fromEnv = System.getenv("OPENAI_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        String path = System.getProperty("openai.key.path", "../.secret");
        String fromDefault = readKeyFile(path);
        if (fromDefault != null) {
            return fromDefault;
        }
        return null;
    }

    private static String readKeyFile(String path) {
        try {
            Path p = Path.of(path);
            if (Files.exists(p)) {
                List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                        continue;
                    }
                    if (trimmed.startsWith("OPENAI_API_KEY=")) {
                        return trimmed.substring("OPENAI_API_KEY=".length()).trim();
                    }
                    return trimmed;
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
        return null;
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private static String loadPrompt() {
        String resourcePath = "agents/gardener-system.md";
        try (InputStream in = OpenAiHttpFixtureDescriptionExpander.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
            // fall back to embedded
        }
        return """
You are the Gardener for a turn-based adventure game.
Keep mechanics unchanged; rewrite every provided thing’s description in 1–2 sentences, hopeful and concrete. Provide a JSON array entry for every thingId you are given.
""";
    }

    private List<GardenerDescriptionPatch> processTargets(
            List<TargetInput> candidates,
            Predicate<TargetInput> filter,
            KernelRegistry registry,
            String apiKey
    ) {
        List<TargetInput> filtered = new ArrayList<>();
        if (candidates != null) {
            candidates.stream().filter(filter).forEach(filtered::add);
        }
        filtered.sort(java.util.Comparator.comparing(TargetInput::label, java.util.Comparator.nullsLast(String::compareTo)));

        List<GardenerDescriptionPatch> patches = new ArrayList<>();
        for (TargetInput target : filtered) {
            try {
                String user = buildUserPrompt(target);
                String response = callOpenAi(apiKey, SYSTEM_PROMPT, user);
                patches.addAll(applyResponseForTarget(target, response, registry));
            } catch (Exception ex) {
                System.out.println("[Gardener] OpenAI call failed for " + target.label() + "; skipping. " + ex.getMessage());
            }
        }
        return patches;
    }

    private static Map<UUID, Plot> plotsById(KernelRegistry registry) {
        Map<UUID, Plot> plots = new java.util.LinkedHashMap<>();
        if (registry == null) {
            return plots;
        }
        registry.getEverything().values().forEach(t -> {
            if (t instanceof Plot p) {
                plots.put(p.getId(), p);
            }
        });
        return plots;
    }

    private record TargetInput(UUID id, UUID fromPlotId, String label, String type, String plotName, String plotDescription, String toPlotName, String toPlotDescription, String direction, List<String> history, List<String> context) {
    }
}

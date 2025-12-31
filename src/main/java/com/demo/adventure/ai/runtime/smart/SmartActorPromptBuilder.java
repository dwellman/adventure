package com.demo.adventure.ai.runtime.smart;

import com.demo.adventure.ai.runtime.PromptTemplates;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SmartActorPromptBuilder {
    private SmartActorPromptBuilder() {
    }

    public static SmartActorPrompt build(SmartActorSpec spec,
                                         SmartActorContext context,
                                         SmartActorWorldSnapshot snapshot) {
        if (spec == null || context == null || snapshot == null) {
            throw new IllegalArgumentException("spec, context, and snapshot are required");
        }
        String systemPrompt = loadSystemPrompt(spec.promptId());
        String userPrompt = buildUserPrompt(spec, context, snapshot);
        return new SmartActorPrompt(systemPrompt, userPrompt);
    }

    private static String loadSystemPrompt(String promptId) {
        if (promptId == null || promptId.isBlank()) {
            throw new IllegalArgumentException("promptId is required");
        }
        String normalized = promptId.trim();
        String path = normalized.endsWith(".md")
                ? "agents/" + normalized
                : "agents/" + normalized + ".md";
        String prompt = PromptTemplates.load(path);
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalStateException("Missing smart actor prompt: " + path);
        }
        return prompt.trim();
    }

    private static String buildUserPrompt(SmartActorSpec spec,
                                          SmartActorContext context,
                                          SmartActorWorldSnapshot snapshot) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("actor", actorPayload(spec, context, snapshot));
        payload.put("context", contextPayload(context, snapshot));
        payload.put("ALLOWED_VERBS", spec.policy().allowedVerbs());
        return yaml().dump(payload).trim();
    }

    private static Map<String, Object> actorPayload(SmartActorSpec spec,
                                                    SmartActorContext context,
                                                    SmartActorWorldSnapshot snapshot) {
        Map<String, Object> actor = new LinkedHashMap<>();
        actor.put("actorKey", spec.actorKey());
        actor.put("actorId", context.actorId().toString());
        actor.put("label", snapshot.actorLabel());
        actor.put("description", snapshot.actorDescription());
        actor.put("backstory", spec.backstory());
        actor.put("persona", spec.persona());
        actor.put("properties", spec.properties());
        actor.put("memorySeeds", memorySeeds(spec.memorySeeds()));
        actor.put("historySnippets", historySnippets(context.historySnippets()));
        actor.put("policy", policyPayload(spec.policy()));
        return actor;
    }

    private static Map<String, Object> contextPayload(SmartActorContext context,
                                                      SmartActorWorldSnapshot snapshot) {
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("plotId", context.plotId().toString());
        ctx.put("plotLabel", snapshot.plotLabel());
        ctx.put("plotDescription", snapshot.plotDescription());
        ctx.put("contextTags", context.contextTags());
        ctx.put("visibleFixtures", snapshot.visibleFixtures());
        ctx.put("visibleItems", snapshot.visibleItems());
        ctx.put("visibleActors", snapshot.visibleActors());
        ctx.put("inventory", snapshot.inventory());
        ctx.put("exits", snapshot.exits());
        ctx.put("lastScene", snapshot.lastScene());
        ctx.put("playerUtterance", snapshot.playerUtterance());
        ctx.put("receipts", snapshot.receipts());
        return ctx;
    }

    private static List<Map<String, Object>> memorySeeds(List<SmartActorMemorySeed> seeds) {
        if (seeds == null || seeds.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (SmartActorMemorySeed seed : seeds) {
            if (seed == null) {
                continue;
            }
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", seed.id());
            entry.put("text", seed.text());
            entry.put("scope", seed.scope().name().toLowerCase(Locale.ROOT));
            entry.put("tags", seed.tags());
            out.add(entry);
        }
        return List.copyOf(out);
    }

    private static List<Map<String, Object>> historySnippets(List<SmartActorHistoryEntry> history) {
        if (history == null || history.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (SmartActorHistoryEntry entry : history) {
            if (entry == null) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", entry.id());
            row.put("text", entry.text());
            row.put("scope", entry.scope().name().toLowerCase(Locale.ROOT));
            row.put("tags", entry.tags());
            row.put("source", entry.source());
            row.put("pinned", entry.pinned());
            out.add(row);
        }
        return List.copyOf(out);
    }

    private static Map<String, Object> policyPayload(SmartActorPolicy policy) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("allowedVerbs", policy.allowedVerbs());
        payload.put("maxUtteranceLength", policy.maxUtteranceLength());
        payload.put("cooldownTurns", policy.cooldownTurns());
        payload.put("maxColorLines", policy.maxColorLines());
        return payload;
    }

    private static Yaml yaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new Yaml(options);
    }
}

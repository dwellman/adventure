package com.demo.adventure.authoring.gardener.ai;

import com.demo.adventure.ai.client.AiChatClient;
import com.demo.adventure.ai.client.AiChatMessage;
import com.demo.adventure.ai.client.AiChatRequest;
import com.demo.adventure.ai.client.AiChatResponse;
import com.demo.adventure.ai.client.OpenAiChatClient;
import com.demo.adventure.ai.runtime.AiConfig;
import com.demo.adventure.ai.runtime.AiPromptPrinter;
import com.demo.adventure.authoring.gardener.FixtureDescriptionExpander;
import com.demo.adventure.authoring.gardener.GardenerDescriptionPatch;
import com.demo.adventure.authoring.gardener.NoopFixtureDescriptionExpander;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Thing;
import org.yaml.snakeyaml.Yaml;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Fixture description expander backed by an LLM. Falls back to a deterministic expander on error.
 */
public final class AiFixtureDescriptionExpander implements FixtureDescriptionExpander {
    private static final String SYSTEM_PROMPT = """
You are the Gardener for a turn-based adventure game.

The game already has a complete mechanical world: locations (plots), items (things), exits,
and rules. Your job is NOT to change rules. Your job is to decorate existing THINGS with short,
safe, story-aware descriptions.

You will be given a small, bounded view of the world:

- A list of candidate THINGS you are allowed to decorate (scenery and background objects).
- For each THING:
  - A stable id (thingId)
  - A name (thingName)
  - A type or role (e.g., SCENERY, ITEM, MECHANIC)
  - Tags that describe materials or flavor (STONE, WOOD, PLANT, METAL, BONES, etc.)
  - The name of the plot (location) where it is found
  - Its current description (if any)
  - A short history of previous descriptions, from oldest to newest
- A StoryContext describing the tone and motif:
  - backstoryId (which story arc this island follows)
  - theme (for example: arrival & discovery, preparing to escape, near-escape)
  - greenMotif (true/false) indicating whether gentle green imagery is part of the world’s style

Your responsibilities:

1. You may ONLY propose new descriptions for the listed THINGS.
   - You must NOT invent new items, exits, characters, or rules.
   - You must NOT suggest interactions or commands (no “you should take this” or “type X”).
   - You must NOT contradict mechanics or tags: if a thing is tagged STONE, do not describe it
     as wood; if it is an ITEM, do not describe it as impossible to pick up.

2. You must respect history and keep the world consistent.
   - Read the previous descriptions; do not contradict them without a clear evolution.
   - If past descriptions mention green moss, you may keep or extend that motif, but you must not
     suddenly describe the same object as clean and polished unless the tags or context imply change.
   - Do not repeat exact previous text; new descriptions should add color while staying consistent.

3. Style and length:
   - 1–2 sentences per THING.
   - All-ages, hopeful and curious tone, similar to a family adventure story.
   - Gentle use of green imagery when greenMotif is true (moss, leaves, soft light), not overdone.
   - Use simple, concrete language; avoid heavy or grim wording.
   - Stay in third person for neutral object descriptions (this is the base description, not the
     narrated second-person voice).

4. No meta or engine talk:
   - Do not mention turns, clocks, mechanics, tests, or code.
   - Do not talk about “the player,” “the engine,” or “the AI.”
   - You are describing the world itself, not how it is implemented.

Your output:

- You must return a JSON array of objects.
- Each object must have exactly:
  - "thingId": the id you were given for the THING
  - "description": the new description text for that THING

If you do not want to change any descriptions, return an empty JSON array: [].
""";
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String MODEL = CONFIG.getString("ai.authoring.model", "gpt-4o-mini");
    private static final double TEMPERATURE = CONFIG.getDouble("ai.authoring.temperature", 0.2);
    private static final double TOP_P = CONFIG.getDouble("ai.authoring.top_p", 1.0);
    private static final boolean ENABLE_LOGPROBS = CONFIG.getBoolean("ai.authoring.logprobs", false);
    private static final int TOP_LOGPROBS = CONFIG.getInt("ai.authoring.top_logprobs", 3);
    private static final Duration TIMEOUT = Duration.ofSeconds(40);
    private static final AiChatClient DEFAULT_CLIENT = new OpenAiChatClient();

    private final AiChatClient chatClient;
    private final String apiKey;
    private final FixtureDescriptionExpander fallback;

    public AiFixtureDescriptionExpander(AiChatClient chatClient) {
        this(chatClient, null, new NoopFixtureDescriptionExpander());
    }

    public AiFixtureDescriptionExpander(AiChatClient chatClient, String apiKey) {
        this(chatClient, apiKey, new NoopFixtureDescriptionExpander());
    }

    public AiFixtureDescriptionExpander(AiChatClient chatClient, String apiKey, FixtureDescriptionExpander fallback) {
        this.chatClient = chatClient == null ? DEFAULT_CLIENT : chatClient;
        this.apiKey = apiKey;
        this.fallback = fallback == null ? new NoopFixtureDescriptionExpander() : fallback;
    }

    @Override
    // Pattern: Trust UX
    // - Falls back to deterministic expansion whenever the AI is unavailable or returns nothing.
    public List<GardenerDescriptionPatch> expand(KernelRegistry registry) {
        if (chatClient == null || apiKey == null || apiKey.isBlank()) {
            return fallback.expand(registry);
        }
        List<FixtureInput> fixtures = collectFixtures(registry);
        if (fixtures.isEmpty()) {
            return List.of();
        }

        try {
            String user = buildUserPrompt(fixtures);
            AiPromptPrinter.printChatPrompt("gardener", SYSTEM_PROMPT, user, false);
            AiChatRequest request = AiChatRequest.builder()
                    .model(MODEL)
                    .messages(List.of(
                            AiChatMessage.system(SYSTEM_PROMPT),
                            AiChatMessage.user(user)
                    ))
                    .temperature(TEMPERATURE)
                    .topP(TOP_P)
                    .logprobs(ENABLE_LOGPROBS)
                    .topLogprobs(ENABLE_LOGPROBS ? TOP_LOGPROBS : null)
                    .timeout(TIMEOUT)
                    .build();
            AiChatResponse response = chatClient.chat(apiKey, request);
            String responseText = response == null ? null : response.content();
            List<GardenerDescriptionPatch> patches = applyResponse(responseText, registry);
            if (patches.isEmpty()) {
                return fallback.expand(registry);
            }
            return patches;
        } catch (Exception ex) {
            return fallback.expand(registry);
        }
    }

    private static List<FixtureInput> collectFixtures(KernelRegistry registry) {
        List<FixtureInput> fixtures = new ArrayList<>();
        if (registry == null) {
            return fixtures;
        }
        for (Thing thing : registry.getEverything().values()) {
            if (thing instanceof Item item && item.isFixture()) {
                String ownerLabel = resolveOwnerLabel(item, registry);
                List<String> history = new ArrayList<>();
                item.getDescriptionHistory().forEach(h -> history.add(h.text()));
                if (history.isEmpty()) {
                    history.add(item.getDescription());
                }
                fixtures.add(new FixtureInput(item.getId(), item.getLabel(), item.getDescription(), ownerLabel, history));
            }
        }
        return fixtures;
    }

    private static String resolveOwnerLabel(Item item, KernelRegistry registry) {
        if (item == null || registry == null) {
            return "";
        }
        Thing owner = registry.get(item.getOwnerId());
        String label = owner == null ? "" : owner.getLabel();
        return label == null ? "" : label;
    }

    private static String buildUserPrompt(List<FixtureInput> fixtures) {
        StringBuilder sb = new StringBuilder();
        sb.append("StoryContext:\n");
        sb.append("- backstoryId: ").append("unknown").append('\n');
        sb.append("- theme: ").append("exploration").append('\n');
        sb.append("- greenMotif: ").append("false").append('\n');
        sb.append("Fixtures:\n");
        for (FixtureInput f : fixtures) {
            sb.append("- thingId: ").append(f.id()).append('\n');
            sb.append("  thingName: ").append(nullToEmpty(f.label())).append('\n');
            sb.append("  type: FIXTURE\n");
            sb.append("  tags: []\n");
            sb.append("  plotName: ").append(nullToEmpty(f.ownerLabel())).append('\n');
            sb.append("  description: ").append(nullToEmpty(f.description())).append('\n');
            sb.append("  history:\n");
            for (String h : f.history()) {
                sb.append("    - ").append(nullToEmpty(h)).append('\n');
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    // Pattern: Learning
    // - Applies accepted description changes and records history for future prompt context.
    private List<GardenerDescriptionPatch> applyResponse(String response, KernelRegistry registry) {
        List<GardenerDescriptionPatch> patches = new ArrayList<>();
        if (response == null || response.isBlank()) {
            return patches;
        }
        Object parsed = new Yaml().load(response);
        if (!(parsed instanceof List<?> list)) {
            return patches;
        }
        for (Object entry : list) {
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
            Thing thing = registry.get(id);
            if (!(thing instanceof Item item) || !item.isFixture()) {
                continue;
            }
            String updated = Objects.toString(descVal, "").trim();
            if (updated.isBlank()) {
                continue;
            }
            String original = item.getDescription();
            if (!original.equals(updated)) {
                int historySize = item.getDescriptionHistory().size();
                if (historySize == 0) {
                    item.recordDescription(original, 0);
                }
                item.recordDescription(updated, historySize == 0 ? 1 : historySize + 1);
                patches.add(new GardenerDescriptionPatch(id, original, updated, "ai-gardener"));
            }
        }
        return patches;
    }

    private static String nullToEmpty(String text) {
        return text == null ? "" : text;
    }

    private record FixtureInput(UUID id, String label, String description, String ownerLabel, List<String> history) {
    }
}

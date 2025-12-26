package com.demo.adventure.authoring.gardener.ai;

import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.authoring.gardener.FixtureDescriptionExpander;
import com.demo.adventure.authoring.gardener.GardenerDescriptionPatch;
import com.demo.adventure.authoring.gardener.NoopFixtureDescriptionExpander;
import org.springframework.ai.chat.client.ChatClient;
import org.yaml.snakeyaml.Yaml;

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

    private final ChatClient chatClient;
    private final FixtureDescriptionExpander fallback;

    public AiFixtureDescriptionExpander(ChatClient chatClient) {
        this(chatClient, new NoopFixtureDescriptionExpander());
    }

    public AiFixtureDescriptionExpander(ChatClient chatClient, FixtureDescriptionExpander fallback) {
        this.chatClient = chatClient;
        this.fallback = fallback == null ? new NoopFixtureDescriptionExpander() : fallback;
    }

    @Override
    // Pattern: Trust UX
    // - Falls back to deterministic expansion whenever the AI is unavailable or returns nothing.
    public List<GardenerDescriptionPatch> expand(KernelRegistry registry) {
        if (chatClient == null) {
            return fallback.expand(registry);
        }
        List<FixtureInput> fixtures = collectFixtures(registry);
        if (fixtures.isEmpty()) {
            return List.of();
        }

        try {
            String user = buildUserPrompt(fixtures);
            String response = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user)
                    .call()
                    .content();
            List<GardenerDescriptionPatch> patches = applyResponse(response, registry);
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

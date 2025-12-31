package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.mechanics.cells.Cell;
import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.engine.mechanics.cells.CellTransferResult;
import com.demo.adventure.engine.mechanics.combat.CombatEncounter;
import com.demo.adventure.engine.mechanics.combat.CombatEngine;
import com.demo.adventure.engine.mechanics.combat.CombatState;
import com.demo.adventure.engine.mechanics.combat.UnknownReferenceReceipt;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.ai.runtime.smart.SmartActorDecision;
import com.demo.adventure.domain.kernel.ContainerPacker;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.mechanics.crafting.CraftingTable;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SkillResolver;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.model.ThingKind;
import com.demo.adventure.engine.mechanics.ops.Open;
import com.demo.adventure.engine.flow.loop.LoopResetReason;
import com.demo.adventure.engine.flow.loop.LoopResetResult;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerContext;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;
import com.demo.adventure.engine.flow.trigger.TriggerEvent;
import com.demo.adventure.engine.flow.trigger.TriggerOutcome;
import com.demo.adventure.engine.flow.trigger.TriggerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GameRuntime {

    private static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-00000000feed");
    private static final String EMOTE_PREFIX = "EMOTE:";
    private static final String CHECK_REQUEST_PREFIX = "CHECK_REQUEST:";
    private static final String CHECK_RESULT_PREFIX = "CHECK_RESULT:";
    private static final int EMOTE_CHECK_SIDES = 20;
    private static final int EMOTE_CHECK_TARGET = 15;
    private static final Set<String> EMOTE_CHECK_KEYWORDS = Set.of(
            "distract",
            "convince",
            "persuade",
            "intimidate",
            "deceive",
            "lie",
            "sneak",
            "steal",
            "pick",
            "unlock",
            "hide",
            "escape",
            "bluff",
            "charm"
    );

    private final SceneNarrator narrator;
    private final Consumer<String> emitter;
    private final boolean aiEnabled;
    private boolean outputSuppressed;

    private KernelRegistry registry;
    private UUID currentPlot;
    private UUID playerId;
    private List<Item> inventory = new ArrayList<>();
    private Map<UUID, Map<UUID, Rectangle2D>> inventoryPlacements = new HashMap<>();
    private LoopRuntime loopRuntime;
    private TriggerEngine triggerEngine;
    private CombatEncounter encounter;
    private Map<String, CraftingRecipe> craftingRecipes;
    private Map<String, TokenType> extraKeywords = Map.of();
    private SmartActorRuntime smartActorRuntime;
    private PendingEmoteCheck pendingEmoteCheck;
    private InteractionState interactionState = InteractionState.none();
    private UUID conversationActorId;
    private String conversationActorLabel = "";

    public GameRuntime(SceneNarrator narrator, Consumer<String> emitter, boolean aiEnabled) {
        this.narrator = narrator;
        this.emitter = emitter;
        this.aiEnabled = aiEnabled;
    }

    public void configure(KernelRegistry registry,
                          UUID currentPlot,
                          UUID playerId,
                          List<Item> inventory,
                          Map<UUID, Map<UUID, Rectangle2D>> inventoryPlacements,
                          LoopRuntime loopRuntime,
                          TriggerEngine triggerEngine,
                          Map<String, CraftingRecipe> craftingRecipes,
                          Map<String, TokenType> extraKeywords) {
        this.registry = registry;
        this.currentPlot = currentPlot;
        this.playerId = playerId;
        this.inventory = inventory == null ? new ArrayList<>() : inventory;
        this.inventoryPlacements = inventoryPlacements == null ? new HashMap<>() : inventoryPlacements;
        this.loopRuntime = loopRuntime;
        this.triggerEngine = triggerEngine;
        this.craftingRecipes = craftingRecipes;
        this.extraKeywords = extraKeywords == null ? Map.of() : extraKeywords;
        this.pendingEmoteCheck = null;
        this.interactionState = InteractionState.none();
        this.conversationActorId = null;
        this.conversationActorLabel = "";
    }

    public void configureSmartActors(SmartActorRuntime smartActorRuntime) {
        this.smartActorRuntime = smartActorRuntime;
    }

    public void setOutputSuppressed(boolean outputSuppressed) {
        this.outputSuppressed = outputSuppressed;
    }

    public KernelRegistry registry() {
        return registry;
    }

    public UUID currentPlotId() {
        return currentPlot;
    }

    public UUID playerId() {
        return playerId;
    }

    public List<Item> inventory() {
        return inventory;
    }

    public Plot currentPlot() {
        if (registry == null || currentPlot == null) {
            return null;
        }
        return registry.get(currentPlot) instanceof Plot plot ? plot : null;
    }

    public String lastSceneState() {
        return narrator == null ? "" : narrator.lastState();
    }

    public void setCurrentPlot(UUID plotId) {
        this.currentPlot = plotId;
        if (registry != null && playerId != null && plotId != null) {
            registry.moveOwnership(playerId, plotId);
        }
    }

    @FunctionalInterface
    public interface ActorAction<T> {
        T run() throws GameBuilderException;
    }

    public <T> T runAsActor(UUID actorId, boolean suppressOutput, boolean updateOwner, ActorAction<T> action) throws GameBuilderException {
        if (registry == null || actorId == null || action == null) {
            return null;
        }
        Actor actor = registry.get(actorId) instanceof Actor found ? found : null;
        if (actor == null) {
            return null;
        }
        UUID savedPlayerId = playerId;
        UUID savedPlot = currentPlot;
        List<Item> savedInventory = inventory;
        Map<UUID, Map<UUID, Rectangle2D>> savedPlacements = inventoryPlacements;
        boolean savedSuppress = outputSuppressed;
        KernelRegistry savedRegistry = registry;

        UUID actorPlot = actor.getOwnerId();
        List<Item> actorInventory = startingInventory(registry, actorId);
        Map<UUID, Map<UUID, Rectangle2D>> actorPlacements = new HashMap<>();

        playerId = actorId;
        currentPlot = actorPlot;
        inventory = actorInventory;
        inventoryPlacements = actorPlacements;
        outputSuppressed = suppressOutput;
        seedInventoryPlacements(actorInventory, actorPlacements);

        T result = action.run();

        UUID actorPlotAfter = currentPlot;
        boolean registryChanged = registry != savedRegistry;
        if (!registryChanged && updateOwner && actorPlotAfter != null) {
            registry.moveOwnership(actorId, actorPlotAfter);
        }

        outputSuppressed = savedSuppress;
        if (!registryChanged) {
            playerId = savedPlayerId;
            currentPlot = savedPlot;
            inventory = savedInventory;
            inventoryPlacements = savedPlacements;
        }
        return result;
    }

    public void setEncounter(CombatEncounter encounter) {
        this.encounter = encounter;
    }

    public boolean inCombat() {
        return encounter != null && encounter.getState() == CombatState.ACTIVE;
    }

    public List<String> visibleFixtureLabels() {
        if (registry == null || currentPlot == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(Item::isVisible)
                .filter(item -> currentPlot.equals(item.getOwnerId()))
                .map(Item::getLabel)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> visibleItemLabels() {
        if (registry == null || currentPlot == null) {
            return List.of();
        }
        List<Item> plotItems = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> !item.isFixture())
                .filter(Item::isVisible)
                .filter(item -> currentPlot.equals(item.getOwnerId()))
                .toList();
        List<Item> fixtureItems = itemsInOpenFixturesAtPlot();
        return java.util.stream.Stream.concat(plotItems.stream(), fixtureItems.stream())
                .map(Item::getLabel)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> inventoryLabels() {
        if (inventory == null || inventory.isEmpty()) {
            return List.of();
        }
        return inventory.stream()
                .map(Item::getLabel)
                .filter(Objects::nonNull)
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    public List<String> visibleActorLabels(UUID excludeActorId) {
        if (registry == null || currentPlot == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(actor -> currentPlot.equals(actor.getOwnerId()))
                .filter(actor -> excludeActorId == null || !excludeActorId.equals(actor.getId()))
                .map(Actor::getLabel)
                .filter(Objects::nonNull)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private List<Item> openFixturesAtPlot() {
        if (registry == null || currentPlot == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(Item::isVisible)
                .filter(item -> currentPlot.equals(item.getOwnerId()))
                .filter(item -> isThingOpen(item, registry, playerId, currentPlot))
                .toList();
    }

    private List<Item> itemsInOpenFixturesAtPlot() {
        List<Item> openFixtures = openFixturesAtPlot();
        if (openFixtures.isEmpty() || registry == null) {
            return List.of();
        }
        Set<UUID> openFixtureIds = openFixtures.stream()
                .map(Item::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (openFixtureIds.isEmpty()) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> !item.isFixture())
                .filter(Item::isVisible)
                .filter(item -> openFixtureIds.contains(item.getOwnerId()))
                .toList();
    }

    public void explore() {
        consumeCell(registry, playerId, "STAMINA", 1);
        boolean success = KeyExpressionEvaluator.evaluate(
                "DICE(6) >= 4",
                KeyExpressionEvaluator.registryHasResolver(registry, currentPlot),
                null,
                null,
                KeyExpressionEvaluator.registryAttributeResolver(registry, currentPlot),
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
        // Hidden items and actors at this plot are discoverable via SEARCH (used to reveal things like hidden hatchet or Scratch).
        List<Item> hiddenItems = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> !item.isVisible())
                .filter(item -> currentPlot.equals(item.getOwnerId()))
                .toList();

        List<Actor> hiddenActors = registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(actor -> !actor.isVisible())
                .filter(actor -> currentPlot.equals(actor.getOwnerId()))
                .toList();

        if (success && (!hiddenItems.isEmpty() || !hiddenActors.isEmpty())) {
            if (!hiddenItems.isEmpty()) {
                Item reveal = hiddenItems.get(0);
                reveal.setVisible(true);
                narrate("You rummage around and uncover: " + reveal.getLabel());
            }
            if (!hiddenActors.isEmpty()) {
                hiddenActors.forEach(a -> a.setVisible(true));
                if (hiddenItems.isEmpty()) {
                    narrate("Something stirs nearby...");
                }
            }
        } else if (success) {
            String breadcrumb = firstExitDirection();
            if (breadcrumb.isBlank()) {
                narrate("You scour the area but nothing new turns up.");
            } else {
                narrate("You scour the area; tracks point " + breadcrumb + ".");
            }
        } else {
            narrate("You poke around and stir up dust, but find nothing useful.");
        }
    }

    public void craft(String target) {
        if (target.isBlank()) {
            CraftingTable table = new CraftingTable(registry, playerId, craftingRecipes);
            String names = table.getRecipes().values().stream()
                    .map(CraftingRecipe::emitLabel)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("(none)");
            narrateMetaLines(List.of(
                    "Known recipes: " + names + ".",
                    "Try HOW CRAFT <item> for details."
            ));
            return;
        }
        CraftingTable table = new CraftingTable(registry, playerId, craftingRecipes);
        try {
            boolean crafted = table.craft(target);
            if (crafted) {
                refreshInventory();
                narrateMeta("You craft a " + target + ".");
            } else {
                var missing = table.missingRequirements(target);
                if (table.findRecipe(target) == null) {
                    String names = table.getRecipes().values().stream()
                            .map(CraftingRecipe::emitLabel)
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("(none)");
                    narrateMeta("No recipe for that. Known craftables: " + names + ".");
                    return;
                }
                if (!missing.isEmpty()) {
                    String needs = String.join(", ", missing);
                    narrateMeta("You still need: " + needs + ".");
                } else {
                    narrateMeta("You lack the ingredients or know-how to craft that.");
                }
            }
        } catch (Exception ex) {
            narrateMeta("Crafting failed: " + ex.getMessage());
        }
    }

    public void how(String argument) {
        if (argument.isBlank()) {
            CraftingTable table = new CraftingTable(registry, playerId, craftingRecipes);
            String names = table.getRecipes().values().stream()
                    .map(CraftingRecipe::emitLabel)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("(none)");
            narrateMetaLines(List.of(
                    "Known craft targets: " + names + ".",
                    "Use HOW CRAFT <item> to see skill and ingredients."
            ));
            return;
        }
        String trimmed = normalizeHowCraftTarget(argument);
        if (trimmed.isBlank()) {
            narrateMeta("Which item? Try HOW CRAFT TORCH.");
            return;
        }

        CraftingTable table = new CraftingTable(registry, playerId, craftingRecipes);
        CraftingRecipe recipe = table.findRecipe(trimmed);
        if (recipe == null) {
            String names = table.getRecipes().values().stream()
                    .map(CraftingRecipe::emitLabel)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("(none)");
            narrateMeta("No known way to craft '" + trimmed + "'. Known: " + names + ".");
            return;
        }

        SkillResolver skillResolver = KeyExpressionEvaluator.registrySkillResolver(registry, playerId);
        boolean skillRequired = recipe.skillTag() != null && !recipe.skillTag().isBlank();
        boolean hasSkill = !skillRequired || skillResolver.hasSkill(recipe.skillTag());

        List<String> missing = table.missingRequirements(recipe.name());
        HashSet<String> missingNormalized = new HashSet<>();
        for (String m : missing) {
            missingNormalized.add(m.trim().toLowerCase(Locale.ROOT));
        }

        List<String> lines = new ArrayList<>();
        lines.add("How to craft " + recipe.emitLabel() + ":");
        if (skillRequired) {
            lines.add("  Skill: " + recipe.skillTag() + (hasSkill ? " (known)" : " (missing)") + ".");
        }
        if (!recipe.requirements().isEmpty()) {
            List<String> parts = new ArrayList<>();
            for (String req : recipe.requirements()) {
                if (missingNormalized.contains(req.toLowerCase(Locale.ROOT))) {
                    parts.add(req + " (missing)");
                } else {
                    parts.add(req);
                }
            }
            lines.add("  Ingredients: " + String.join(", ", parts));
        }

        if (missing.isEmpty()) {
            lines.add("  Status: ready to craft with CRAFT " + recipe.emitLabel() + ".");
        } else {
            lines.add("  Status: missing " + String.join(", ", missing) + ".");
        }
        narrateMetaLines(lines);
    }

    public void lookDirectionOrThing(String arg) {
        Direction dir = parseDirection(arg);
        if (dir == null) {
            String target = arg == null ? "" : arg.trim();
            if (target.isBlank()) {
                narrate("Nothing else stands out.");
                return;
            }

            String targetLower = target.toLowerCase(Locale.ROOT);

            Item plotItem = registry.getEverything().values().stream()
                    .filter(Item.class::isInstance)
                    .map(Item.class::cast)
                    .filter(Item::isVisible)
                    .filter(i -> currentPlot.equals(i.getOwnerId()))
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (plotItem != null) {
                describeThing(plotItem);
                return;
            }

            Item fixtureItem = itemsInOpenFixturesAtPlot().stream()
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (fixtureItem != null) {
                describeThing(fixtureItem);
                return;
            }

            Item fixture = registry.getEverything().values().stream()
                    .filter(Item.class::isInstance)
                    .map(Item.class::cast)
                    .filter(Item::isFixture)
                    .filter(Item::isVisible)
                    .filter(i -> currentPlot.equals(i.getOwnerId()))
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (fixture != null) {
                describeThing(fixture);
                return;
            }

            Actor actor = registry.getEverything().values().stream()
                    .filter(Actor.class::isInstance)
                    .map(Actor.class::cast)
                    .filter(Actor::isVisible)
                    .filter(a -> currentPlot.equals(a.getOwnerId()))
                    .filter(a -> a.getLabel() != null && a.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (actor != null) {
                describeThing(actor);
                return;
            }

            Item carried = registry.getEverything().values().stream()
                    .filter(Item.class::isInstance)
                    .map(Item.class::cast)
                    .filter(i -> playerId.equals(i.getOwnerId()))
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                    .findFirst()
                    .orElse(null);
            if (carried != null) {
                describeThing(carried);
                return;
            }

            Plot current = registry.get(currentPlot) instanceof Plot plot ? plot : null;
            if (current != null && current.getDescription() != null &&
                    current.getDescription().toLowerCase(Locale.ROOT).contains(targetLower)) {
                narrate("You focus on the " + target + ". It stands out just as described.");
            } else {
                narrate("Nothing else stands out about " + target + ".");
            }
            return;
        }
        Gate gate = exits().stream()
                .filter(g -> dir.equals(g.directionFrom(currentPlot)))
                .findFirst()
                .orElse(null);
        if (gate == null) {
            narrate("Nothing special to the " + dir.toLongName().toLowerCase(Locale.ROOT) + ".");
            return;
        }
        String gateDesc = stripGateDestinationTag(gate.getDescriptionFrom(currentPlot));
        if (gateDesc != null && !gateDesc.isBlank()) {
            narrate(formatDirectionLook(dir, gateDesc));
        } else {
            narrate(formatDirectionLook(dir, "You see an exit."));
        }
    }

    public boolean isConversationActive() {
        return conversationActorId != null;
    }

    public String conversationActorLabel() {
        return conversationActorLabel == null ? "" : conversationActorLabel;
    }

    public InteractionState interactionState() {
        return interactionState == null ? InteractionState.none() : interactionState;
    }

    public enum MentionResolutionType {
        MATCH,
        AMBIGUOUS,
        NONE
    }

    public record MentionResolution(MentionResolutionType type, UUID actorId, String actorLabel, int tokensMatched) {
        public static MentionResolution none() {
            return new MentionResolution(MentionResolutionType.NONE, null, "", 0);
        }

        public static MentionResolution ambiguous() {
            return new MentionResolution(MentionResolutionType.AMBIGUOUS, null, "", 0);
        }
    }

    public MentionResolution resolveMentionActor(List<String> tokens) {
        if (registry == null || currentPlot == null || tokens == null || tokens.isEmpty()) {
            return MentionResolution.none();
        }
        List<String> mentionTokens = normalizeTokens(tokens);
        if (mentionTokens.isEmpty()) {
            return MentionResolution.none();
        }
        List<Actor> actors = visibleActorsAtPlot(registry, currentPlot);
        if (actors.isEmpty()) {
            return MentionResolution.none();
        }
        List<MentionCandidate> candidates = new ArrayList<>();
        for (Actor actor : actors) {
            int matchLength = mentionMatchLength(actor, mentionTokens);
            if (matchLength > 0) {
                candidates.add(new MentionCandidate(actor, matchLength));
            }
        }
        if (candidates.isEmpty()) {
            return MentionResolution.none();
        }
        int best = candidates.stream().mapToInt(MentionCandidate::tokensMatched).max().orElse(0);
        List<MentionCandidate> top = candidates.stream()
                .filter(candidate -> candidate.tokensMatched() == best)
                .toList();
        if (top.size() != 1) {
            return MentionResolution.ambiguous();
        }
        Actor actor = top.get(0).actor();
        String label = actor.getLabel();
        String safeLabel = label == null ? "" : label.trim();
        return new MentionResolution(MentionResolutionType.MATCH, actor.getId(), safeLabel, best);
    }

    public void endConversation() {
        if (conversationActorId == null) {
            return;
        }
        conversationActorId = null;
        conversationActorLabel = "";
        narrate("You end the conversation.");
    }

    public void talk(String target) {
        if (target == null || target.isBlank()) {
            narrate("Talk to whom?");
            return;
        }
        Actor actor = findVisibleActorByKeyOrLabel(registry, currentPlot, target);
        if (actor == null) {
            narrate("You don't see " + target + " here.");
            return;
        }
        conversationActorId = actor.getId();
        String label = actor.getLabel();
        String safeLabel = label == null || label.isBlank() ? "someone" : label;
        conversationActorLabel = safeLabel;
        narrate("You turn to " + safeLabel + ".");
    }

    public void talkToConversation(String playerUtterance) {
        if (conversationActorId == null) {
            narrate("No one is listening.");
            return;
        }
        Actor actor = registry == null ? null : registry.get(conversationActorId) instanceof Actor found ? found : null;
        if (actor == null || !actor.isVisible() || currentPlot == null || !currentPlot.equals(actor.getOwnerId())) {
            conversationActorId = null;
            conversationActorLabel = "";
            narrate("No one answers.");
            return;
        }
        talkToActor(actor, playerUtterance);
    }

    private void talkToActor(Actor actor, String playerUtterance) {
        if (actor == null) {
            narrate("No one answers.");
            return;
        }
        String label = actor.getLabel() == null || actor.getLabel().isBlank() ? "Someone" : actor.getLabel();
        if (!aiEnabled || smartActorRuntime == null || !smartActorRuntime.handlesActor(actor.getId())) {
            narrate(label + " has nothing to say.");
            return;
        }
        SmartActorDecision decision;
        try {
            decision = smartActorRuntime.respondToPlayer(this, actor.getId(), playerUtterance);
        } catch (GameBuilderException ex) {
            narrate(label + " has nothing to say.");
            return;
        }
        if (decision == null) {
            narrate(label + " has nothing to say.");
            return;
        }
        String reply = switch (decision.type()) {
            case COLOR -> decision.color();
            case UTTERANCE -> decision.utterance();
            case NONE -> "";
        };
        String cleaned = reply == null ? "" : reply.replace("\r", " ").replace("\n", " ").trim();
        if (cleaned.isBlank()) {
            narrate(label + " has nothing to say.");
            return;
        }
        narrate(label + ": " + cleaned);
    }

    private String formatDirectionLook(Direction dir, String description) {
        String prefix = directionPrefix(dir);
        String desc = description == null ? "" : description.trim();
        if (desc.isEmpty()) {
            desc = "Nothing special.";
        }
        return prefix + ": " + desc;
    }

    private String directionPrefix(Direction dir) {
        if (dir == null) {
            return "There";
        }
        return switch (dir) {
            case UP -> "Up";
            case DOWN -> "Down";
            default -> "To the " + directionLabel(dir);
        };
    }

    private String directionLabel(Direction dir) {
        String raw = dir == null ? "" : dir.toLongName();
        return raw.toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String stripGateDestinationTag(String description) {
        if (description == null || description.isBlank()) {
            return description;
        }
        String trimmed = description.trim();
        int idx = trimmed.toLowerCase(Locale.ROOT).lastIndexOf(" to:");
        if (idx == -1) {
            return trimmed;
        }
        String before = trimmed.substring(0, idx).trim();
        String dest = trimmed.substring(idx + 4).trim();
        if (dest.isEmpty()) {
            return trimmed;
        }
        if (before.toLowerCase(Locale.ROOT).contains(dest.toLowerCase(Locale.ROOT))) {
            return before;
        }
        return trimmed;
    }

    public void describe() {
        String rawScene = buildSceneSnapshot();
        if (rawScene.isBlank()) {
            narrate("(unknown location)");
            return;
        }
        narrate(rawScene);
        if (!outputSuppressed && narrator != null) {
            narrator.updateScene(rawScene);
        }
    }

    public void primeScene() {
        if (narrator == null) {
            return;
        }
        String rawScene = buildSceneSnapshot();
        if (!rawScene.isBlank()) {
            narrator.updateScene(rawScene);
        }
    }

    private String buildSceneSnapshot() {
        Plot plot = registry == null || currentPlot == null ? null : registry.get(currentPlot) instanceof Plot current ? current : null;
        if (plot == null) {
            return "";
        }
        StringBuilder snapshot = new StringBuilder();
        if (plot.getLabel() != null && !plot.getLabel().isBlank()) {
            snapshot.append("# ").append(plot.getLabel().trim()).append("\n");
        }
        if (plot.getDescription() != null && !plot.getDescription().isBlank()) {
            snapshot.append(plot.getDescription().trim()).append("\n");
        }

        List<Item> fixtures = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(Item::isVisible)
                .filter(item -> currentPlot.equals(item.getOwnerId()))
                .toList();
        if (!fixtures.isEmpty()) {
            snapshot.append("Fixtures:\n");
            fixtures.forEach(f -> snapshot.append("- ").append(f.getLabel()).append("\n"));
        }

        List<Item> plotItems = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(item -> !item.isFixture())
                .filter(Item::isVisible)
                .filter(item -> currentPlot.equals(item.getOwnerId()))
                .toList();
        Map<UUID, Item> itemsById = new LinkedHashMap<>();
        plotItems.forEach(item -> itemsById.put(item.getId(), item));
        itemsInOpenFixturesAtPlot().forEach(item -> itemsById.putIfAbsent(item.getId(), item));
        List<Item> items = new ArrayList<>(itemsById.values());
        if (!items.isEmpty()) {
            snapshot.append("Items:\n");
            items.forEach(i -> snapshot.append("- ").append(i.getLabel()).append("\n"));
        }

        List<Actor> actors = registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(actor -> currentPlot.equals(actor.getOwnerId()))
                .filter(Actor::isVisible)
                .filter(actor -> !actor.getId().equals(playerId))
                .toList();
        if (!actors.isEmpty()) {
            snapshot.append("You see:\n");
            actors.forEach(a -> snapshot.append("- ").append(a.getLabel()).append("\n"));
        }

        List<Gate> exits = exits();
        if (!exits.isEmpty()) {
            List<String> dirList = exits.stream()
                    .map(g -> {
                        Direction d = g.directionFrom(currentPlot);
                        if (d == null) {
                            return null;
                        }
                        return d.toLongName();
                    })
                    .filter(Objects::nonNull)
                    .sorted()
                    .toList();
            if (!dirList.isEmpty()) {
                String separator = " \u2022 ";
                snapshot.append("Exits: ").append(String.join(separator, dirList)).append("\n");
            }
        }
        return snapshot.toString().trim();
    }

    public Item take(String name) {
        if (name.isBlank()) {
            narrate("Take what?");
            return null;
        }
        Item item = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> !i.isFixture())
                .filter(Item::isVisible)
                .filter(i -> currentPlot.equals(i.getOwnerId()))
                .filter(i -> i.getLabel().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (item == null) {
            item = itemsInOpenFixturesAtPlot().stream()
                    .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(name))
                    .findFirst()
                    .orElse(null);
        }
        if (item == null) {
            narrate("No such item here.");
            return null;
        }
        Item container = primaryContainer(inventory);
        if (!fitsInventory(item, container, inventoryPlacements)) {
            String destLabel = container == null ? "pack" : container.getLabel();
            narrate("You can't fit the " + item.getLabel() + " in your " + destLabel + ".");
            return null;
        }
        registry.moveOwnership(item.getId(), playerId);
        inventory.add(item);
        narrate("You take the " + item.getLabel() + ".");
        return item;
    }

    public void drop(String name) {
        if (name.isBlank()) {
            narrate("Drop what?");
            return;
        }
        Item item = inventory.stream()
                .filter(i -> i.getLabel().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (item == null) {
            narrate("You aren't carrying that.");
            return;
        }
        inventory.remove(item);
        inventoryPlacements.values().forEach(m -> m.remove(item.getId()));
        registry.moveOwnership(item.getId(), currentPlot);
        item.setVisible(true);
        narrate("You drop the " + item.getLabel() + ".");
    }

    public void open(String target) {
        if (target == null || target.isBlank()) {
            narrate("Open what?");
            return;
        }
        String trimmed = target.trim();
        Direction dir = parseDirection(trimmed);
        if (dir != null) {
            Gate gate = exits().stream()
                    .filter(g -> dir.equals(g.directionFrom(currentPlot)))
                    .findFirst()
                    .orElse(null);
            if (gate == null) {
                narrate("No door that way.");
                return;
            }
            if (isGateOpen(gate, registry, playerId, currentPlot)) {
                narrate("It's already open.");
            } else {
                narrate("It won't open.");
            }
            return;
        }
        String targetLower = trimmed.toLowerCase(Locale.ROOT);
        Gate gateByLabel = exits().stream()
                .filter(g -> g.getLabel() != null && g.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (gateByLabel != null) {
            if (isGateOpen(gateByLabel, registry, playerId, currentPlot)) {
                narrate("It's already open.");
            } else {
                narrate("It won't open.");
            }
            return;
        }

        Item item = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isVisible)
                .filter(i -> currentPlot.equals(i.getOwnerId()) || playerId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (item != null) {
            if (isThingOpen(item, registry, playerId, currentPlot)) {
                narrate("It's already open.");
            } else {
                String result = Open.open(item);
                narrate(result.isBlank() ? "You open it." : result);
            }
            return;
        }

        narrate("You don't see that here.");
    }

    public UseResult use(String target, String preposition, String object) {
        if (target == null || target.isBlank()) {
            narrate("Use what?");
            return UseResult.invalid();
        }
        String trimmed = target.trim();
        Thing source = findThingByLabel(registry, currentPlot, playerId, trimmed);
        if (source == null) {
            narrate("You don't see that here.");
            return UseResult.invalid();
        }
        if (object != null && !object.isBlank()) {
            String destLabel = object.trim();
            Thing dest = findThingByLabel(registry, currentPlot, playerId, destLabel);
            if (dest == null) {
                narrate("You don't see that here.");
                return UseResult.invalid();
            }
            int transferred = transferAllCells(registry, source, dest);
            if (transferred == 0) {
                String prep = preposition == null ? "on" : preposition;
                narrate("You try to use " + trimmed + " " + prep + " " + destLabel + ", but nothing happens.");
            } else {
                narrate("You use " + trimmed + " on " + destLabel + ".");
            }
            return new UseResult(source, dest, true);
        }
        if (source.getCells().isEmpty()) {
            narrate("You try to use " + trimmed + ", but nothing happens.");
            return new UseResult(source, null, true);
        }
        if (source.getCells().size() > 1) {
            narrate("Use " + trimmed + " on what?");
            return UseResult.invalid();
        }
        var entry = source.getCells().entrySet().iterator().next();
        CellMutationReceipt receipt = CellOps.consume(source, entry.getKey(), 1);
        registry.recordCellMutation(receipt);
        narrate("You use " + trimmed + ".");
        return new UseResult(source, null, true);
    }

    public void attack(String target) {
        if (target == null || target.isBlank()) {
            narrate("Attack what?");
            return;
        }
        Actor attacker = registry == null ? null : registry.get(playerId) instanceof Actor actor ? actor : null;
        if (attacker == null) {
            narrate("That cannot be done right now.");
            return;
        }
        Actor targetActor = findVisibleActorByLabel(registry, currentPlot, target);
        if (targetActor == null) {
            if (registry != null) {
                registry.recordReceipt(new UnknownReferenceReceipt(narrator.lastCommand(), target));
            }
            narrate("I don't know what that is.");
            return;
        }
        if (encounter == null || encounter.getState() != CombatState.ACTIVE || !currentPlot.equals(encounter.getLocationId())) {
            List<Actor> participants = new ArrayList<>(visibleActorsAtPlot(registry, currentPlot));
            if (participants.stream().noneMatch(a -> attacker.getId().equals(a.getId()))) {
                participants.add(attacker);
            }
            if (participants.stream().noneMatch(a -> targetActor.getId().equals(a.getId()))) {
                participants.add(targetActor);
            }
            encounter = CombatEngine.startEncounter(registry, currentPlot, participants, attacker.getId());
            narrate("Combat begins.");
        }
        if (!attacker.getId().equals(encounter.currentActorId())) {
            narrate("It is not your turn.");
            return;
        }
        if (attacker.getId().equals(targetActor.getId())) {
            narrate("You can't attack yourself.");
            return;
        }
        if (!encounter.isActiveParticipant(targetActor.getId())) {
            narrate("They are not here.");
            return;
        }
        if (encounter.isDefeated(targetActor.getId())) {
            narrate("They are already defeated.");
            return;
        }
        CombatEngine.AttackOutcome outcome = CombatEngine.attack(registry, encounter, attacker, targetActor);
        narrateAttackOutcome(attacker, targetActor, outcome, playerId);
        if (outcome.targetDefeated()) {
            narrate(targetActor.getLabel() + " is defeated.");
        }
        String end = CombatEngine.checkEnd(registry, encounter, playerId);
        if (end != null) {
            narrateCombatEnd(end);
            encounter = null;
            return;
        }
        CombatEngine.advanceTurn(registry, encounter);
        resolveNpcTurns(registry, currentPlot, playerId);
    }

    public void flee() {
        if (encounter == null || encounter.getState() != CombatState.ACTIVE || !currentPlot.equals(encounter.getLocationId())) {
            narrate("You are not in combat.");
            return;
        }
        Actor actor = registry == null ? null : registry.get(playerId) instanceof Actor a ? a : null;
        if (actor == null) {
            narrate("That cannot be done right now.");
            return;
        }
        if (!actor.getId().equals(encounter.currentActorId())) {
            narrate("It is not your turn.");
            return;
        }
        CombatEngine.FleeOutcome outcome = CombatEngine.flee(registry, encounter, actor);
        if (outcome.escaped()) {
            narrate("You flee.");
        } else {
            narrate("You fail to flee.");
        }
        String end = CombatEngine.checkEnd(registry, encounter, playerId);
        if (end != null) {
            narrateCombatEnd(end);
            encounter = null;
            return;
        }
        CombatEngine.advanceTurn(registry, encounter);
        resolveNpcTurns(registry, currentPlot, playerId);
    }

    public void put(String target, String object) {
        if (target == null || target.isBlank()) {
            narrate("Put what?");
            return;
        }
        if (object != null && !object.isBlank()) {
            narrate("You can't put items into containers yet.");
            return;
        }
        drop(target);
    }

    public void showInventory() {
        if (inventory.isEmpty()) {
            emit("You are carrying nothing.");
            return;
        }
        emit("You are carrying:");
        // Build a lookup of ownerId -> contained items so container contents show even if not listed as inventory items.
        Map<UUID, List<Item>> contained = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> i.getOwnerId() != null)
                .collect(java.util.stream.Collectors.groupingBy(Item::getOwnerId));

        for (Item item : inventory) {
            List<Item> contents = contained.getOrDefault(item.getId(), List.of());
            String inner = contents.stream()
                    .map(Item::getLabel)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            String capacityNote = "";
            if (item.getCapacityWidth() > 0 && item.getCapacityHeight() > 0) {
                double used = capacityUsed(item, inventoryPlacements);
                capacityNote = " [space: " + fmtPct(used) + "]";
            }
            emit("- " + item.getLabel() + capacityNote + (inner.isEmpty() ? "" : " (contains: " + inner + ")"));
        }
    }

    public void describeThing(Thing thing) {
        String label = thing.getLabel() == null ? "It" : thing.getLabel();
        String desc = thing.getDescription();
        if (desc == null || desc.isBlank()) {
            narrate(label + ": nothing unusual here.");
        } else {
            narrate(label + ": " + desc);
        }
    }

    public UUID move(Direction direction) {
        return tryMove(direction).nextPlotId();
    }

    public MoveResult tryMove(Direction direction) {
        if (direction == null) {
            return MoveResult.none();
        }
        for (Gate gate : exits()) {
            Direction dir = gate.directionFrom(currentPlot);
            if (dir == direction) {
                if (!isGateOpen(gate, registry, playerId, currentPlot)) {
                    String desc = stripGateDestinationTag(gate.getDescriptionFrom(currentPlot));
                    String reason = (desc == null || desc.isBlank()) ? "That way is blocked." : ensurePeriod(desc);
                    return MoveResult.blocked(reason);
                }
                return MoveResult.moved(gate.otherSide(currentPlot));
            }
        }
        return MoveResult.none();
    }

    public Direction parseDirection(String token) {
        try {
            return Direction.parse(token);
        } catch (Exception ex) {
            return null;
        }
    }

    public TriggerOutcome fireTrigger(TriggerType type, Thing target, Thing object) {
        if (triggerEngine == null || type == null || registry == null) {
            return TriggerOutcome.empty();
        }
        String targetLabel = target == null ? "" : target.getLabel();
        String objectLabel = object == null ? "" : object.getLabel();
        TriggerEvent event = new TriggerEvent(
                type,
                targetLabel,
                objectLabel,
                target == null ? null : target.getId(),
                object == null ? null : object.getId()
        );
        TriggerContext context = new TriggerContext(registry, currentPlot, playerId, findWorldStateId(registry));
        return triggerEngine.fire(event, context);
    }

    public CommandOutcome resolveTriggerOutcome(TriggerOutcome outcome) throws GameBuilderException {
        TriggerResolution resolution = applyTriggerOutcome(outcome);
        if (resolution.endGame()) {
            return CommandOutcome.endGameOutcome();
        }
        ResetContext reset = resolution.reset();
        if (reset != null) {
            updateState(reset);
            return CommandOutcome.skipTurnAdvanceOutcome();
        }
        refreshInventory();
        return CommandOutcome.none();
    }

    public CommandOutcome advanceTurn() throws GameBuilderException {
        Plot turnPlot = registry.get(currentPlot) instanceof Plot current ? current : null;
        TriggerOutcome turnOutcome = fireTrigger(TriggerType.ON_TURN, turnPlot, null);
        CommandOutcome turnResolution = resolveTriggerOutcome(turnOutcome);
        if (turnResolution.endGame() || turnResolution.skipTurnAdvance()) {
            return turnResolution;
        }

        LoopResetReason resetReason = loopRuntime == null ? null : loopRuntime.advanceTurn(registry);
        if (resetReason != null) {
            ResetContext reset = applyLoopReset(resetReason, "");
            if (reset != null) {
                updateState(reset);
                return CommandOutcome.skipTurnAdvanceOutcome();
            }
        }
        if (smartActorRuntime != null) {
            CommandOutcome smartOutcome = smartActorRuntime.advanceTurn(this);
            if (smartOutcome.endGame() || smartOutcome.skipTurnAdvance()) {
                return smartOutcome;
            }
        }
        return CommandOutcome.none();
    }

    public void applyLoopResetIfNeeded(LoopResetReason reason, String message) throws GameBuilderException {
        if (reason == null) {
            return;
        }
        ResetContext reset = applyLoopReset(reason, message);
        if (reset != null) {
            updateState(reset);
        }
    }

    private void updateState(ResetContext reset) {
        this.registry = reset.registry();
        this.currentPlot = reset.plotId();
        this.playerId = reset.playerId();
        this.inventory = reset.inventory();
    }

    private void emit(String text) {
        if (outputSuppressed) {
            return;
        }
        if (emitter != null) {
            emitter.accept(text);
        }
    }

    public void narrate(String text) {
        if (outputSuppressed) {
            return;
        }
        narrator.narrate(text);
    }

    public void emote(String rawEmote) {
        String emoteText = normalizeEmoteText(rawEmote);
        if (emoteText.isBlank()) {
            return;
        }
        if (emoteNeedsCheck(emoteText)) {
            pendingEmoteCheck = new PendingEmoteCheck(emoteText, EMOTE_CHECK_SIDES, EMOTE_CHECK_TARGET);
            interactionState = InteractionState.awaitingDice(formatDiceCall(pendingEmoteCheck.sides(), pendingEmoteCheck.target()));
            narrate(formatCheckRequest(pendingEmoteCheck));
            return;
        }
        pendingEmoteCheck = null;
        interactionState = InteractionState.none();
        narrate(formatEmote(emoteText));
    }

    public void rollDice(String argument) {
        if (outputSuppressed) {
            return;
        }
        if (pendingEmoteCheck == null || interactionState.type() != InteractionType.AWAITING_DICE) {
            interactionState = InteractionState.none();
            narrate("No check to roll.");
            return;
        }
        DiceSpec spec = parseDiceSpec(argument);
        if (spec == null) {
            spec = new DiceSpec(pendingEmoteCheck.sides(), pendingEmoteCheck.target());
        }
        if (!matchesPending(spec, pendingEmoteCheck)) {
            String expected = interactionState.expectedToken();
            if (expected.isBlank()) {
                expected = formatDiceCall(pendingEmoteCheck.sides(), pendingEmoteCheck.target());
            }
            narrate("Roll " + expected + ".");
            return;
        }
        DiceCheckResult result;
        try {
            result = evaluateDiceCheck(spec.sides(), spec.target());
        } catch (RuntimeException ex) {
            narrate(ex.getMessage());
            return;
        }
        String outcome = result.success() ? "SUCCESS" : "FAIL";
        String resolved = formatCheckResult(result.roll(), spec.target(), outcome, pendingEmoteCheck.emoteText());
        pendingEmoteCheck = null;
        interactionState = InteractionState.none();
        narrate(resolved);
    }

    public void narrateColor(String color) {
        if (outputSuppressed) {
            return;
        }
        if (narrator != null) {
            narrator.narrateColor(color);
        }
    }

    private void narrateMetaLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        narrateMeta(String.join("\n", lines));
    }

    private void narrateMeta(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (aiEnabled) {
            emit(text);
            return;
        }
        narrate(text);
    }

    private String normalizeHowCraftTarget(String argument) {
        if (argument == null || argument.isBlank()) {
            return "";
        }
        List<Token> tokens = trimEolTokens(CommandScanner.scan(argument, extraKeywords));
        if (tokens.isEmpty()) {
            return "";
        }
        int start = 0;
        if (tokens.get(0).type == TokenType.MAKE) {
            start = 1;
        }
        return tokens.subList(start, tokens.size()).stream()
                .map(t -> t.lexeme)
                .reduce((a, b) -> a + " " + b)
                .orElse("")
                .trim();
    }

    private List<Token> trimEolTokens(List<Token> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<Token> trimmed = new ArrayList<>(tokens);
        while (!trimmed.isEmpty() && trimmed.get(trimmed.size() - 1).type == TokenType.EOL) {
            trimmed.remove(trimmed.size() - 1);
        }
        return trimmed;
    }

    private String firstExitDirection() {
        return exits().stream()
                .map(g -> g.directionFrom(currentPlot))
                .filter(Objects::nonNull)
                .map(Direction::toLongName)
                .sorted()
                .findFirst()
                .orElse("");
    }

    private void consumeCell(KernelRegistry registry, UUID thingId, String cellName, long delta) {
        if (registry == null || thingId == null || cellName == null || cellName.isBlank()) {
            return;
        }
        Thing thing = registry.get(thingId);
        if (thing == null) {
            return;
        }
        CellMutationReceipt receipt = CellOps.consume(thing, cellName, delta);
        registry.recordCellMutation(receipt);
    }

    private String ensurePeriod(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String t = text.trim();
        return t.endsWith(".") ? t : t + ".";
    }

    private List<Gate> exits() {
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .filter(Gate::isVisible)
                .filter(g -> g.connects(currentPlot))
                .toList();
    }

    private TriggerResolution applyTriggerOutcome(TriggerOutcome outcome) throws GameBuilderException {
        if (outcome == null) {
            return TriggerResolution.none();
        }
        for (String message : outcome.messages()) {
            if (message != null && !message.isBlank()) {
                narrate(message);
            }
        }
        if (outcome.endGame()) {
            return new TriggerResolution(null, true);
        }
        if (outcome.hasReset()) {
            ResetContext reset = applyLoopReset(outcome.resetReason(), outcome.resetMessage());
            return new TriggerResolution(reset, false);
        }
        return TriggerResolution.none();
    }

    private ResetContext applyLoopReset(LoopResetReason reason, String overrideMessage) throws GameBuilderException {
        if (loopRuntime == null || reason == null) {
            return null;
        }
        LoopResetResult reset = loopRuntime.reset(registry, reason);
        KernelRegistry nextRegistry = reset.world().registry();
        UUID nextPlot = reset.world().startPlotId();
        UUID nextPlayer = findPlayerActor(nextRegistry, nextPlot);
        List<Item> nextInventory = new ArrayList<>(startingInventory(nextRegistry, nextPlayer));
        inventoryPlacements.clear();
        seedInventoryPlacements(nextInventory, inventoryPlacements);
        encounter = null;
        narrator.resetScene();
        String message = (overrideMessage != null && !overrideMessage.isBlank()) ? overrideMessage : reset.message();
        if (message != null && !message.isBlank()) {
            narrate(message);
        }
        this.registry = nextRegistry;
        this.currentPlot = nextPlot;
        this.playerId = nextPlayer;
        this.inventory = nextInventory;
        describe();
        return new ResetContext(nextRegistry, nextPlot, nextPlayer, nextInventory);
    }

    private UUID findWorldStateId(KernelRegistry registry) {
        if (registry == null) {
            return null;
        }
        return registry.getEverything().values().stream()
                .filter(t -> t != null && t.getKind() == ThingKind.WORLD)
                .map(Thing::getId)
                .findFirst()
                .orElse(null);
    }

    private boolean isGateOpen(Gate gate, KernelRegistry registry, UUID playerId, UUID plotId) {
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, plotId, playerId);
        return gate.isOpen(
                KeyExpressionEvaluator.registryHasResolver(registry, playerId),
                KeyExpressionEvaluator.registrySearchResolver(registry, playerId),
                attributeResolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
    }

    private boolean isThingOpen(Thing thing, KernelRegistry registry, UUID playerId, UUID plotId) {
        if (thing == null) {
            return false;
        }
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, plotId, playerId);
        return thing.isOpen(
                KeyExpressionEvaluator.registryHasResolver(registry, playerId),
                KeyExpressionEvaluator.registrySearchResolver(registry, playerId),
                attributeResolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
    }

    private Item primaryContainer(List<Item> inventory) {
        return inventory.stream()
                .filter(i -> i.getCapacityWidth() > 0 && i.getCapacityHeight() > 0)
                .max(java.util.Comparator.comparingDouble(i -> i.getCapacityWidth() * i.getCapacityHeight()))
                .orElse(null);
    }

    private boolean fitsInventory(Item item, Item container, Map<UUID, Map<UUID, Rectangle2D>> placements) {
        double capacityWidth = container == null ? 0.3 : container.getCapacityWidth();
        double capacityHeight = container == null ? 0.3 : container.getCapacityHeight();
        UUID bucket = container == null ? (playerId == null ? PLAYER_ID : playerId) : container.getId();

        Map<UUID, Rectangle2D> bucketPlacements = placements.computeIfAbsent(bucket, k -> new HashMap<>());
        List<Rectangle2D> occupied = new ArrayList<>(bucketPlacements.values());

        double width = normalizeFootprint(item.getFootprintWidth(), capacityWidth);
        double height = normalizeFootprint(item.getFootprintHeight(), capacityHeight);

        var placement = ContainerPacker.place(width, height, occupied);
        if (placement.isEmpty()) {
            return false;
        }
        bucketPlacements.put(item.getId(), placement.get().asRectangle());
        return true;
    }

    public void seedInventoryPlacements(List<Item> inventory, Map<UUID, Map<UUID, Rectangle2D>> placements) {
        Item container = primaryContainer(inventory);
        for (Item item : inventory) {
            if (container != null && item.getId().equals(container.getId())) {
                continue;
            }
            fitsInventory(item, container, placements);
        }
    }

    private double normalizeFootprint(double value, double capacity) {
        double v = Math.max(0.01, value);
        double cap = capacity <= 0 ? 1.0 : capacity;
        return Math.min(1.0, v / cap);
    }

    private String fmtPct(double used) {
        int pct = (int) Math.round(used * 100.0);
        return pct + "%";
    }

    private double capacityUsed(Item container, Map<UUID, Map<UUID, Rectangle2D>> placements) {
        UUID bucket = container.getId();
        Map<UUID, Rectangle2D> entries = placements.get(bucket);
        if (entries == null || entries.isEmpty()) {
            return 0.0;
        }
        double used = entries.values().stream()
                .filter(Objects::nonNull)
                .mapToDouble(Rectangle2D::area)
                .sum();
        return Math.min(1.0, used);
    }

    private void refreshInventory() {
        if (registry == null || playerId == null) {
            inventory.clear();
            inventoryPlacements.clear();
            return;
        }
        Map<UUID, Item> ownedItems = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> !i.isFixture())
                .filter(i -> playerId.equals(i.getOwnerId()))
                .collect(Collectors.toMap(Item::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        if (ownedItems.isEmpty()) {
            inventory.clear();
            inventoryPlacements.clear();
            return;
        }
        List<Item> refreshed = new ArrayList<>();
        for (Item item : inventory) {
            if (item == null) {
                continue;
            }
            Item owned = ownedItems.remove(item.getId());
            if (owned != null) {
                refreshed.add(owned);
            }
        }
        refreshed.addAll(ownedItems.values());
        inventory.clear();
        inventory.addAll(refreshed);
        inventoryPlacements.clear();
        seedInventoryPlacements(inventory, inventoryPlacements);
    }

    private Actor findVisibleActorByLabel(KernelRegistry registry, UUID plotId, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        String targetLower = label.trim().toLowerCase(Locale.ROOT);
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(a -> plotId.equals(a.getOwnerId()))
                .filter(a -> a.getLabel() != null && a.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
    }

    private Actor findVisibleActorByKeyOrLabel(KernelRegistry registry, UUID plotId, String labelOrKey) {
        Actor actor = findVisibleActorByLabel(registry, plotId, labelOrKey);
        if (actor != null) {
            return actor;
        }
        UUID actorId = actorIdForKey(labelOrKey);
        if (actorId == null || registry == null || plotId == null) {
            return null;
        }
        Actor byId = registry.get(actorId) instanceof Actor found ? found : null;
        if (byId == null || !byId.isVisible() || !plotId.equals(byId.getOwnerId())) {
            return null;
        }
        return byId;
    }

    private int mentionMatchLength(Actor actor, List<String> mentionTokens) {
        if (actor == null || mentionTokens == null || mentionTokens.isEmpty()) {
            return 0;
        }
        int best = 0;
        List<String> labelTokens = tokenizeLabel(actor.getLabel());
        if (!labelTokens.isEmpty()) {
            best = Math.max(best, longestPrefixMatch(mentionTokens, labelTokens));
            if (labelTokens.contains(mentionTokens.get(0))) {
                best = Math.max(best, 1);
            }
        }
        best = Math.max(best, keyMatchLength(actor.getId(), mentionTokens));
        return best;
    }

    private int longestPrefixMatch(List<String> left, List<String> right) {
        if (left == null || right == null) {
            return 0;
        }
        int max = Math.min(left.size(), right.size());
        int matched = 0;
        for (int i = 0; i < max; i++) {
            if (!left.get(i).equals(right.get(i))) {
                break;
            }
            matched = i + 1;
        }
        return matched;
    }

    private int keyMatchLength(UUID actorId, List<String> mentionTokens) {
        if (actorId == null || mentionTokens == null || mentionTokens.isEmpty()) {
            return 0;
        }
        int best = 0;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mentionTokens.size(); i++) {
            if (i > 0) {
                sb.append(' ');
            }
            sb.append(mentionTokens.get(i));
            UUID candidate = actorIdForKey(sb.toString());
            if (actorId.equals(candidate)) {
                best = i + 1;
            }
        }
        return best;
    }

    private List<String> normalizeTokens(List<String> tokens) {
        if (tokens == null || tokens.isEmpty()) {
            return List.of();
        }
        List<String> normalized = new ArrayList<>();
        for (String token : tokens) {
            if (token == null) {
                continue;
            }
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            normalized.add(trimmed.toLowerCase(Locale.ROOT));
        }
        return normalized;
    }

    private List<String> tokenizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return List.of();
        }
        List<Token> tokens = CommandScanner.scan(label);
        List<String> words = new ArrayList<>();
        for (Token token : tokens) {
            if (token == null || token.type == TokenType.EOL || token.type == TokenType.HELP) {
                continue;
            }
            if (token.type == TokenType.STRING) {
                splitIntoWords(token.lexeme, words);
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim();
            if (!lexeme.isEmpty()) {
                words.add(lexeme.toLowerCase(Locale.ROOT));
            }
        }
        return words;
    }

    private void splitIntoWords(String lexeme, List<String> words) {
        if (lexeme == null || words == null) {
            return;
        }
        for (String part : lexeme.split("\\s+")) {
            if (part == null) {
                continue;
            }
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                words.add(trimmed.toLowerCase(Locale.ROOT));
            }
        }
    }

    private UUID actorIdForKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        return UUID.nameUUIDFromBytes(("actor:" + normalized).getBytes(StandardCharsets.UTF_8));
    }

    private List<Actor> visibleActorsAtPlot(KernelRegistry registry, UUID plotId) {
        if (registry == null || plotId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(actor -> plotId.equals(actor.getOwnerId()))
                .toList();
    }

    private record MentionCandidate(Actor actor, int tokensMatched) {
    }

    private Thing findThingByLabel(KernelRegistry registry, UUID plotId, UUID playerId, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        String targetLower = label.trim().toLowerCase(Locale.ROOT);
        Thing player = playerId == null ? null : registry.get(playerId);
        if (player != null && player.getLabel() != null && player.getLabel().equalsIgnoreCase(targetLower)) {
            return player;
        }

        Thing inventoryItem = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> playerId != null && playerId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (inventoryItem != null) {
            return inventoryItem;
        }

        Thing plotItem = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> !i.isFixture())
                .filter(Item::isVisible)
                .filter(i -> plotId != null && plotId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (plotItem != null) {
            return plotItem;
        }

        Thing fixture = registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(Item::isFixture)
                .filter(Item::isVisible)
                .filter(i -> plotId != null && plotId.equals(i.getOwnerId()))
                .filter(i -> i.getLabel() != null && i.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
        if (fixture != null) {
            return fixture;
        }

        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(a -> plotId != null && plotId.equals(a.getOwnerId()))
                .filter(a -> a.getLabel() != null && a.getLabel().equalsIgnoreCase(targetLower))
                .findFirst()
                .orElse(null);
    }

    private int transferAllCells(KernelRegistry registry, Thing source, Thing dest) {
        if (registry == null || source == null || dest == null) {
            return 0;
        }
        if (source.getCells().isEmpty()) {
            return 0;
        }
        int transferred = 0;
        for (var entry : source.getCells().entrySet()) {
            String cellName = entry.getKey();
            Cell cell = entry.getValue();
            if (cell == null || cell.getAmount() <= 0) {
                continue;
            }
            if (dest.getCell(cellName) == null) {
                dest.setCell(cellName, new Cell(cell.getCapacity(), 0));
            }
            CellTransferResult result = CellOps.transfer(source, dest, cellName, cell.getAmount());
            if (result != null) {
                registry.recordCellMutation(result.fromReceipt());
                registry.recordCellMutation(result.toReceipt());
                registry.recordCellTransfer(result.transferReceipt());
                if (result.transferReceipt().appliedDelta() > 0) {
                    transferred++;
                }
            }
        }
        return transferred;
    }

    private void resolveNpcTurns(KernelRegistry registry, UUID plotId, UUID playerId) {
        while (encounter != null && encounter.getState() == CombatState.ACTIVE) {
            UUID actorId = encounter.currentActorId();
            if (actorId == null || actorId.equals(playerId)) {
                return;
            }
            if (smartActorRuntime != null && smartActorRuntime.handlesActor(actorId)) {
                try {
                    smartActorRuntime.advanceCombatTurn(this, actorId);
                } catch (GameBuilderException ex) {
                    return;
                }
                if (encounter == null || encounter.getState() != CombatState.ACTIVE) {
                    return;
                }
                continue;
            }
            Actor npc = registry == null ? null : registry.get(actorId) instanceof Actor actor ? actor : null;
            Actor player = registry == null ? null : registry.get(playerId) instanceof Actor actor ? actor : null;
            if (npc == null || player == null) {
                encounter.markDefeated(actorId);
                CombatEngine.advanceTurn(registry, encounter);
                continue;
            }
            CombatEngine.AttackOutcome outcome = CombatEngine.attack(registry, encounter, npc, player);
            narrateAttackOutcome(npc, player, outcome, playerId);
            if (outcome.targetDefeated()) {
                narrate(player.getLabel() + " is defeated.");
            }
            String end = CombatEngine.checkEnd(registry, encounter, playerId);
            if (end != null) {
                narrateCombatEnd(end);
                encounter = null;
                return;
            }
            CombatEngine.advanceTurn(registry, encounter);
        }
    }

    CommandOutcome resolveSmartActorCombatAction(UUID actorId, Command command) {
        if (encounter == null || encounter.getState() != CombatState.ACTIVE) {
            return CommandOutcome.none();
        }
        if (registry == null || actorId == null) {
            return CommandOutcome.none();
        }
        if (!actorId.equals(encounter.currentActorId())) {
            return CommandOutcome.none();
        }
        Actor actor = registry.get(actorId) instanceof Actor found ? found : null;
        if (actor == null) {
            encounter.markDefeated(actorId);
            CombatEngine.advanceTurn(registry, encounter);
            return CommandOutcome.none();
        }
        CommandAction action = command == null ? CommandAction.UNKNOWN : command.action();
        return switch (action) {
            case ATTACK -> resolveSmartActorAttack(actor, command.target());
            case FLEE -> resolveSmartActorFlee(actor);
            default -> resolveSmartActorPass(actor);
        };
    }

    private CommandOutcome resolveSmartActorAttack(Actor actor, String targetLabel) {
        if (actor == null || registry == null || encounter == null) {
            return CommandOutcome.none();
        }
        Actor targetActor = findVisibleActorByLabel(registry, currentPlot, targetLabel);
        if (targetActor == null) {
            return resolveSmartActorPass(actor);
        }
        if (actor.getId().equals(targetActor.getId())) {
            return resolveSmartActorPass(actor);
        }
        if (!encounter.isActiveParticipant(targetActor.getId())) {
            return resolveSmartActorPass(actor);
        }
        if (encounter.isDefeated(targetActor.getId())) {
            narrate(targetActor.getLabel() + " is already defeated.");
            CombatEngine.advanceTurn(registry, encounter);
            return CommandOutcome.none();
        }
        CombatEngine.AttackOutcome outcome = CombatEngine.attack(registry, encounter, actor, targetActor);
        narrateAttackOutcome(actor, targetActor, outcome, playerId);
        if (outcome.targetDefeated()) {
            narrate(targetActor.getLabel() + " is defeated.");
        }
        String end = CombatEngine.checkEnd(registry, encounter, playerId);
        if (end != null) {
            narrateCombatEnd(end);
            encounter = null;
            return CommandOutcome.none();
        }
        CombatEngine.advanceTurn(registry, encounter);
        return CommandOutcome.none();
    }

    private CommandOutcome resolveSmartActorFlee(Actor actor) {
        if (actor == null || registry == null || encounter == null) {
            return CommandOutcome.none();
        }
        CombatEngine.FleeOutcome outcome = CombatEngine.flee(registry, encounter, actor);
        String label = actor.getLabel() == null ? "Someone" : actor.getLabel();
        if (outcome.escaped()) {
            narrate(label + " flees.");
        } else {
            narrate(label + " fails to flee.");
        }
        String end = CombatEngine.checkEnd(registry, encounter, playerId);
        if (end != null) {
            narrateCombatEnd(end);
            encounter = null;
            return CommandOutcome.none();
        }
        CombatEngine.advanceTurn(registry, encounter);
        return CommandOutcome.none();
    }

    private CommandOutcome resolveSmartActorPass(Actor actor) {
        String label = actor == null || actor.getLabel() == null ? "Someone" : actor.getLabel();
        narrate(label + " hesitates.");
        if (encounter != null && encounter.getState() == CombatState.ACTIVE) {
            CombatEngine.advanceTurn(registry, encounter);
        }
        return CommandOutcome.none();
    }

    private void narrateAttackOutcome(Actor attacker, Actor target, CombatEngine.AttackOutcome outcome, UUID playerId) {
        String attackerLabel = attacker == null ? "Someone" : attacker.getLabel();
        String targetLabel = target == null ? "someone" : target.getLabel();
        if (!outcome.hit()) {
            if (attacker != null && attacker.getId().equals(playerId)) {
                narrate("You miss " + targetLabel + ".");
            } else if (target != null && target.getId().equals(playerId)) {
                narrate(attackerLabel + " misses you.");
            } else {
                narrate(attackerLabel + " misses " + targetLabel + ".");
            }
            return;
        }
        String health = formatHealth(outcome);
        if (attacker != null && attacker.getId().equals(playerId)) {
            narrate("You hit " + targetLabel + " for " + outcome.damageApplied() + ". " + targetLabel + " health: " + health + ".");
        } else if (target != null && target.getId().equals(playerId)) {
            narrate(attackerLabel + " hits you for " + outcome.damageApplied() + ". Your health: " + health + ".");
        } else {
            narrate(attackerLabel + " hits " + targetLabel + " for " + outcome.damageApplied() + ". " + targetLabel + " health: " + health + ".");
        }
    }

    private String formatHealth(CombatEngine.AttackOutcome outcome) {
        long capacity = outcome.targetCapacity();
        if (capacity <= 0) {
            return String.valueOf(outcome.targetAmount());
        }
        int pct = (int) Math.round(outcome.targetVolume() * 100.0);
        return outcome.targetAmount() + "/" + capacity + " (" + pct + "%)";
    }

    private void narrateCombatEnd(String outcome) {
        if (outcome == null) {
            narrate("Combat ends.");
            return;
        }
        switch (outcome) {
            case "PLAYER_FLED" -> narrate("You escape the fight.");
            case "PLAYER_DEFEATED" -> narrate("You are defeated.");
            case "VICTORY" -> narrate("You are victorious.");
            default -> narrate("Combat ends.");
        }
    }

    public UUID findPlayerActor(KernelRegistry registry, UUID plotId) {
        return registry.getEverything().values().stream()
                .filter(Actor.class::isInstance)
                .map(Actor.class::cast)
                .filter(Actor::isVisible)
                .filter(actor -> plotId.equals(actor.getOwnerId()))
                .map(Actor::getId)
                .findFirst()
                .orElse(PLAYER_ID);
    }

    public List<Item> startingInventory(KernelRegistry registry, UUID playerId) {
        if (registry == null || playerId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Item.class::isInstance)
                .map(Item.class::cast)
                .filter(i -> playerId.equals(i.getOwnerId()))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private String normalizeEmoteText(String rawEmote) {
        if (rawEmote == null) {
            return "";
        }
        String trimmed = rawEmote.trim();
        if (trimmed.length() >= EMOTE_PREFIX.length()
                && trimmed.regionMatches(true, 0, EMOTE_PREFIX, 0, EMOTE_PREFIX.length())) {
            trimmed = trimmed.substring(EMOTE_PREFIX.length()).trim();
        }
        return trimmed;
    }

    private boolean emoteNeedsCheck(String emoteText) {
        if (emoteText == null || emoteText.isBlank()) {
            return false;
        }
        List<Token> tokens = CommandScanner.scan(emoteText);
        for (Token token : tokens) {
            if (token == null || token.type == TokenType.EOL) {
                continue;
            }
            if (token.type == TokenType.STRING) {
                for (String part : token.lexeme.split("\\s+")) {
                    if (EMOTE_CHECK_KEYWORDS.contains(part.toLowerCase(Locale.ROOT))) {
                        return true;
                    }
                }
                continue;
            }
            if (token.type != TokenType.IDENTIFIER) {
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim().toLowerCase(Locale.ROOT);
            if (EMOTE_CHECK_KEYWORDS.contains(lexeme)) {
                return true;
            }
        }
        return false;
    }

    private DiceSpec parseDiceSpec(String argument) {
        if (argument == null || argument.isBlank()) {
            return null;
        }
        List<Token> tokens = CommandScanner.scan(argument);
        List<Integer> values = new ArrayList<>();
        for (Token token : tokens) {
            if (token == null || token.type == TokenType.EOL) {
                continue;
            }
            if (token.type != TokenType.IDENTIFIER && token.type != TokenType.NUMBER && token.type != TokenType.STRING) {
                continue;
            }
            String lexeme = token.lexeme == null ? "" : token.lexeme.trim();
            if (lexeme.isEmpty()) {
                continue;
            }
            int parsed = parseDiceNumber(lexeme);
            if (parsed > 0) {
                values.add(parsed);
            }
            if (values.size() >= 2) {
                break;
            }
        }
        if (values.size() < 2) {
            return null;
        }
        return new DiceSpec(values.get(0), values.get(1));
    }

    private int parseDiceNumber(String lexeme) {
        String trimmed = lexeme == null ? "" : lexeme.trim();
        if (trimmed.isEmpty()) {
            return -1;
        }
        if (trimmed.startsWith("d") || trimmed.startsWith("D")) {
            trimmed = trimmed.substring(1);
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private DiceCheckResult evaluateDiceCheck(int sides, int target) {
        KeyExpressionEvaluator.DiceRoller previous = KeyExpressionEvaluator.getDefaultDiceRoller();
        List<Integer> rolls = new ArrayList<>();
        KeyExpressionEvaluator.setDefaultDiceRoller(s -> {
            int roll = previous.roll(s);
            rolls.add(roll);
            return roll;
        });
        boolean success;
        try {
            success = KeyExpressionEvaluator.evaluate("DICE(" + sides + ") >= " + target);
        } finally {
            KeyExpressionEvaluator.setDefaultDiceRoller(previous);
        }
        int roll = rolls.isEmpty() ? 0 : rolls.get(rolls.size() - 1);
        return new DiceCheckResult(roll, success);
    }

    private String formatDiceCall(int sides, int target) {
        return "dice(" + sides + ", " + target + ")";
    }

    private String formatEmote(String emoteText) {
        return EMOTE_PREFIX + " " + emoteText;
    }

    private String formatCheckRequest(PendingEmoteCheck check) {
        return CHECK_REQUEST_PREFIX + " " + formatDiceCall(check.sides(), check.target())
                + " | " + EMOTE_PREFIX + " " + check.emoteText();
    }

    private String formatCheckResult(int roll, int target, String outcome, String emoteText) {
        return CHECK_RESULT_PREFIX + " roll=" + roll + " target=" + target + " outcome=" + outcome
                + " | " + EMOTE_PREFIX + " " + emoteText;
    }

    private boolean matchesPending(DiceSpec spec, PendingEmoteCheck pending) {
        if (spec == null || pending == null) {
            return false;
        }
        return spec.sides() == pending.sides() && spec.target() == pending.target();
    }

    private record PendingEmoteCheck(String emoteText, int sides, int target) {
    }

    private record DiceSpec(int sides, int target) {
    }

    private record DiceCheckResult(int roll, boolean success) {
    }

    public enum InteractionType {
        NONE,
        AWAITING_DICE,
        AWAITING_CHOICE,
        AWAITING_CONFIRM
    }

    public record InteractionState(InteractionType type, String expectedToken, String promptLine) {
        public static InteractionState none() {
            return new InteractionState(InteractionType.NONE, "", "");
        }

        public static InteractionState awaitingDice(String diceCall) {
            String expected = diceCall == null ? "" : diceCall.trim();
            String prompt = expected.isEmpty() ? "Roll dice." : "Roll " + expected + ".";
            return new InteractionState(InteractionType.AWAITING_DICE, expected, prompt);
        }
    }
}

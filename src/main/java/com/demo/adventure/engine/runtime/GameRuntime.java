package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.mechanics.cells.CellMutationReceipt;
import com.demo.adventure.engine.mechanics.cells.CellOps;
import com.demo.adventure.engine.mechanics.combat.CombatEncounter;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.Token;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.interpreter.CommandScanner;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.mechanics.crafting.CraftingTable;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator.SkillResolver;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.model.ThingKind;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.engine.flow.loop.LoopResetReason;
import com.demo.adventure.engine.flow.loop.LoopResetResult;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;
import com.demo.adventure.engine.flow.trigger.TriggerOutcome;
import com.demo.adventure.engine.flow.trigger.TriggerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public final class GameRuntime {

    static final UUID PLAYER_ID = UUID.fromString("00000000-0000-0000-0000-00000000feed");

    private final SceneNarrator narrator;
    private final Consumer<String> emitter;
    private final boolean aiEnabled;
    private boolean outputSuppressed;
    private final RuntimeScene scene;
    private final RuntimeInventory inventoryService;
    private final RuntimeConversation conversation;
    private final RuntimeEmoteDice emoteDice;
    private final RuntimeCombat combat;
    private final RuntimeItemUse itemUse;
    private final RuntimeCommandActions commandActions;
    private final RuntimeNavigation navigation;
    private final RuntimeTriggers triggers;

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

    public GameRuntime(SceneNarrator narrator, Consumer<String> emitter, boolean aiEnabled) {
        this.narrator = narrator;
        this.emitter = emitter;
        this.aiEnabled = aiEnabled;
        this.scene = new RuntimeScene(this);
        this.inventoryService = new RuntimeInventory(this);
        this.conversation = new RuntimeConversation(this);
        this.emoteDice = new RuntimeEmoteDice(this);
        this.combat = new RuntimeCombat(this);
        this.itemUse = new RuntimeItemUse(this);
        this.commandActions = new RuntimeCommandActions(this);
        this.navigation = new RuntimeNavigation(this);
        this.triggers = new RuntimeTriggers(this);
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
        this.emoteDice.reset();
        this.conversation.reset();
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

    CombatEncounter encounter() {
        return encounter;
    }

    public List<Item> inventory() {
        return inventory;
    }

    Map<UUID, Map<UUID, Rectangle2D>> inventoryPlacements() {
        return inventoryPlacements;
    }

    boolean aiEnabled() {
        return aiEnabled;
    }

    SmartActorRuntime smartActorRuntime() {
        return smartActorRuntime;
    }

    TriggerEngine triggerEngine() {
        return triggerEngine;
    }

    LoopRuntime loopRuntime() {
        return loopRuntime;
    }

    List<Actor> visibleActorsAtPlot(KernelRegistry registry, UUID plotId) {
        return conversation.visibleActorsAtPlot(registry, plotId);
    }

    String lastCommand() {
        return narrator == null ? "" : narrator.lastCommand();
    }

    boolean isOutputSuppressed() {
        return outputSuppressed;
    }

    void updateScene(String rawScene) {
        if (!outputSuppressed && narrator != null) {
            narrator.updateScene(rawScene);
        }
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
        return combat.inCombat();
    }

    public List<String> visibleFixtureLabels() {
        return scene.visibleFixtureLabels();
    }

    public List<String> visibleItemLabels() {
        return scene.visibleItemLabels();
    }

    public List<String> inventoryLabels() {
        return inventoryService.inventoryLabels();
    }

    public List<String> visibleActorLabels(UUID excludeActorId) {
        return scene.visibleActorLabels(excludeActorId);
    }

    List<Item> itemsInOpenFixturesAtPlot() {
        return scene.itemsInOpenFixturesAtPlot();
    }

    public void explore() {
        commandActions.explore();
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
        scene.lookDirectionOrThing(arg);
    }

    public boolean isConversationActive() {
        return conversation.isConversationActive();
    }

    public String conversationActorLabel() {
        return conversation.conversationActorLabel();
    }

    public InteractionState interactionState() {
        return emoteDice.interactionState();
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
        return conversation.resolveMentionActor(tokens);
    }

    public void endConversation() {
        conversation.endConversation();
    }

    public void talk(String target) {
        conversation.talk(target);
    }

    public void talkToConversation(String playerUtterance) {
        conversation.talkToConversation(playerUtterance);
    }


    public void describe() {
        scene.describe();
    }

    public void primeScene() {
        scene.primeScene();
    }

    public Item take(String name) {
        return inventoryService.take(name);
    }

    public void drop(String name) {
        inventoryService.drop(name);
    }

    public void open(String target) {
        commandActions.open(target);
    }

    public UseResult use(String target, String preposition, String object) {
        return itemUse.use(target, preposition, object);
    }

    public void attack(String target) {
        combat.attack(target);
    }

    public void flee() {
        combat.flee();
    }

    public void put(String target, String object) {
        commandActions.put(target, object);
    }

    public void showInventory() {
        inventoryService.showInventory();
    }

    public void describeThing(Thing thing) {
        scene.describeThing(thing);
    }

    public UUID move(Direction direction) {
        return navigation.move(direction);
    }

    public MoveResult tryMove(Direction direction) {
        return navigation.tryMove(direction);
    }

    public Direction parseDirection(String token) {
        return navigation.parseDirection(token);
    }

    public TriggerOutcome fireTrigger(TriggerType type, Thing target, Thing object) {
        return triggers.fireTrigger(type, target, object);
    }

    public CommandOutcome resolveTriggerOutcome(TriggerOutcome outcome) throws GameBuilderException {
        return triggers.resolveTriggerOutcome(outcome);
    }

    public CommandOutcome advanceTurn() throws GameBuilderException {
        return triggers.advanceTurn();
    }

    public void applyLoopResetIfNeeded(LoopResetReason reason, String message) throws GameBuilderException {
        triggers.applyLoopResetIfNeeded(reason, message);
    }

    void updateState(ResetContext reset) {
        this.registry = reset.registry();
        this.currentPlot = reset.plotId();
        this.playerId = reset.playerId();
        this.inventory = reset.inventory();
    }

    void emit(String text) {
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
        emoteDice.emote(rawEmote);
    }

    public void rollDice(String argument) {
        emoteDice.rollDice(argument);
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

    void consumeCell(KernelRegistry registry, UUID thingId, String cellName, long delta) {
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

    List<Gate> exits() {
        return scene.exits();
    }

    String firstExitDirection() {
        return scene.firstExitDirection();
    }

    String ensurePeriod(String text) {
        return scene.ensurePeriod(text);
    }

    String stripGateDestinationTag(String description) {
        return scene.stripGateDestinationTag(description);
    }

    TriggerResolution applyTriggerOutcome(TriggerOutcome outcome) throws GameBuilderException {
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

    ResetContext applyLoopReset(LoopResetReason reason, String overrideMessage) throws GameBuilderException {
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

    UUID findWorldStateId(KernelRegistry registry) {
        if (registry == null) {
            return null;
        }
        return registry.getEverything().values().stream()
                .filter(t -> t != null && t.getKind() == ThingKind.WORLD)
                .map(Thing::getId)
                .findFirst()
                .orElse(null);
    }

    boolean isGateOpen(Gate gate, KernelRegistry registry, UUID playerId, UUID plotId) {
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, plotId, playerId);
        return gate.isOpen(
                KeyExpressionEvaluator.registryHasResolver(registry, playerId),
                KeyExpressionEvaluator.registrySearchResolver(registry, playerId),
                attributeResolver,
                KeyExpressionEvaluator.AttributeResolutionPolicy.COMPUTE_FALLBACK_ZERO
        );
    }

    boolean isThingOpen(Thing thing, KernelRegistry registry, UUID playerId, UUID plotId) {
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

    public void seedInventoryPlacements(List<Item> inventory, Map<UUID, Map<UUID, Rectangle2D>> placements) {
        inventoryService.seedInventoryPlacements(inventory, placements, playerId);
    }

    void refreshInventory() {
        inventoryService.refreshInventory();
    }

    Actor findVisibleActorByLabel(KernelRegistry registry, UUID plotId, String label) {
        return conversation.findVisibleActorByLabel(registry, plotId, label);
    }

    CommandOutcome resolveSmartActorCombatAction(UUID actorId, Command command) {
        return combat.resolveSmartActorCombatAction(actorId, command);
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
        return inventoryService.startingInventory(registry, playerId);
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

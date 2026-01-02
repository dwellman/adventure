package com.demo.adventure.engine.runtime;

import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.mechanics.combat.CombatEncounter;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Rectangle2D;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.engine.flow.loop.LoopResetReason;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;
import com.demo.adventure.engine.flow.trigger.TriggerOutcome;
import com.demo.adventure.engine.flow.trigger.TriggerType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    private final RuntimeCrafting crafting;
    private final RuntimeCombat combat;
    private final RuntimeItemUse itemUse;
    private final RuntimeCommandActions commandActions;
    private final RuntimeNavigation navigation;
    private final RuntimeTriggers triggers;
    private final RuntimeResets resets;
    private final RuntimeActorContext actorContext;
    private final RuntimeKernelOps kernelOps;

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
        this.crafting = new RuntimeCrafting(this);
        this.combat = new RuntimeCombat(this);
        this.itemUse = new RuntimeItemUse(this);
        this.commandActions = new RuntimeCommandActions(this);
        this.navigation = new RuntimeNavigation(this);
        this.triggers = new RuntimeTriggers(this);
        this.resets = new RuntimeResets(this);
        this.actorContext = new RuntimeActorContext(this);
        this.kernelOps = new RuntimeKernelOps(this);
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

    void replaceInventoryPlacements(Map<UUID, Map<UUID, Rectangle2D>> placements) {
        inventoryPlacements = placements == null ? new HashMap<>() : placements;
    }

    Map<String, CraftingRecipe> craftingRecipes() {
        return craftingRecipes;
    }

    Map<String, TokenType> extraKeywords() {
        return extraKeywords;
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

    void resetNarratorScene() {
        if (narrator != null) {
            narrator.resetScene();
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
        return actorContext.runAsActor(actorId, suppressOutput, updateOwner, action);
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
        crafting.craft(target);
    }

    public void how(String argument) {
        crafting.how(argument);
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

    void narrateMetaLines(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return;
        }
        narrateMeta(String.join("\n", lines));
    }

    void narrateMeta(String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (aiEnabled) {
            emit(text);
            return;
        }
        narrate(text);
    }


    void consumeCell(KernelRegistry registry, UUID thingId, String cellName, long delta) {
        kernelOps.consumeCell(registry, thingId, cellName, delta);
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
        return resets.applyTriggerOutcome(outcome);
    }

    ResetContext applyLoopReset(LoopResetReason reason, String overrideMessage) throws GameBuilderException {
        return resets.applyLoopReset(reason, overrideMessage);
    }

    UUID findWorldStateId(KernelRegistry registry) {
        return kernelOps.findWorldStateId(registry);
    }

    boolean isGateOpen(Gate gate, KernelRegistry registry, UUID playerId, UUID plotId) {
        return kernelOps.isGateOpen(gate, registry, playerId, plotId);
    }

    boolean isThingOpen(Thing thing, KernelRegistry registry, UUID playerId, UUID plotId) {
        return kernelOps.isThingOpen(thing, registry, playerId, plotId);
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

}

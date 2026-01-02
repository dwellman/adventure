package com.demo.adventure.engine.integrity;

import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.engine.cli.RuntimeLoader;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerAction;
import com.demo.adventure.engine.flow.trigger.TriggerActionType;
import com.demo.adventure.engine.flow.trigger.TriggerDefinition;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class GameIntegrityCheck {

    public GameIntegrityReport evaluate(String resourcePath) throws Exception {
        return evaluate(resourcePath, GameIntegrityConfig.defaults());
    }

    public GameIntegrityReport evaluate(String resourcePath, GameIntegrityConfig config) throws Exception {
        Objects.requireNonNull(resourcePath, "resourcePath");
        GameIntegrityConfig safeConfig = config == null ? GameIntegrityConfig.defaults() : config;

        KeyExpressionEvaluator.setDebugOutput(false);
        GameContext game = loadGame(resourcePath);
        LoopRuntime loopRuntime = new LoopRuntime(game.save(), game.loopConfig());
        WorldBuildResult world = loopRuntime.buildWorld();
        KernelRegistry registry = world.registry();

        List<GameIntegrityIssue> issues = new ArrayList<>();
        LabelIndex labelIndex = LabelIndex.fromRegistry(registry, game.craftingRecipes());

        validateOwnerRefs(game.save(), registry, issues);
        validateTriggers(game.triggers(), registry, issues);
        validateHiddenReveal(registry, game.triggers(), issues);
        validateCrafting(game.craftingRecipes(), labelIndex, issues);
        GameIntegrityKeyExpressions.validateKeyExpressions(game, registry, labelIndex, issues);

        List<Set<String>> winRequirements = GameIntegrityKeyExpressions.collectWinRequirements(
                game.triggers(),
                labelIndex,
                issues
        );
        GameIntegritySimulation.ReachabilityResult possible = GameIntegritySimulation.runReachability(
                game,
                safeConfig,
                GameIntegritySimulation.DiceMode.MAX,
                winRequirements
        );
        GameIntegritySimulation.ReachabilityResult guaranteed = GameIntegritySimulation.runReachability(
                game,
                safeConfig,
                GameIntegritySimulation.DiceMode.MIN,
                winRequirements
        );

        IntegrityWinRequirementEvaluator.evaluate(winRequirements, possible, issues);

        if (!possible.summary().winFound()) {
            GameIntegritySeverity severity = possible.summary().searchExhausted()
                    ? GameIntegritySeverity.ERROR
                    : GameIntegritySeverity.WARNING;
            issues.add(new GameIntegrityIssue(
                    severity,
                    "E_WIN_UNREACHABLE",
                    "No reachable win state found (dice=max).",
                    resourcePath
            ));
        }
        if (!guaranteed.summary().winFound()) {
            issues.add(new GameIntegrityIssue(
                    GameIntegritySeverity.WARNING,
                    "W_WIN_NOT_GUARANTEED",
                    "Win state not guaranteed under dice=min.",
                    resourcePath
            ));
        }

        return new GameIntegrityReport(resourcePath, possible.summary(), guaranteed.summary(), issues);
    }

    private GameContext loadGame(String resourcePath) throws Exception {
        GameSave save = RuntimeLoader.loadSave(resourcePath);
        LoopConfig loopConfig = RuntimeLoader.loadLoopConfig(resourcePath);
        List<TriggerDefinition> triggers = RuntimeLoader.loadTriggerDefinitions(resourcePath);
        Map<String, CraftingRecipe> recipes = RuntimeLoader.loadCraftingRecipes(resourcePath);
        Map<String, TokenType> aliases = RuntimeLoader.loadVerbAliases(resourcePath);
        List<UseSpec> useSpecs = buildUseSpecs(triggers);
        return new GameContext(resourcePath, save, loopConfig, triggers, recipes, aliases, useSpecs);
    }

    private void validateOwnerRefs(GameSave save, KernelRegistry registry, List<GameIntegrityIssue> issues) {
        if (save == null || registry == null || issues == null) {
            return;
        }
        Map<UUID, Thing> all = registry.getEverything();
        for (WorldRecipe.FixtureSpec fixture : save.fixtures()) {
            UUID ownerId = fixture.ownerId();
            Thing owner = all.get(ownerId);
            if (owner == null) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_FIXTURE_OWNER_MISSING",
                        "Fixture owner not found for " + fixture.name() + ".",
                        fixture.name()
                ));
            } else if (!(owner instanceof Plot)) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_FIXTURE_OWNER_NOT_PLOT",
                        "Fixture owner must be a plot: " + fixture.name() + ".",
                        fixture.name()
                ));
            }
        }
        for (GameSave.ItemRecipe item : save.items()) {
            UUID ownerId = item.ownerId();
            Thing owner = all.get(ownerId);
            if (owner == null) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_ITEM_OWNER_MISSING",
                        "Item owner not found for " + item.name() + ".",
                        item.name()
                ));
            }
            if (item.fixture() && !(owner instanceof Plot)) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_ITEM_FIXTURE_OWNER_NOT_PLOT",
                        "Fixture item owner must be a plot: " + item.name() + ".",
                        item.name()
                ));
            }
        }
        for (GameSave.ActorRecipe actor : save.actors()) {
            UUID ownerId = actor.ownerId();
            Thing owner = all.get(ownerId);
            if (owner == null) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_ACTOR_OWNER_MISSING",
                        "Actor owner not found for " + actor.name() + ".",
                        actor.name()
                ));
            } else if (!(owner instanceof Plot)) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_ACTOR_OWNER_NOT_PLOT",
                        "Actor owner must be a plot: " + actor.name() + ".",
                        actor.name()
                ));
            }
        }
    }

    private void validateTriggers(List<TriggerDefinition> triggers, KernelRegistry registry, List<GameIntegrityIssue> issues) {
        if (triggers == null || registry == null || issues == null) {
            return;
        }
        LabelIndex labels = LabelIndex.fromRegistry(registry, Map.of());
        for (TriggerDefinition trigger : triggers) {
            if (trigger == null) {
                continue;
            }
            String target = IntegrityLabels.normalizeLabel(trigger.target());
            if (!target.isBlank() && !labels.labels().contains(target)) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_TRIGGER_TARGET_MISSING",
                        "Trigger target not found: " + trigger.target() + ".",
                        trigger.id()
                ));
            }
            String object = IntegrityLabels.normalizeLabel(trigger.object());
            if (!object.isBlank() && !labels.labels().contains(object)) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_TRIGGER_OBJECT_MISSING",
                        "Trigger object not found: " + trigger.object() + ".",
                        trigger.id()
                ));
            }
            for (TriggerAction action : trigger.actions()) {
                if (action == null) {
                    continue;
                }
                if (action.target() != null && !action.target().isBlank() && !IntegrityLabels.isSpecialTarget(action.target())) {
                    String actionTarget = IntegrityLabels.normalizeLabel(action.target());
                    if (!actionTarget.isBlank() && !labels.labels().contains(actionTarget)) {
                        issues.add(new GameIntegrityIssue(
                                GameIntegritySeverity.ERROR,
                                "E_TRIGGER_ACTION_TARGET_MISSING",
                                "Trigger action target not found: " + action.target() + ".",
                                trigger.id()
                        ));
                    }
                }
                if (action.owner() != null && !action.owner().isBlank() && !IntegrityLabels.isSpecialTarget(action.owner())) {
                    String actionOwner = IntegrityLabels.normalizeLabel(action.owner());
                    if (!actionOwner.isBlank() && !labels.labels().contains(actionOwner)) {
                        issues.add(new GameIntegrityIssue(
                                GameIntegritySeverity.ERROR,
                                "E_TRIGGER_ACTION_OWNER_MISSING",
                                "Trigger action owner not found: " + action.owner() + ".",
                                trigger.id()
                        ));
                    }
                }
            }
            boolean hasStateChange = trigger.actions().stream()
                    .filter(Objects::nonNull)
                    .map(TriggerAction::type)
                    .anyMatch(type -> type == TriggerActionType.REVEAL
                            || type == TriggerActionType.HIDE
                            || type == TriggerActionType.SET_VISIBLE
                            || type == TriggerActionType.SET_KEY
                            || type == TriggerActionType.SET_VISIBILITY_KEY
                            || type == TriggerActionType.MOVE_OWNER
                            || type == TriggerActionType.SET_CELL
                            || type == TriggerActionType.INCREMENT_CELL
                            || type == TriggerActionType.SET_DESCRIPTION
                            || type == TriggerActionType.RESET_LOOP
                            || type == TriggerActionType.END_GAME);
            if (!hasStateChange) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.WARNING,
                        "W_TRIGGER_NO_STATE_CHANGE",
                        "Trigger does not change state (message-only).",
                        trigger.id()
                ));
            }
        }
    }

    private void validateHiddenReveal(KernelRegistry registry, List<TriggerDefinition> triggers, List<GameIntegrityIssue> issues) {
        if (registry == null || issues == null) {
            return;
        }
        Set<String> revealed = new HashSet<>();
        if (triggers != null) {
            for (TriggerDefinition trigger : triggers) {
                if (trigger == null) {
                    continue;
                }
                for (TriggerAction action : trigger.actions()) {
                    if (action == null || action.type() == null) {
                        continue;
                    }
                    if (action.type() != TriggerActionType.REVEAL && action.type() != TriggerActionType.SET_VISIBLE) {
                        continue;
                    }
                    if (action.type() == TriggerActionType.SET_VISIBLE && Boolean.FALSE.equals(action.visible())) {
                        continue;
                    }
                    String label = IntegrityLabels.resolveRevealTarget(action.target(), trigger.target(), trigger.object());
                    if (!label.isBlank()) {
                        revealed.add(label);
                    }
                }
            }
        }
        for (Thing thing : registry.getEverything().values()) {
            if (!(thing instanceof Item item)) {
                continue;
            }
            if (item.isVisibleFlag()) {
                continue;
            }
            String label = IntegrityLabels.normalizeLabel(item.getLabel());
            if (label.isBlank()) {
                continue;
            }
            if (!item.isFixture() && IntegrityLabels.isOwnedByPlot(registry, item)) {
                continue;
            }
            if (revealed.contains(label)) {
                continue;
            }
            issues.add(new GameIntegrityIssue(
                    GameIntegritySeverity.ERROR,
                    "E_HIDDEN_NO_REVEAL",
                    "Hidden item lacks reveal path: " + item.getLabel() + ".",
                    item.getLabel()
            ));
        }
    }

    private void validateCrafting(Map<String, CraftingRecipe> recipes, LabelIndex labels, List<GameIntegrityIssue> issues) {
        if (recipes == null || recipes.isEmpty() || labels == null || issues == null) {
            return;
        }
        for (CraftingRecipe recipe : recipes.values()) {
            if (recipe == null) {
                continue;
            }
            for (String req : recipe.requirements()) {
                String normalized = IntegrityLabels.normalizeLabel(req);
                if (!normalized.isBlank() && !labels.labels().contains(normalized)) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.WARNING,
                            "E_CRAFTING_REQUIREMENT_MISSING",
                            "Crafting requirement not found: " + req + ".",
                            recipe.name()
                    ));
                }
            }
            for (String consume : recipe.consume()) {
                String normalized = IntegrityLabels.normalizeLabel(consume);
                if (!normalized.isBlank() && !labels.labels().contains(normalized)) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.WARNING,
                            "E_CRAFTING_CONSUME_MISSING",
                            "Crafting consume item not found: " + consume + ".",
                            recipe.name()
                    ));
                }
            }
            if (!recipe.skillTag().isBlank()) {
                String skill = IntegrityLabels.normalizeLabel(recipe.skillTag());
                if (!skill.isBlank() && !labels.skills().contains(skill)) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.ERROR,
                            "E_CRAFTING_SKILL_MISSING",
                            "Crafting skill not found: " + recipe.skillTag() + ".",
                            recipe.name()
                    ));
                }
            }
        }
    }

    private List<UseSpec> buildUseSpecs(List<TriggerDefinition> triggers) {
        if (triggers == null || triggers.isEmpty()) {
            return List.of();
        }
        List<UseSpec> specs = new ArrayList<>();
        for (TriggerDefinition trigger : triggers) {
            if (trigger == null || trigger.type() != com.demo.adventure.engine.flow.trigger.TriggerType.ON_USE) {
                continue;
            }
            if (trigger.target() == null || trigger.target().isBlank()) {
                continue;
            }
            specs.add(new UseSpec(trigger.target(), trigger.object()));
        }
        return specs;
    }

    
}

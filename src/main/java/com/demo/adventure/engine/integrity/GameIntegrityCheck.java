package com.demo.adventure.engine.integrity;

import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.engine.cli.RuntimeLoader;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandOutput;
import com.demo.adventure.engine.command.TokenType;
import com.demo.adventure.engine.command.handlers.CommandHandlers;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.engine.flow.loop.LoopConfig;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerAction;
import com.demo.adventure.engine.flow.trigger.TriggerActionType;
import com.demo.adventure.engine.flow.trigger.TriggerDefinition;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionCompiler;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionResult;
import com.demo.adventure.engine.mechanics.keyexpr.UnknownReferenceException;
import com.demo.adventure.engine.mechanics.keyexpr.ast.BinaryNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.FunctionCallNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.KeyExpressionNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.StringLiteralNode;
import com.demo.adventure.engine.mechanics.keyexpr.ast.UnaryNode;
import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Item;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.domain.model.Thing;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.domain.save.WorldRecipe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class GameIntegrityCheck {

    private enum DiceMode {
        MIN,
        MAX
    }

    private record GameContext(
            String resourcePath,
            GameSave save,
            LoopConfig loopConfig,
            List<TriggerDefinition> triggers,
            Map<String, CraftingRecipe> craftingRecipes,
            Map<String, TokenType> aliases,
            List<UseSpec> useSpecs
    ) {
    }

    private record ReachabilityResult(
            GameIntegrityReachability summary,
            Set<String> reachableItems,
            boolean[] requiredSatisfied
    ) {
    }

    private record SimulationResult(GameRuntime runtime, boolean endGame, boolean invalid) {
    }

    private record UseSpec(String target, String object) {
    }

    private record LabelIndex(Set<String> labels, Set<String> skills) {
        static LabelIndex fromRegistry(KernelRegistry registry, Map<String, CraftingRecipe> recipes) {
            Set<String> labels = new HashSet<>();
            Set<String> skills = new HashSet<>();
            if (registry != null) {
                for (Thing thing : registry.getEverything().values()) {
                    if (thing == null) {
                        continue;
                    }
                    String label = normalizeLabel(thing.getLabel());
                    if (!label.isBlank()) {
                        labels.add(label);
                        if (thing instanceof Gate) {
                            String reversed = reverseGateLabel(thing.getLabel());
                            if (!reversed.isBlank()) {
                                labels.add(normalizeLabel(reversed));
                            }
                        }
                    }
                    if (thing instanceof Actor actor) {
                        for (String skill : actor.getSkills()) {
                            String normalized = normalizeLabel(skill);
                            if (!normalized.isBlank()) {
                                skills.add(normalized);
                            }
                        }
                    }
                }
            }
            if (recipes != null && !recipes.isEmpty()) {
                for (CraftingRecipe recipe : recipes.values()) {
                    if (recipe == null) {
                        continue;
                    }
                    String emit = normalizeLabel(recipe.emitLabel());
                    if (!emit.isBlank()) {
                        labels.add(emit);
                    }
                }
            }
            return new LabelIndex(labels, skills);
        }
    }

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
        validateKeyExpressions(game, registry, labelIndex, issues);

        List<Set<String>> winRequirements = collectWinRequirements(game.triggers(), labelIndex, issues);
        ReachabilityResult possible = runReachability(game, safeConfig, DiceMode.MAX, winRequirements);
        ReachabilityResult guaranteed = runReachability(game, safeConfig, DiceMode.MIN, winRequirements);

        evaluateWinRequirements(winRequirements, possible, issues);

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
            String target = normalizeLabel(trigger.target());
            if (!target.isBlank() && !labels.labels().contains(target)) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_TRIGGER_TARGET_MISSING",
                        "Trigger target not found: " + trigger.target() + ".",
                        trigger.id()
                ));
            }
            String object = normalizeLabel(trigger.object());
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
                if (action.target() != null && !action.target().isBlank() && !isSpecialTarget(action.target())) {
                    String actionTarget = normalizeLabel(action.target());
                    if (!actionTarget.isBlank() && !labels.labels().contains(actionTarget)) {
                        issues.add(new GameIntegrityIssue(
                                GameIntegritySeverity.ERROR,
                                "E_TRIGGER_ACTION_TARGET_MISSING",
                                "Trigger action target not found: " + action.target() + ".",
                                trigger.id()
                        ));
                    }
                }
                if (action.owner() != null && !action.owner().isBlank() && !isSpecialTarget(action.owner())) {
                    String actionOwner = normalizeLabel(action.owner());
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
                    String label = resolveRevealTarget(action.target(), trigger.target(), trigger.object());
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
            String label = normalizeLabel(item.getLabel());
            if (label.isBlank()) {
                continue;
            }
            if (!item.isFixture() && isOwnedByPlot(registry, item)) {
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
                String normalized = normalizeLabel(req);
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
                String normalized = normalizeLabel(consume);
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
                String skill = normalizeLabel(recipe.skillTag());
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

    private void validateKeyExpressions(GameContext game, KernelRegistry registry, LabelIndex labels, List<GameIntegrityIssue> issues) {
        if (game == null || registry == null || labels == null || issues == null) {
            return;
        }
        List<ExpressionSpec> expressions = new ArrayList<>();
        for (WorldRecipe.GateSpec gate : game.save().gates()) {
            if (gate.keyString() != null && !gate.keyString().isBlank()) {
                expressions.add(new ExpressionSpec(gate.keyString(), "gate:" + gate.label()));
            }
        }
        for (GameSave.ItemRecipe item : game.save().items()) {
            if (item.keyString() != null && !item.keyString().isBlank()) {
                expressions.add(new ExpressionSpec(item.keyString(), "item:" + item.name()));
            }
        }
        for (TriggerDefinition trigger : game.triggers()) {
            if (trigger.key() != null && !trigger.key().isBlank()) {
                expressions.add(new ExpressionSpec(trigger.key(), "trigger:" + trigger.id()));
            }
            for (TriggerAction action : trigger.actions()) {
                if (action == null) {
                    continue;
                }
                if (action.key() != null && !action.key().isBlank()) {
                    expressions.add(new ExpressionSpec(action.key(), "trigger-action:" + trigger.id()));
                }
                if (action.visibilityKey() != null && !action.visibilityKey().isBlank()) {
                    expressions.add(new ExpressionSpec(action.visibilityKey(), "trigger-visibility:" + trigger.id()));
                }
            }
        }
        for (CraftingRecipe recipe : game.craftingRecipes().values()) {
            if (recipe == null) {
                continue;
            }
            if (recipe.expression() != null && !recipe.expression().isBlank()) {
                expressions.add(new ExpressionSpec(recipe.expression(), "craft:" + recipe.name()));
            }
        }

        UUID[] scopeIds = registry.getEverything().keySet().toArray(new UUID[0]);
        KeyExpressionEvaluator.AttributeResolver attributeResolver =
                KeyExpressionEvaluator.registryAttributeResolver(registry, scopeIds);

        for (ExpressionSpec spec : expressions) {
            if (spec == null || spec.expression().isBlank()) {
                continue;
            }
            KeyExpressionNode ast;
            try {
                ast = new KeyExpressionCompiler().compile(spec.expression());
            } catch (Exception ex) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_KEYEXPR_PARSE",
                        "Key expression parse failed: " + ex.getMessage(),
                        spec.context()
                ));
                continue;
            }
            validateFunctionReferences(ast, labels, issues, spec.context());
            try {
                KeyExpressionResult result = KeyExpressionEvaluator.evaluateResult(
                        spec.expression(),
                        label -> false,
                        label -> false,
                        tag -> false,
                        attributeResolver,
                        KeyExpressionEvaluator.AttributeResolutionPolicy.QUERY_STRICT
                );
                if (!result.isSuccess()) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.ERROR,
                            "E_KEYEXPR_INVALID",
                            "Key expression invalid: " + result.error().message(),
                            spec.context()
                    ));
                }
            } catch (UnknownReferenceException ex) {
                issues.add(new GameIntegrityIssue(
                        GameIntegritySeverity.ERROR,
                        "E_KEYEXPR_REF",
                        "Key expression reference error: " + ex.getError().message(),
                        spec.context()
                ));
            }
        }
    }

    private List<Set<String>> collectWinRequirements(
            List<TriggerDefinition> triggers,
            LabelIndex labels,
            List<GameIntegrityIssue> issues
    ) {
        List<Set<String>> requirements = new ArrayList<>();
        if (triggers == null) {
            return requirements;
        }
        for (TriggerDefinition trigger : triggers) {
            if (trigger == null) {
                continue;
            }
            boolean hasWin = trigger.actions().stream()
                    .filter(Objects::nonNull)
                    .anyMatch(action -> action.type() == TriggerActionType.END_GAME);
            if (!hasWin) {
                continue;
            }
            Set<String> required = new HashSet<>();
            if (trigger.key() != null && !trigger.key().isBlank()) {
                try {
                    KeyExpressionNode ast = new KeyExpressionCompiler().compile(trigger.key());
                    collectHasReferences(ast, required);
                } catch (Exception ex) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.ERROR,
                            "E_WIN_KEYEXPR_PARSE",
                            "Win key expression parse failed: " + ex.getMessage(),
                            trigger.id()
                    ));
                }
            }
            requirements.add(required);
        }
        return requirements;
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

    private ReachabilityResult runReachability(
            GameContext game,
            GameIntegrityConfig config,
            DiceMode diceMode,
            List<Set<String>> winRequirements
    ) throws Exception {
        if (game == null) {
            return new ReachabilityResult(new GameIntegrityReachability(false, true, 0, 0, 0), Set.of(), new boolean[0]);
        }
        GameIntegrityConfig safeConfig = config == null ? GameIntegrityConfig.defaults() : config;
        boolean[] requiredSatisfied = new boolean[winRequirements == null ? 0 : winRequirements.size()];
        Set<String> reachableItems = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Deque<PathNode> queue = new ArrayDeque<>();
        queue.add(new PathNode(List.of(), 0));

        int actionsEvaluated = 0;
        int maxDepthReached = 0;
        boolean searchExhausted = true;
        boolean winFound = false;

        KeyExpressionEvaluator.DiceRoller prevDice = KeyExpressionEvaluator.getDefaultDiceRoller();
        KeyExpressionEvaluator.setDefaultDiceRoller(sides -> diceMode == DiceMode.MAX ? sides : 1);
        try {
            while (!queue.isEmpty()) {
                PathNode node = queue.poll();
                if (node.depth() > safeConfig.maxDepth()) {
                    searchExhausted = false;
                    continue;
                }
                SimulationResult sim = simulate(game, node.commands());
                if (sim.invalid()) {
                    continue;
                }
                maxDepthReached = Math.max(maxDepthReached, node.depth());
                String signature = stateSignature(sim.runtime());
                if (!visited.add(signature)) {
                    continue;
                }
                if (visited.size() >= safeConfig.maxStates()) {
                    searchExhausted = false;
                    break;
                }
                Set<String> inventory = inventoryLabels(sim.runtime());
                reachableItems.addAll(inventory);
                if (winRequirements != null && !winRequirements.isEmpty()) {
                    for (int i = 0; i < winRequirements.size(); i++) {
                        if (requiredSatisfied[i]) {
                            continue;
                        }
                        Set<String> required = winRequirements.get(i);
                        if (required == null || required.isEmpty()) {
                            requiredSatisfied[i] = true;
                            continue;
                        }
                        if (inventory.containsAll(required)) {
                            requiredSatisfied[i] = true;
                        }
                    }
                }
                if (sim.endGame()) {
                    winFound = true;
                    break;
                }
                List<String> actions = generateActions(sim.runtime(), game.craftingRecipes(), game.useSpecs());
                actionsEvaluated += actions.size();
                if (actions.size() > safeConfig.maxActionsPerState()) {
                    actions = actions.subList(0, safeConfig.maxActionsPerState());
                    searchExhausted = false;
                }
                for (String action : actions) {
                    List<String> next = new ArrayList<>(node.commands());
                    next.add(action);
                    queue.add(new PathNode(next, node.depth() + 1));
                }
            }
        } finally {
            KeyExpressionEvaluator.setDefaultDiceRoller(prevDice);
        }

        GameIntegrityReachability summary = new GameIntegrityReachability(
                winFound,
                searchExhausted,
                visited.size(),
                actionsEvaluated,
                maxDepthReached
        );
        return new ReachabilityResult(summary, reachableItems, requiredSatisfied);
    }

    private SimulationResult simulate(GameContext game, List<String> commands) throws Exception {
        GameRuntime runtime = buildRuntime(game);
        CommandInterpreter interpreter = new CommandInterpreter();
        interpreter.setExtraKeywords(game.aliases());
        Map<CommandAction, GameCommandHandler> handlers = CommandHandlers.defaultHandlers();
        CommandContext context = new CommandContext(NullOutput.INSTANCE, runtime);

        for (String commandText : commands) {
            if (commandText == null || commandText.isBlank()) {
                continue;
            }
            Command cmd = interpreter.interpret(commandText);
            if (cmd == null || cmd.hasError() || cmd.action() == CommandAction.UNKNOWN) {
                return new SimulationResult(runtime, false, true);
            }
            GameCommandHandler handler = handlers.get(cmd.action());
            if (handler == null) {
                return new SimulationResult(runtime, false, true);
            }
            CommandOutcome outcome = handler.handle(context, cmd);
            if (outcome.endGame()) {
                return new SimulationResult(runtime, true, false);
            }
            if (!outcome.skipTurnAdvance()) {
                CommandOutcome turnOutcome = runtime.advanceTurn();
                if (turnOutcome.endGame()) {
                    return new SimulationResult(runtime, true, false);
                }
            }
        }
        return new SimulationResult(runtime, false, false);
    }

    private GameRuntime buildRuntime(GameContext game) throws Exception {
        LoopRuntime loopRuntime = new LoopRuntime(game.save(), game.loopConfig());
        WorldBuildResult world = loopRuntime.buildWorld();
        KernelRegistry registry = world.registry();
        UUID currentPlot = world.startPlotId();

        GameRuntime runtime = new GameRuntime(null, text -> { }, false);
        runtime.setOutputSuppressed(true);
        UUID playerId = runtime.findPlayerActor(registry, currentPlot);
        List<Item> inventory = new ArrayList<>(runtime.startingInventory(registry, playerId));
        Map<UUID, Map<UUID, com.demo.adventure.domain.model.Rectangle2D>> placements = new HashMap<>();
        runtime.seedInventoryPlacements(inventory, placements);

        TriggerEngine triggerEngine = new TriggerEngine(game.triggers());
        runtime.configure(
                registry,
                currentPlot,
                playerId,
                inventory,
                placements,
                loopRuntime,
                triggerEngine,
                game.craftingRecipes(),
                game.aliases()
        );
        return runtime;
    }

    private List<String> generateActions(
            GameRuntime runtime,
            Map<String, CraftingRecipe> recipes,
            List<UseSpec> useSpecs
    ) {
        if (runtime == null) {
            return List.of();
        }
        Set<String> actions = new LinkedHashSet<>();
        actions.add("search");

        List<Direction> exits = visibleExits(runtime);
        for (Direction direction : exits) {
            actions.add("go " + direction.toLongName().toLowerCase(Locale.ROOT));
        }

        List<String> visibleItems = runtime.visibleItemLabels();
        for (String item : visibleItems) {
            actions.add("take " + item);
        }

        List<String> inventory = runtime.inventoryLabels();
        for (String item : inventory) {
            actions.add("drop " + item);
        }

        List<String> fixtures = runtime.visibleFixtureLabels();
        for (String fixture : fixtures) {
            actions.add("open " + fixture);
        }
        for (String item : inventory) {
            actions.add("open " + item);
        }

        List<String> sources = unionLabels(inventory, fixtures, visibleItems);
        List<String> objects = unionLabels(fixtures, visibleItems, inventory);
        if (useSpecs != null && !useSpecs.isEmpty()) {
            for (UseSpec spec : useSpecs) {
                if (spec == null || spec.target() == null || spec.target().isBlank()) {
                    continue;
                }
                String target = matchLabel(spec.target(), sources);
                if (target == null) {
                    continue;
                }
                if (spec.object() == null || spec.object().isBlank()) {
                    actions.add("use " + target);
                } else {
                    String object = matchLabel(spec.object(), objects);
                    if (object != null) {
                        actions.add("use " + target + " on " + object);
                    }
                }
            }
        }
        for (String source : sources) {
            if (thingHasCells(runtime, source)) {
                actions.add("use " + source);
            }
        }

        if (recipes != null && !recipes.isEmpty()) {
            for (CraftingRecipe recipe : recipes.values()) {
                if (recipe == null || recipe.name().isBlank()) {
                    continue;
                }
                actions.add("make " + recipe.name());
            }
        }

        List<String> sorted = new ArrayList<>(actions);
        sorted.sort(String.CASE_INSENSITIVE_ORDER);
        return sorted;
    }

    private List<Direction> visibleExits(GameRuntime runtime) {
        KernelRegistry registry = runtime.registry();
        UUID plotId = runtime.currentPlotId();
        if (registry == null || plotId == null) {
            return List.of();
        }
        return registry.getEverything().values().stream()
                .filter(Gate.class::isInstance)
                .map(Gate.class::cast)
                .filter(Gate::isVisible)
                .filter(gate -> gate.connects(plotId))
                .map(gate -> gate.directionFrom(plotId))
                .filter(Objects::nonNull)
                .distinct()
                .sorted(Comparator.comparing(Direction::toLongName))
                .toList();
    }

    private static List<String> unionLabels(List<String>... lists) {
        Set<String> labels = new LinkedHashSet<>();
        if (lists != null) {
            for (List<String> list : lists) {
                if (list == null) {
                    continue;
                }
                for (String label : list) {
                    if (label != null && !label.isBlank()) {
                        labels.add(label);
                    }
                }
            }
        }
        return new ArrayList<>(labels);
    }

    private static String matchLabel(String target, List<String> candidates) {
        if (target == null || target.isBlank() || candidates == null) {
            return null;
        }
        String normalized = normalizeLabel(target);
        for (String candidate : candidates) {
            if (normalized.equals(normalizeLabel(candidate))) {
                return candidate;
            }
        }
        return null;
    }

    private static boolean thingHasCells(GameRuntime runtime, String label) {
        if (runtime == null || runtime.registry() == null || label == null || label.isBlank()) {
            return false;
        }
        Thing thing = findThingByLabel(runtime.registry(), label);
        return thing != null && thing.getCells() != null && !thing.getCells().isEmpty();
    }

    private static Thing findThingByLabel(KernelRegistry registry, String label) {
        if (registry == null || label == null || label.isBlank()) {
            return null;
        }
        String trimmed = label.trim();
        return registry.getEverything().values().stream()
                .filter(Objects::nonNull)
                .filter(thing -> thing.getLabel() != null)
                .filter(thing -> thing.getLabel().equalsIgnoreCase(trimmed))
                .findFirst()
                .orElse(null);
    }

    private void validateFunctionReferences(KeyExpressionNode node, LabelIndex labels, List<GameIntegrityIssue> issues, String context) {
        if (node == null || labels == null || issues == null) {
            return;
        }
        for (FunctionCallNode call : collectFunctions(node)) {
            if (call == null || call.name() == null) {
                continue;
            }
            String name = call.name().trim().toUpperCase(Locale.ROOT);
            if ("HAS".equals(name) || "SEARCH".equals(name)) {
                String label = firstStringLiteral(call);
                if (label == null) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.WARNING,
                            "W_KEYEXPR_DYNAMIC_LABEL",
                            "Key expression uses non-literal " + name + " label.",
                            context
                    ));
                    continue;
                }
                String normalized = normalizeLabel(label);
                if (!normalized.isBlank() && !labels.labels().contains(normalized)) {
                    GameIntegritySeverity severity = context != null && context.startsWith("craft:")
                            ? GameIntegritySeverity.WARNING
                            : GameIntegritySeverity.ERROR;
                    issues.add(new GameIntegrityIssue(
                            severity,
                            "E_KEYEXPR_LABEL_MISSING",
                            "Key expression references missing " + name + " label: " + label + ".",
                            context
                    ));
                }
            }
            if ("SKILL".equals(name)) {
                String label = firstStringLiteral(call);
                if (label == null) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.WARNING,
                            "W_KEYEXPR_DYNAMIC_SKILL",
                            "Key expression uses non-literal SKILL tag.",
                            context
                    ));
                    continue;
                }
                String normalized = normalizeLabel(label);
                if (!normalized.isBlank() && !labels.skills().contains(normalized)) {
                    issues.add(new GameIntegrityIssue(
                            GameIntegritySeverity.ERROR,
                            "E_KEYEXPR_SKILL_MISSING",
                            "Key expression references missing SKILL tag: " + label + ".",
                            context
                    ));
                }
            }
        }
    }

    private static List<FunctionCallNode> collectFunctions(KeyExpressionNode node) {
        if (node == null) {
            return List.of();
        }
        List<FunctionCallNode> functions = new ArrayList<>();
        Deque<KeyExpressionNode> stack = new ArrayDeque<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            KeyExpressionNode current = stack.pop();
            if (current instanceof FunctionCallNode call) {
                functions.add(call);
                for (KeyExpressionNode arg : call.arguments()) {
                    stack.push(arg);
                }
                continue;
            }
            if (current instanceof BinaryNode binary) {
                stack.push(binary.left());
                stack.push(binary.right());
                continue;
            }
            if (current instanceof UnaryNode unary) {
                stack.push(unary.operand());
                continue;
            }
        }
        return functions;
    }

    private static String firstStringLiteral(FunctionCallNode call) {
        if (call == null || call.arguments().isEmpty()) {
            return null;
        }
        KeyExpressionNode arg = call.arguments().get(0);
        if (arg instanceof StringLiteralNode str) {
            return str.value();
        }
        return null;
    }

    private static void collectHasReferences(KeyExpressionNode node, Set<String> collector) {
        if (node == null || collector == null) {
            return;
        }
        Deque<KeyExpressionNode> stack = new ArrayDeque<>();
        stack.push(node);
        while (!stack.isEmpty()) {
            KeyExpressionNode current = stack.pop();
            if (current instanceof FunctionCallNode call) {
                String name = call.name() == null ? "" : call.name().trim().toUpperCase(Locale.ROOT);
                if ("HAS".equals(name)) {
                    String label = firstStringLiteral(call);
                    if (label != null) {
                        collector.add(normalizeLabel(label));
                    }
                }
                for (KeyExpressionNode arg : call.arguments()) {
                    stack.push(arg);
                }
                continue;
            }
            if (current instanceof BinaryNode binary) {
                stack.push(binary.left());
                stack.push(binary.right());
            } else if (current instanceof UnaryNode unary) {
                stack.push(unary.operand());
            }
        }
    }

    private void evaluateWinRequirements(
            List<Set<String>> winRequirements,
            ReachabilityResult possible,
            List<GameIntegrityIssue> issues
    ) {
        if (winRequirements == null || winRequirements.isEmpty() || possible == null || issues == null) {
            return;
        }
        boolean searchExhausted = possible.summary().searchExhausted();
        for (Set<String> required : winRequirements) {
            if (required == null || required.isEmpty()) {
                continue;
            }
            for (String label : required) {
                if (!possible.reachableItems().contains(label)) {
                    issues.add(new GameIntegrityIssue(
                            searchExhausted ? GameIntegritySeverity.ERROR : GameIntegritySeverity.WARNING,
                            "E_REQUIRED_ITEM_UNREACHABLE",
                            "Required item not reachable: " + label + ".",
                            label
                    ));
                }
            }
        }
        boolean[] satisfied = possible.requiredSatisfied();
        for (int i = 0; i < winRequirements.size(); i++) {
            Set<String> required = winRequirements.get(i);
            if (required == null || required.isEmpty()) {
                continue;
            }
            boolean ok = i < satisfied.length && satisfied[i];
            if (!ok) {
                issues.add(new GameIntegrityIssue(
                        searchExhausted ? GameIntegritySeverity.ERROR : GameIntegritySeverity.WARNING,
                        "E_REQUIRED_SET_UNSATISFIED",
                        "Required items never held together: " + String.join(", ", required) + ".",
                        String.join(", ", required)
                ));
            }
        }
    }

    private static Set<String> inventoryLabels(GameRuntime runtime) {
        if (runtime == null) {
            return Set.of();
        }
        return runtime.inventoryLabels().stream()
                .map(GameIntegrityCheck::normalizeLabel)
                .filter(label -> !label.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String stateSignature(GameRuntime runtime) {
        if (runtime == null || runtime.registry() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("plot=").append(runtime.currentPlotId());
        Map<UUID, Thing> things = runtime.registry().getEverything();
        List<UUID> ids = new ArrayList<>(things.keySet());
        ids.sort(Comparator.comparing(UUID::toString));
        for (UUID id : ids) {
            Thing thing = things.get(id);
            if (thing == null) {
                continue;
            }
            sb.append("|").append(id);
            sb.append(":").append(thing.getKind());
            sb.append(":owner=").append(thing.getOwnerId());
            sb.append(":vis=").append(thing.isVisibleFlag());
            sb.append(":key=").append(thing.getKey());
            sb.append(":vkey=").append(thing.getVisibilityKey());
            if (thing instanceof Item item) {
                sb.append(":fixture=").append(item.isFixture());
            }
            if (thing.getCells() != null && !thing.getCells().isEmpty()) {
                List<String> cells = new ArrayList<>();
                for (Map.Entry<String, com.demo.adventure.engine.mechanics.cells.Cell> entry : thing.getCells().entrySet()) {
                    com.demo.adventure.engine.mechanics.cells.Cell cell = entry.getValue();
                    if (cell == null) {
                        continue;
                    }
                    cells.add(entry.getKey() + "=" + cell.getAmount() + "/" + cell.getCapacity());
                }
                Collections.sort(cells);
                for (String cell : cells) {
                    sb.append(":cell=").append(cell);
                }
            }
        }
        List<String> inv = new ArrayList<>(inventoryLabels(runtime));
        inv.sort(String.CASE_INSENSITIVE_ORDER);
        for (String item : inv) {
            sb.append(":inv=").append(item);
        }
        return sb.toString();
    }

    private static String normalizeLabel(String label) {
        if (label == null) {
            return "";
        }
        return label.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isSpecialTarget(String value) {
        if (value == null) {
            return false;
        }
        String key = value.trim().toUpperCase(Locale.ROOT);
        return key.equals("@PLAYER")
                || key.equals("@PLOT")
                || key.equals("@WORLD")
                || key.equals("@TARGET")
                || key.equals("@OBJECT");
    }

    private static String reverseGateLabel(String label) {
        if (label == null) {
            return "";
        }
        String[] parts = label.split("->", -1);
        if (parts.length != 2) {
            return "";
        }
        String left = parts[0].trim();
        String right = parts[1].trim();
        if (left.isBlank() || right.isBlank()) {
            return "";
        }
        return right + " -> " + left;
    }

    private static String resolveRevealTarget(String target, String triggerTarget, String triggerObject) {
        if (target == null || target.isBlank()) {
            return normalizeLabel(triggerTarget);
        }
        String key = target.trim().toUpperCase(Locale.ROOT);
        if (key.equals("@TARGET")) {
            return normalizeLabel(triggerTarget);
        }
        if (key.equals("@OBJECT")) {
            return normalizeLabel(triggerObject);
        }
        if (key.startsWith("@")) {
            return "";
        }
        return normalizeLabel(target);
    }

    private static boolean isOwnedByPlot(KernelRegistry registry, Item item) {
        if (registry == null || item == null || item.getOwnerId() == null) {
            return false;
        }
        Thing owner = registry.get(item.getOwnerId());
        return owner instanceof Plot;
    }

    private record ExpressionSpec(String expression, String context) {
    }

    private record PathNode(List<String> commands, int depth) {
    }

    private enum NullOutput implements CommandOutput {
        INSTANCE;

        @Override
        public void emit(String text) {
        }

        @Override
        public void printHelp() {
        }
    }
}

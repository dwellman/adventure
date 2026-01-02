package com.demo.adventure.engine.integrity;

import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandOutput;
import com.demo.adventure.engine.command.handlers.CommandHandlers;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.engine.flow.loop.LoopRuntime;
import com.demo.adventure.engine.flow.trigger.TriggerEngine;
import com.demo.adventure.engine.mechanics.crafting.CraftingRecipe;
import com.demo.adventure.engine.mechanics.keyexpr.KeyExpressionEvaluator;
import com.demo.adventure.engine.runtime.CommandContext;
import com.demo.adventure.engine.runtime.GameRuntime;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

final class GameIntegritySimulation {
    enum DiceMode {
        MIN,
        MAX
    }

    record ReachabilityResult(
            GameIntegrityReachability summary,
            Set<String> reachableItems,
            boolean[] requiredSatisfied
    ) {
    }

    static ReachabilityResult runReachability(
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
                String signature = IntegritySimulationState.stateSignature(sim.runtime());
                if (!visited.add(signature)) {
                    continue;
                }
                if (visited.size() >= safeConfig.maxStates()) {
                    searchExhausted = false;
                    break;
                }
                Set<String> inventory = IntegritySimulationState.inventoryLabels(sim.runtime());
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
                List<String> actions = IntegritySimulationActions.generateActions(
                        sim.runtime(),
                        game.craftingRecipes(),
                        game.useSpecs()
                );
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

    private static SimulationResult simulate(GameContext game, List<String> commands) throws Exception {
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

    private static GameRuntime buildRuntime(GameContext game) throws Exception {
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

    private record SimulationResult(GameRuntime runtime, boolean endGame, boolean invalid) {
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

package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.TranslatorService;
import com.demo.adventure.ai.runtime.smart.SmartActorContext;
import com.demo.adventure.ai.runtime.smart.SmartActorContextBuilder;
import com.demo.adventure.ai.runtime.smart.SmartActorContextInput;
import com.demo.adventure.ai.runtime.smart.SmartActorContextInputBuilder;
import com.demo.adventure.ai.runtime.smart.SmartActorDecision;
import com.demo.adventure.ai.runtime.smart.SmartActorDecisionParser;
import com.demo.adventure.ai.runtime.smart.SmartActorHistoryStore;
import com.demo.adventure.ai.runtime.smart.SmartActorPlanner;
import com.demo.adventure.ai.runtime.smart.SmartActorPrompt;
import com.demo.adventure.ai.runtime.smart.SmartActorPromptBuilder;
import com.demo.adventure.ai.runtime.smart.SmartActorRegistry;
import com.demo.adventure.ai.runtime.smart.SmartActorSpec;
import com.demo.adventure.ai.runtime.smart.SmartActorTagIndex;
import com.demo.adventure.ai.runtime.smart.SmartActorWorldSnapshot;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SmartActorRuntime {
    private static final Set<CommandAction> COMBAT_ACTIONS = EnumSet.of(CommandAction.ATTACK, CommandAction.FLEE);

    private final SmartActorRegistry registry;
    private final SmartActorHistoryStore historyStore;
    private final SmartActorContextInputBuilder inputBuilder;
    private final SmartActorContextBuilder contextBuilder;
    private final SmartActorPlanner planner;
    private final CommandInterpreter interpreter;
    private final Map<CommandAction, GameCommandHandler> handlers;
    private final SmartActorCommandResolver commandResolver;
    private final SmartActorSnapshotBuilder snapshotBuilder;
    private final SmartActorHistoryRecorder historyRecorder;
    private final Map<UUID, Integer> lastActionTurn = new HashMap<>();
    private final boolean debug;
    private boolean localOnly;
    private int turnIndex;

    public SmartActorRuntime(SmartActorRegistry registry,
                             SmartActorTagIndex tagIndex,
                             SmartActorPlanner planner,
                             TranslatorService translatorService,
                             CommandInterpreter interpreter,
                             Map<CommandAction, GameCommandHandler> handlers,
                             boolean debug) {
        if (registry == null) {
            throw new IllegalArgumentException("smart actor registry is required");
        }
        if (planner == null) {
            throw new IllegalArgumentException("smart actor planner is required");
        }
        if (translatorService == null) {
            throw new IllegalArgumentException("translatorService is required");
        }
        if (interpreter == null) {
            throw new IllegalArgumentException("interpreter is required");
        }
        if (handlers == null) {
            throw new IllegalArgumentException("handlers are required");
        }
        this.registry = registry;
        this.planner = planner;
        this.interpreter = interpreter;
        this.handlers = handlers;
        this.debug = debug;
        SmartActorTagIndex safeTags = tagIndex == null ? SmartActorTagIndex.empty() : tagIndex;
        this.historyStore = new SmartActorHistoryStore();
        registry.entries().values().forEach(historyStore::seedFromSpec);
        this.inputBuilder = new SmartActorContextInputBuilder(safeTags);
        this.contextBuilder = new SmartActorContextBuilder(historyStore);
        this.commandResolver = new SmartActorCommandResolver(translatorService, interpreter, handlers, debug);
        this.snapshotBuilder = new SmartActorSnapshotBuilder();
        this.historyRecorder = new SmartActorHistoryRecorder(historyStore);
    }

    public boolean handlesActor(UUID actorId) {
        return actorId != null && registry.specFor(actorId) != null;
    }

    public void setLocalOnly(boolean localOnly) {
        this.localOnly = localOnly;
    }

    public CommandOutcome advanceTurn(GameRuntime runtime) throws GameBuilderException {
        if (runtime == null || registry.isEmpty() || runtime.inCombat()) {
            return CommandOutcome.none();
        }
        turnIndex++;
        for (Map.Entry<UUID, SmartActorSpec> entry : registry.entries().entrySet()) {
            UUID actorId = entry.getKey();
            SmartActorSpec spec = entry.getValue();
            if (spec == null || !eligible(actorId, spec)) {
                continue;
            }
            Actor actor = runtime.registry().get(actorId) instanceof Actor found ? found : null;
            if (actor == null || !actor.isVisible() || actor.getOwnerId() == null) {
                continue;
            }
            if (localOnly && !samePlot(actor.getOwnerId(), runtime.currentPlotId())) {
                continue;
            }
            UUID plotId = actor.getOwnerId();
            SmartActorContextInput input = inputBuilder.build(runtime.registry(), actorId, plotId, Set.of());
            SmartActorContext context = contextBuilder.build(spec, input);
            SmartActorWorldSnapshot snapshot = buildSnapshot(runtime, actorId);
            if (snapshot == null) {
                continue;
            }
            SmartActorPrompt prompt = SmartActorPromptBuilder.build(spec, context, snapshot);
            SmartActorDecisionParser.Result decisionResult = planner.decide(prompt);
            CommandOutcome outcome = handleDecision(runtime, actorId, spec, context, snapshot, decisionResult);
            if (outcome.endGame() || outcome.skipTurnAdvance()) {
                return outcome;
            }
        }
        return CommandOutcome.none();
    }

    public CommandOutcome advanceCombatTurn(GameRuntime runtime, UUID actorId) throws GameBuilderException {
        if (runtime == null || actorId == null || registry.isEmpty()) {
            return CommandOutcome.none();
        }
        turnIndex++;
        SmartActorSpec spec = registry.specFor(actorId);
        if (spec == null) {
            CommandOutcome outcome = runtime.resolveSmartActorCombatAction(actorId, null);
            return outcome == null ? CommandOutcome.none() : outcome;
        }
        Actor actor = runtime.registry().get(actorId) instanceof Actor found ? found : null;
        if (actor == null || !actor.isVisible() || actor.getOwnerId() == null) {
            CommandOutcome outcome = runtime.resolveSmartActorCombatAction(actorId, null);
            return outcome == null ? CommandOutcome.none() : outcome;
        }
        UUID plotId = actor.getOwnerId();
        SmartActorContextInput input = inputBuilder.build(runtime.registry(), actorId, plotId, Set.of());
        SmartActorContext context = contextBuilder.build(spec, input);
        SmartActorWorldSnapshot snapshot = buildSnapshot(runtime, actorId);
        if (snapshot == null) {
            return combatSkip(runtime, actorId);
        }
        if (!eligible(actorId, spec)) {
            return combatSkip(runtime, actorId);
        }
        SmartActorPrompt prompt = SmartActorPromptBuilder.build(spec, context, snapshot);
        SmartActorDecisionParser.Result decisionResult = planner.decide(prompt);
        CommandOutcome outcome = handleCombatDecision(runtime, actorId, spec, context, snapshot, decisionResult);
        return outcome == null ? CommandOutcome.none() : outcome;
    }

    public SmartActorDecision respondToPlayer(GameRuntime runtime, UUID actorId, String playerUtterance) throws GameBuilderException {
        if (runtime == null || actorId == null || registry.isEmpty()) {
            return null;
        }
        SmartActorSpec spec = registry.specFor(actorId);
        if (spec == null) {
            return null;
        }
        Actor actor = runtime.registry().get(actorId) instanceof Actor found ? found : null;
        if (actor == null || !actor.isVisible() || actor.getOwnerId() == null) {
            return null;
        }
        UUID plotId = actor.getOwnerId();
        SmartActorContextInput input = inputBuilder.build(runtime.registry(), actorId, plotId, Set.of());
        SmartActorContext context = contextBuilder.build(spec, input);
        SmartActorWorldSnapshot snapshot = buildSnapshot(runtime, actorId, playerUtterance);
        if (snapshot == null) {
            return null;
        }
        SmartActorPrompt prompt = SmartActorPromptBuilder.build(spec, context, snapshot);
        SmartActorDecisionParser.Result decisionResult = planner.decide(prompt);
        if (decisionResult == null || decisionResult.type() != SmartActorDecisionParser.Result.Type.DECISION) {
            return null;
        }
        SmartActorDecision decision = decisionResult.decision();
        if (decision == null) {
            return null;
        }
        historyRecorder.recordConversationHistory(spec, context, playerUtterance, decision, turnIndex);
        markActedNextTurn(actorId);
        return decision;
    }

    private CommandOutcome handleDecision(GameRuntime runtime,
                                          UUID actorId,
                                          SmartActorSpec spec,
                                          SmartActorContext context,
                                          SmartActorWorldSnapshot snapshot,
                                          SmartActorDecisionParser.Result decisionResult) throws GameBuilderException {
        if (decisionResult == null || decisionResult.type() != SmartActorDecisionParser.Result.Type.DECISION) {
            return fallback(runtime, actorId, spec, context);
        }
        SmartActorDecision decision = decisionResult.decision();
        if (decision == null) {
            return CommandOutcome.none();
        }
        return switch (decision.type()) {
            case NONE -> CommandOutcome.none();
            case COLOR -> handleColor(runtime, actorId, spec, context, decision);
            case UTTERANCE -> handleUtterance(runtime, actorId, spec, context, snapshot, decision);
        };
    }

    private CommandOutcome handleCombatDecision(GameRuntime runtime,
                                                UUID actorId,
                                                SmartActorSpec spec,
                                                SmartActorContext context,
                                                SmartActorWorldSnapshot snapshot,
                                                SmartActorDecisionParser.Result decisionResult) throws GameBuilderException {
        if (decisionResult == null || decisionResult.type() != SmartActorDecisionParser.Result.Type.DECISION) {
            return combatPass(runtime, actorId, spec, context, "PASS");
        }
        SmartActorDecision decision = decisionResult.decision();
        if (decision == null) {
            return combatPass(runtime, actorId, spec, context, "PASS");
        }
        return switch (decision.type()) {
            case NONE -> combatPass(runtime, actorId, spec, context, "NONE");
            case COLOR -> handleCombatColor(runtime, actorId, spec, context, decision);
            case UTTERANCE -> handleCombatUtterance(runtime, actorId, spec, context, snapshot, decision);
        };
    }

    private CommandOutcome handleColor(GameRuntime runtime,
                                       UUID actorId,
                                       SmartActorSpec spec,
                                       SmartActorContext context,
                                       SmartActorDecision decision) {
        int maxLines = Math.max(0, spec.policy().maxColorLines());
        if (maxLines == 0) {
            return CommandOutcome.none();
        }
        String color = commandResolver.trimLines(decision.color(), maxLines);
        if (color.isBlank()) {
            return CommandOutcome.none();
        }
        runtime.narrateColor(color);
        historyRecorder.recordHistory(spec, context, "COLOR: " + color, turnIndex);
        markActed(actorId);
        return CommandOutcome.none();
    }

    private CommandOutcome handleUtterance(GameRuntime runtime,
                                           UUID actorId,
                                           SmartActorSpec spec,
                                           SmartActorContext context,
                                           SmartActorWorldSnapshot snapshot,
                                           SmartActorDecision decision) throws GameBuilderException {
        String commandText = commandResolver.resolveCommandText(
                spec,
                snapshot,
                decision.utterance(),
                false,
                action -> allowsVerb(spec, action, false)
        );
        if (commandText == null || commandText.isBlank()) {
            return CommandOutcome.none();
        }
        CommandOutcome outcome = commandResolver.executeCommand(runtime, actorId, commandText);
        historyRecorder.recordHistory(spec, context, "UTTERANCE: " + commandText, turnIndex);
        markActed(actorId);
        return outcome == null ? CommandOutcome.none() : outcome;
    }

    private CommandOutcome handleCombatColor(GameRuntime runtime,
                                             UUID actorId,
                                             SmartActorSpec spec,
                                             SmartActorContext context,
                                             SmartActorDecision decision) {
        int maxLines = Math.max(0, spec.policy().maxColorLines());
        if (maxLines == 0) {
            return combatPass(runtime, actorId, spec, context, "COLOR_SKIPPED");
        }
        String color = commandResolver.trimLines(decision.color(), maxLines);
        if (color.isBlank()) {
            return combatPass(runtime, actorId, spec, context, "COLOR_SKIPPED");
        }
        runtime.narrateColor(color);
        historyRecorder.recordHistory(spec, context, "COLOR: " + color, turnIndex);
        markActed(actorId);
        CommandOutcome outcome = runtime.resolveSmartActorCombatAction(actorId, null);
        return outcome == null ? CommandOutcome.none() : outcome;
    }

    private CommandOutcome handleCombatUtterance(GameRuntime runtime,
                                                 UUID actorId,
                                                 SmartActorSpec spec,
                                                 SmartActorContext context,
                                                 SmartActorWorldSnapshot snapshot,
                                                 SmartActorDecision decision) throws GameBuilderException {
        String commandText = commandResolver.resolveCommandText(
                spec,
                snapshot,
                decision.utterance(),
                true,
                action -> allowsVerb(spec, action, true)
        );
        if (commandText == null || commandText.isBlank()) {
            return combatPass(runtime, actorId, spec, context, "UTTERANCE_EMPTY");
        }
        Command command = interpreter.interpret(commandText);
        if (!commandResolver.isValid(command) || !allowsVerb(spec, command.action(), true)) {
            return combatPass(runtime, actorId, spec, context, "UTTERANCE_INVALID");
        }
        CommandOutcome outcome = runtime.resolveSmartActorCombatAction(actorId, command);
        historyRecorder.recordHistory(spec, context, "UTTERANCE: " + commandText, turnIndex);
        markActed(actorId);
        return outcome == null ? CommandOutcome.none() : outcome;
    }

    private CommandOutcome combatPass(GameRuntime runtime,
                                      UUID actorId,
                                      SmartActorSpec spec,
                                      SmartActorContext context,
                                      String reason) {
        CommandOutcome outcome = runtime.resolveSmartActorCombatAction(actorId, null);
        historyRecorder.recordHistory(spec, context, "PASS: " + reason, turnIndex);
        markActed(actorId);
        return outcome == null ? CommandOutcome.none() : outcome;
    }

    private CommandOutcome combatSkip(GameRuntime runtime, UUID actorId) {
        CommandOutcome outcome = runtime.resolveSmartActorCombatAction(actorId, null);
        return outcome == null ? CommandOutcome.none() : outcome;
    }

    private CommandOutcome fallback(GameRuntime runtime,
                                    UUID actorId,
                                    SmartActorSpec spec,
                                    SmartActorContext context) throws GameBuilderException {
        if (!allowsVerb(spec, CommandAction.LOOK, false)) {
            return CommandOutcome.none();
        }
        CommandOutcome outcome = commandResolver.executeCommand(runtime, actorId, "look");
        historyRecorder.recordHistory(spec, context, "UTTERANCE: look", turnIndex);
        markActed(actorId);
        return outcome == null ? CommandOutcome.none() : outcome;
    }
    private boolean eligible(UUID actorId, SmartActorSpec spec) {
        int cooldown = Math.max(0, spec.policy().cooldownTurns());
        Integer last = lastActionTurn.get(actorId);
        if (last == null) {
            return true;
        }
        return (turnIndex - last) > cooldown;
    }

    private boolean allowsVerb(SmartActorSpec spec, CommandAction action, boolean combatOnly) {
        if (spec == null || action == null) {
            return false;
        }
        if (combatOnly && !COMBAT_ACTIONS.contains(action)) {
            return false;
        }
        List<String> allowed = spec.policy().allowedVerbs();
        if (allowed.isEmpty()) {
            return true;
        }
        return allowed.contains(action.name());
    }

    private String resolveCommandText(SmartActorSpec spec,
                                      SmartActorWorldSnapshot snapshot,
                                      String utterance,
                                      boolean combatOnly) {
        return commandResolver.resolveCommandText(
                spec,
                snapshot,
                utterance,
                combatOnly,
                action -> allowsVerb(spec, action, combatOnly)
        );
    }

    private String trimLines(String text, int maxLines) {
        return commandResolver.trimLines(text, maxLines);
    }

    private List<String> exitsFor(KernelRegistry registry, UUID plotId) {
        return snapshotBuilder.exitsFor(registry, plotId);
    }

    private List<String> recentReceipts(KernelRegistry registry) {
        return snapshotBuilder.recentReceipts(registry);
    }

    private void markActed(UUID actorId) {
        if (actorId != null) {
            lastActionTurn.put(actorId, turnIndex);
        }
    }

    private void markActedNextTurn(UUID actorId) {
        if (actorId != null) {
            lastActionTurn.put(actorId, turnIndex + 1);
        }
    }

    private boolean samePlot(UUID actorPlot, UUID playerPlot) {
        if (actorPlot == null || playerPlot == null) {
            return false;
        }
        return actorPlot.equals(playerPlot);
    }

    private SmartActorWorldSnapshot buildSnapshot(GameRuntime runtime, UUID actorId) throws GameBuilderException {
        return snapshotBuilder.build(runtime, actorId);
    }

    private SmartActorWorldSnapshot buildSnapshot(GameRuntime runtime, UUID actorId, String playerUtterance) throws GameBuilderException {
        return snapshotBuilder.build(runtime, actorId, playerUtterance);
    }
}

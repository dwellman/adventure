package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.TranslationOrchestrator;
import com.demo.adventure.ai.runtime.TranslatorService;
import com.demo.adventure.ai.runtime.smart.SmartActorContext;
import com.demo.adventure.ai.runtime.smart.SmartActorContextBuilder;
import com.demo.adventure.ai.runtime.smart.SmartActorContextInput;
import com.demo.adventure.ai.runtime.smart.SmartActorContextInputBuilder;
import com.demo.adventure.ai.runtime.smart.SmartActorDecision;
import com.demo.adventure.ai.runtime.smart.SmartActorDecisionParser;
import com.demo.adventure.ai.runtime.smart.SmartActorHistoryScope;
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
import com.demo.adventure.engine.command.CommandOutputs;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.domain.kernel.KernelRegistry;
import com.demo.adventure.domain.model.Actor;
import com.demo.adventure.domain.model.Direction;
import com.demo.adventure.domain.model.Gate;
import com.demo.adventure.domain.model.Plot;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class SmartActorRuntime {
    private static final int RECEIPT_LIMIT = 6;
    private static final Set<CommandAction> COMBAT_ACTIONS = EnumSet.of(CommandAction.ATTACK, CommandAction.FLEE);

    private final SmartActorRegistry registry;
    private final SmartActorHistoryStore historyStore;
    private final SmartActorContextInputBuilder inputBuilder;
    private final SmartActorContextBuilder contextBuilder;
    private final SmartActorPlanner planner;
    private final TranslatorService translatorService;
    private final CommandInterpreter interpreter;
    private final Map<CommandAction, GameCommandHandler> handlers;
    private final Map<UUID, Integer> lastActionTurn = new HashMap<>();
    private final boolean debug;
    private boolean localOnly;
    private int turnIndex;
    private long dialogueSequence;

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
        this.translatorService = translatorService;
        this.interpreter = interpreter;
        this.handlers = handlers;
        this.debug = debug;
        SmartActorTagIndex safeTags = tagIndex == null ? SmartActorTagIndex.empty() : tagIndex;
        this.historyStore = new SmartActorHistoryStore();
        registry.entries().values().forEach(historyStore::seedFromSpec);
        this.inputBuilder = new SmartActorContextInputBuilder(safeTags);
        this.contextBuilder = new SmartActorContextBuilder(historyStore);
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
        recordConversationHistory(spec, context, playerUtterance, decision);
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
        String color = trimLines(decision.color(), maxLines);
        if (color.isBlank()) {
            return CommandOutcome.none();
        }
        runtime.narrateColor(color);
        recordHistory(spec, context, "COLOR: " + color);
        markActed(actorId);
        return CommandOutcome.none();
    }

    private CommandOutcome handleUtterance(GameRuntime runtime,
                                           UUID actorId,
                                           SmartActorSpec spec,
                                           SmartActorContext context,
                                           SmartActorWorldSnapshot snapshot,
                                           SmartActorDecision decision) throws GameBuilderException {
        String commandText = resolveCommandText(spec, snapshot, decision.utterance(), false);
        if (commandText == null || commandText.isBlank()) {
            return CommandOutcome.none();
        }
        CommandOutcome outcome = executeCommand(runtime, actorId, commandText);
        recordHistory(spec, context, "UTTERANCE: " + commandText);
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
        String color = trimLines(decision.color(), maxLines);
        if (color.isBlank()) {
            return combatPass(runtime, actorId, spec, context, "COLOR_SKIPPED");
        }
        runtime.narrateColor(color);
        recordHistory(spec, context, "COLOR: " + color);
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
        String commandText = resolveCommandText(spec, snapshot, decision.utterance(), true);
        if (commandText == null || commandText.isBlank()) {
            return combatPass(runtime, actorId, spec, context, "UTTERANCE_EMPTY");
        }
        Command command = interpreter.interpret(commandText);
        if (!isValid(command) || !allowsVerb(spec, command.action(), true)) {
            return combatPass(runtime, actorId, spec, context, "UTTERANCE_INVALID");
        }
        CommandOutcome outcome = runtime.resolveSmartActorCombatAction(actorId, command);
        recordHistory(spec, context, "UTTERANCE: " + commandText);
        markActed(actorId);
        return outcome == null ? CommandOutcome.none() : outcome;
    }

    private CommandOutcome combatPass(GameRuntime runtime,
                                      UUID actorId,
                                      SmartActorSpec spec,
                                      SmartActorContext context,
                                      String reason) {
        CommandOutcome outcome = runtime.resolveSmartActorCombatAction(actorId, null);
        recordHistory(spec, context, "PASS: " + reason);
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
        CommandOutcome outcome = executeCommand(runtime, actorId, "look");
        recordHistory(spec, context, "UTTERANCE: look");
        markActed(actorId);
        return outcome == null ? CommandOutcome.none() : outcome;
    }

    private CommandOutcome executeCommand(GameRuntime runtime,
                                          UUID actorId,
                                          String commandText) throws GameBuilderException {
        Command command = interpreter.interpret(commandText);
        if (!isValid(command)) {
            return CommandOutcome.none();
        }
        GameCommandHandler handler = handlers.get(command.action());
        if (handler == null) {
            return CommandOutcome.none();
        }
        return runtime.runAsActor(actorId, true, true, () -> {
            CommandContext context = new CommandContext(CommandOutputs.noop(), runtime);
            return handler.handle(context, command);
        });
    }

    private String resolveCommandText(SmartActorSpec spec,
                                      SmartActorWorldSnapshot snapshot,
                                      String utterance,
                                      boolean combatOnly) {
        String cleaned = utterance == null ? "" : utterance.trim();
        if (cleaned.isBlank()) {
            return fallbackText(spec, combatOnly);
        }
        int maxLength = spec.policy().maxUtteranceLength();
        if (maxLength > 0 && cleaned.length() > maxLength) {
            return fallbackText(spec, combatOnly);
        }
        Command parsed = interpreter.interpret(cleaned);
        if (isValid(parsed)) {
            if (allowsVerb(spec, parsed.action(), combatOnly)) {
                return cleaned;
            }
            return fallbackText(spec, combatOnly);
        }
        TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                translatorService,
                cleaned,
                snapshot.visibleFixtures(),
                snapshot.visibleItems(),
                snapshot.inventory(),
                snapshot.lastScene(),
                debug,
                interpreter::interpret,
                msg -> {
                    if (debug) {
                        System.out.println(msg);
                    }
                }
        );
        if (outcome.type() != TranslationOrchestrator.OutcomeType.COMMAND) {
            return fallbackText(spec, combatOnly);
        }
        Command translated = interpreter.interpret(outcome.commandText());
        if (!isValid(translated)) {
            return fallbackText(spec, combatOnly);
        }
        if (!allowsVerb(spec, translated.action(), combatOnly)) {
            return fallbackText(spec, combatOnly);
        }
        return outcome.commandText();
    }

    private String fallbackText(SmartActorSpec spec, boolean combatOnly) {
        if (combatOnly) {
            return "";
        }
        return allowsVerb(spec, CommandAction.LOOK, false) ? "look" : "";
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

    private boolean isValid(Command command) {
        return command != null && command.action() != CommandAction.UNKNOWN && !command.hasError();
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

    private void recordHistory(SmartActorSpec spec, SmartActorContext context, String text) {
        if (spec == null || context == null) {
            return;
        }
        if (spec.history() == null || spec.history().storeKey().isBlank()) {
            return;
        }
        String id = spec.actorKey() + ":" + turnIndex;
        historyStore.append(
                spec.history().storeKey(),
                id,
                text,
                context.contextTags(),
                SmartActorHistoryScope.ACTOR,
                turnIndex,
                "smart-actor"
        );
    }

    private void recordConversationHistory(SmartActorSpec spec,
                                           SmartActorContext context,
                                           String playerUtterance,
                                           SmartActorDecision decision) {
        if (spec == null || context == null || decision == null) {
            return;
        }
        if (spec.history() == null || spec.history().storeKey().isBlank()) {
            return;
        }
        long timestamp = nextDialogueTimestamp();
        String storeKey = spec.history().storeKey();
        String actorKey = spec.actorKey() == null ? "actor" : spec.actorKey();
        String utterance = playerUtterance == null ? "" : playerUtterance.trim();
        if (!utterance.isBlank()) {
            historyStore.append(
                    storeKey,
                    actorKey + ":player:" + timestamp,
                    "PLAYER: " + utterance,
                    context.contextTags(),
                    SmartActorHistoryScope.ACTOR,
                    timestamp,
                    "player"
            );
        }
        String reply = switch (decision.type()) {
            case COLOR -> decision.color();
            case UTTERANCE -> decision.utterance();
            case NONE -> "";
        };
        String cleaned = reply == null ? "" : reply.trim();
        if (!cleaned.isBlank()) {
            historyStore.append(
                    storeKey,
                    actorKey + ":reply:" + timestamp,
                    "REPLY: " + cleaned,
                    context.contextTags(),
                    SmartActorHistoryScope.ACTOR,
                    timestamp,
                    "smart-actor"
            );
        }
    }

    private long nextDialogueTimestamp() {
        dialogueSequence++;
        return (turnIndex * 1000L) + dialogueSequence;
    }

    private boolean samePlot(UUID actorPlot, UUID playerPlot) {
        if (actorPlot == null || playerPlot == null) {
            return false;
        }
        return actorPlot.equals(playerPlot);
    }

    private SmartActorWorldSnapshot buildSnapshot(GameRuntime runtime, UUID actorId) throws GameBuilderException {
        return buildSnapshot(runtime, actorId, "");
    }

    private SmartActorWorldSnapshot buildSnapshot(GameRuntime runtime, UUID actorId, String playerUtterance) throws GameBuilderException {
        return runtime.runAsActor(actorId, true, false, () -> {
            Actor actor = runtime.registry().get(actorId) instanceof Actor found ? found : null;
            Plot plot = runtime.currentPlot();
            String actorLabel = actor == null ? "" : Objects.toString(actor.getLabel(), "");
            String actorDescription = actor == null ? "" : Objects.toString(actor.getDescription(), "");
            String plotLabel = plot == null ? "" : Objects.toString(plot.getLabel(), "");
            String plotDescription = plot == null ? "" : Objects.toString(plot.getDescription(), "");
            List<String> fixtures = runtime.visibleFixtureLabels();
            List<String> items = runtime.visibleItemLabels();
            List<String> actors = runtime.visibleActorLabels(actorId);
            List<String> inventory = runtime.inventoryLabels();
            List<String> exits = exitsFor(runtime.registry(), runtime.currentPlotId());
            String lastScene = runtime.lastSceneState();
            List<String> receipts = recentReceipts(runtime.registry());
            return new SmartActorWorldSnapshot(
                    actorLabel,
                    actorDescription,
                    plotLabel,
                    plotDescription,
                    fixtures,
                    items,
                    actors,
                    inventory,
                    exits,
                    lastScene,
                    playerUtterance,
                    receipts
            );
        });
    }

    private List<String> exitsFor(KernelRegistry registry, UUID plotId) {
        if (registry == null || plotId == null) {
            return List.of();
        }
        List<String> exits = new ArrayList<>();
        for (Object value : registry.getEverything().values()) {
            if (!(value instanceof Gate gate)) {
                continue;
            }
            if (!gate.isVisible() || !gate.connects(plotId)) {
                continue;
            }
            Direction direction = gate.directionFrom(plotId);
            if (direction != null) {
                exits.add(direction.toLongName());
            }
        }
        return exits.stream()
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> recentReceipts(KernelRegistry registry) {
        if (registry == null) {
            return List.of();
        }
        List<Object> receipts = registry.getReceipts();
        if (receipts.isEmpty()) {
            return List.of();
        }
        int start = Math.max(0, receipts.size() - RECEIPT_LIMIT);
        return receipts.subList(start, receipts.size()).stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .toList();
    }

    private String trimLines(String text, int maxLines) {
        if (text == null || text.isBlank()) {
            return "";
        }
        if (maxLines <= 0) {
            return "";
        }
        String[] lines = text.trim().split("\\R", -1);
        List<String> kept = new ArrayList<>(Math.min(lines.length, maxLines));
        for (String line : lines) {
            if (kept.size() >= maxLines) {
                break;
            }
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                kept.add(trimmed);
            }
        }
        return String.join("\n", kept).trim();
    }
}

package com.demo.adventure.engine.runtime;

import com.demo.adventure.ai.runtime.TranslationOrchestrator;
import com.demo.adventure.ai.runtime.TranslatorService;
import com.demo.adventure.ai.runtime.smart.SmartActorSpec;
import com.demo.adventure.ai.runtime.smart.SmartActorWorldSnapshot;
import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandOutputs;
import com.demo.adventure.engine.command.handlers.CommandOutcome;
import com.demo.adventure.engine.command.handlers.GameCommandHandler;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
import com.demo.adventure.support.exceptions.GameBuilderException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

final class SmartActorCommandResolver {
    private final TranslatorService translatorService;
    private final CommandInterpreter interpreter;
    private final Map<CommandAction, GameCommandHandler> handlers;
    private final boolean debug;

    SmartActorCommandResolver(
            TranslatorService translatorService,
            CommandInterpreter interpreter,
            Map<CommandAction, GameCommandHandler> handlers,
            boolean debug
    ) {
        this.translatorService = translatorService;
        this.interpreter = interpreter;
        this.handlers = handlers;
        this.debug = debug;
    }

    CommandOutcome executeCommand(GameRuntime runtime, java.util.UUID actorId, String commandText) throws GameBuilderException {
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

    String resolveCommandText(
            SmartActorSpec spec,
            SmartActorWorldSnapshot snapshot,
            String utterance,
            boolean combatOnly,
            Predicate<CommandAction> allowsVerb
    ) {
        String cleaned = utterance == null ? "" : utterance.trim();
        if (cleaned.isBlank()) {
            return fallbackText(spec, combatOnly, allowsVerb);
        }
        int maxLength = spec.policy().maxUtteranceLength();
        if (maxLength > 0 && cleaned.length() > maxLength) {
            return fallbackText(spec, combatOnly, allowsVerb);
        }
        Command parsed = interpreter.interpret(cleaned);
        if (isValid(parsed)) {
            if (allowsVerb.test(parsed.action())) {
                return cleaned;
            }
            return fallbackText(spec, combatOnly, allowsVerb);
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
            return fallbackText(spec, combatOnly, allowsVerb);
        }
        Command translated = interpreter.interpret(outcome.commandText());
        if (!isValid(translated)) {
            return fallbackText(spec, combatOnly, allowsVerb);
        }
        if (!allowsVerb.test(translated.action())) {
            return fallbackText(spec, combatOnly, allowsVerb);
        }
        return outcome.commandText();
    }

    String trimLines(String text, int maxLines) {
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

    boolean isValid(Command command) {
        return command != null && command.action() != CommandAction.UNKNOWN && !command.hasError();
    }

    private String fallbackText(SmartActorSpec spec, boolean combatOnly, Predicate<CommandAction> allowsVerb) {
        if (combatOnly) {
            return "";
        }
        if (spec == null || allowsVerb == null) {
            return "";
        }
        return allowsVerb.test(CommandAction.LOOK) ? "look" : "";
    }
}

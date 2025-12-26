package com.demo.adventure.ai.runtime;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import java.util.List;
import java.util.function.Consumer;

public final class TranslationOrchestrator {
    private TranslationOrchestrator() {}

    public enum OutcomeType {
        COMMAND,
        FAILED
    }

    public record Outcome(
            OutcomeType type,
            String commandText,
            String failureMessage
    ) {
    }

    @FunctionalInterface
    public interface CommandParser {
        Command parse(String input);
    }

    public static Outcome resolve(
            TranslatorService translatorService,
            String input,
            List<String> fixtures,
            List<String> items,
            List<String> inventory,
            String sceneContext,
            boolean translatorDebug,
            CommandParser commandParser,
            Consumer<String> logger
    ) {
        if (translatorDebug) {
            logger.accept("~ translator input='" + input + "'");
        }
        TranslatorService.TranslationResult translated = translatorService.translate(
                input, fixtures, items, inventory, sceneContext
        );
        if (translated.type() != TranslatorService.TranslationResult.Type.COMMAND) {
            if (translatorDebug) {
                logger.accept("~ translator output error: " + translated.error());
            }
            return new Outcome(
                    OutcomeType.FAILED,
                    null,
                    "translator output error: " + translated.error()
            );
        }
        String commandText = translated.command();
        Command command = commandParser.parse(commandText);
        boolean invalid = command.action() == CommandAction.UNKNOWN || command.hasError();
        if (invalid) {
            if (translatorDebug) {
                logger.accept("~ translator invalid command: " + commandText);
            }
            return new Outcome(
                    OutcomeType.FAILED,
                    null,
                    "translator produced invalid command"
            );
        }
        if (translatorDebug) {
            logger.accept("~ translator command: " + commandText);
        }
        return new Outcome(OutcomeType.COMMAND, commandText, null);
    }
}

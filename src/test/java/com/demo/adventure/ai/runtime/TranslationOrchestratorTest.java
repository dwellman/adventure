package com.demo.adventure.ai.runtime;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandPhrase;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TranslationOrchestratorTest {

    @Test
    void translatesOnceWhenLocalParseFails() {
        QueueTranslator translator = new QueueTranslator(List.of(
                commandLine("look")
        ));
        TranslatorService service = new TranslatorService(true, "test", translator);
        List<String> logs = new ArrayList<>();

        TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                service,
                "look around",
                List.of("Desk"),
                List.of("Coin"),
                List.of("Torch"),
                "Hall\nExits: north",
                true,
                TranslationOrchestratorTest::parseCommand,
                logs::add
        );

        assertThat(outcome.type()).isEqualTo(TranslationOrchestrator.OutcomeType.COMMAND);
        assertThat(outcome.commandText()).isEqualTo("look");
        assertThat(translator.prompts()).hasSize(1);
        assertThat(logs).containsExactly(
                "~ translator input='look around'",
                "~ translator command: look"
        );
    }

    @Test
    void failsOnInvalidTranslatorCommand() {
        QueueTranslator translator = new QueueTranslator(List.of(
                commandLine("???")
        ));
        TranslatorService service = new TranslatorService(true, "test", translator);
        List<String> logs = new ArrayList<>();

        TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                service,
                "look around",
                List.of(),
                List.of(),
                List.of(),
                "",
                false,
                TranslationOrchestratorTest::parseCommand,
                logs::add
        );

        assertThat(outcome.type()).isEqualTo(TranslationOrchestrator.OutcomeType.FAILED);
        assertThat(outcome.failureMessage()).contains("translator produced invalid command");
        assertThat(translator.prompts()).hasSize(1);
        assertThat(logs).isEmpty();
    }

    private static Command parseCommand(String commandText) {
        if ("look".equalsIgnoreCase(commandText)) {
            return Command.from(CommandAction.LOOK, CommandPhrase.empty());
        }
        return Command.unknown();
    }

    private static String commandLine(String command) {
        return command;
    }

    private static final class QueueTranslator implements TranslatorService.CommandTranslationClient {
        private final Deque<String> outputs;
        private final List<String> prompts = new ArrayList<>();

        private QueueTranslator(List<String> outputs) {
            this.outputs = new ArrayDeque<>(outputs);
        }

        @Override
        public String translate(String apiKey, String prompt) {
            prompts.add(prompt);
            return outputs.isEmpty() ? "" : outputs.removeFirst();
        }

        private List<String> prompts() {
            return prompts;
        }
    }
}

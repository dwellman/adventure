package com.demo.adventure.ai.runtime;

import com.demo.adventure.engine.command.Command;
import com.demo.adventure.engine.command.CommandAction;
import com.demo.adventure.engine.command.CommandPhrase;
import com.demo.adventure.engine.command.interpreter.CommandInterpreter;
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

    @Test
    void rejectsDirectionCommandWhenInputHasNoDirectionToken() {
        QueueTranslator translator = new QueueTranslator(List.of(
                commandLine("look east")
        ));
        TranslatorService service = new TranslatorService(true, "test", translator);
        List<String> logs = new ArrayList<>();
        CommandInterpreter interpreter = new CommandInterpreter();

        TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                service,
                "can i get on the train",
                List.of(),
                List.of(),
                List.of(),
                "",
                true,
                interpreter::interpret,
                logs::add
        );

        assertThat(outcome.type()).isEqualTo(TranslationOrchestrator.OutcomeType.FAILED);
        assertThat(outcome.failureMessage()).contains("ungrounded command");
        assertThat(logs).containsExactly(
                "~ translator input='can i get on the train'",
                "~ translator rejected ungrounded command: look east"
        );
    }

    @Test
    void rejectsUngroundedIdentifierCommand() {
        QueueTranslator translator = new QueueTranslator(List.of(
                commandLine("inspect ledger")
        ));
        TranslatorService service = new TranslatorService(true, "test", translator);
        List<String> logs = new ArrayList<>();
        CommandInterpreter interpreter = new CommandInterpreter();

        TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                service,
                "can i get on the train",
                List.of(),
                List.of(),
                List.of(),
                "",
                true,
                interpreter::interpret,
                logs::add
        );

        assertThat(outcome.type()).isEqualTo(TranslationOrchestrator.OutcomeType.FAILED);
        assertThat(outcome.failureMessage()).contains("ungrounded command");
        assertThat(logs).containsExactly(
                "~ translator input='can i get on the train'",
                "~ translator rejected ungrounded command: inspect ledger"
        );
    }

    @Test
    void acceptsEmoteWhenGrounded() {
        QueueTranslator translator = new QueueTranslator(List.of(
                commandLine("EMOTE: Do a little dance.")
        ));
        TranslatorService service = new TranslatorService(true, "test", translator);
        List<String> logs = new ArrayList<>();
        CommandInterpreter interpreter = new CommandInterpreter();

        TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                service,
                "Do a little dance.",
                List.of(),
                List.of(),
                List.of(),
                "",
                true,
                interpreter::interpret,
                logs::add
        );

        assertThat(outcome.type()).isEqualTo(TranslationOrchestrator.OutcomeType.EMOTE);
        assertThat(outcome.commandText()).isEqualTo("EMOTE: Do a little dance.");
        assertThat(logs).containsExactly(
                "~ translator input='Do a little dance.'",
                "~ translator emote: Do a little dance."
        );
    }

    @Test
    void acceptsVisibleFixtureLabelNotInInput() {
        QueueTranslator translator = new QueueTranslator(List.of(
                commandLine("inspect case board")
        ));
        TranslatorService service = new TranslatorService(true, "test", translator);
        CommandInterpreter interpreter = new CommandInterpreter();

        TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                service,
                "is there anything interesting about the board?",
                List.of("Case Board"),
                List.of(),
                List.of(),
                "",
                false,
                interpreter::interpret,
                text -> {}
        );

        assertThat(outcome.type()).isEqualTo(TranslationOrchestrator.OutcomeType.COMMAND);
        assertThat(outcome.commandText()).isEqualTo("inspect case board");
    }

    @Test
    void rejectsVisibleLabelWhenInputHasNoOverlap() {
        QueueTranslator translator = new QueueTranslator(List.of(
                commandLine("inspect case board")
        ));
        TranslatorService service = new TranslatorService(true, "test", translator);
        CommandInterpreter interpreter = new CommandInterpreter();

        TranslationOrchestrator.Outcome outcome = TranslationOrchestrator.resolve(
                service,
                "oh cool",
                List.of("Case Board"),
                List.of(),
                List.of(),
                "",
                false,
                interpreter::interpret,
                text -> {}
        );

        assertThat(outcome.type()).isEqualTo(TranslationOrchestrator.OutcomeType.FAILED);
        assertThat(outcome.failureMessage()).contains("ungrounded command");
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

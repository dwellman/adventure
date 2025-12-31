package com.demo.adventure.engine.cli;

import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class GameCliWalkaboutFlowTest {

    @RegisterExtension
    static ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void walkaboutCoversMentionsConversationAndFallback() throws Exception {
        String input = String.join("\n",
                "2",
                "@",
                "@Nobody",
                "@Elias hello",
                "okay, bye",
                "talk Elias Crane",
                "hello",
                "okay, bye",
                "where am i?",
                "go",
                "quit",
                "q",
                ""
        );

        String output = runCli(input);

        assertThat(output).contains("Talk to whom?");
        assertThat(output).contains("You don't see anyone by that name.");
        assertThat(output).contains("You end the conversation.");
        assertThat(output).contains("Thanks for playing.");
    }

    private String runCli(String input) throws Exception {
        InputStream original = System.in;
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            console.reset();
            GameCli cli = new GameCli();
            Method run = GameCli.class.getDeclaredMethod("run");
            run.setAccessible(true);
            invoke(run, cli);
            return console.output();
        } finally {
            System.setIn(original);
        }
    }

    private void invoke(Method method, Object target) throws Exception {
        try {
            method.invoke(target);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception causeException) {
                throw causeException;
            }
            throw ex;
        }
    }
}

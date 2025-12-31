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

class GameCliQuitFlowTest {

    @RegisterExtension
    static ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void quitFromMenuExitsProgram() throws Exception {
        String output = runCli("q\n");

        assertThat(output).contains("Goodbye.");
    }

    @Test
    void quitFromGameReturnsToMenu() throws Exception {
        String output = runCli("1\nquit\nq\n");

        assertThat(output).contains("Thanks for playing.");
        assertThat(countOccurrences(output, "Select a game")).isGreaterThanOrEqualTo(2);
    }

    @Test
    void unknownMenuSelectionPromptsAgain() throws Exception {
        String output = runCli("x\nq\n");

        assertThat(output).contains("Unknown selection: x");
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

    private int countOccurrences(String text, String needle) {
        if (text == null || needle == null || needle.isEmpty()) {
            return 0;
        }
        int count = 0;
        int idx = 0;
        while (true) {
            int next = text.indexOf(needle, idx);
            if (next < 0) {
                break;
            }
            count++;
            idx = next + needle.length();
        }
        return count;
    }
}

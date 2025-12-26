package com.demo.adventure.buui;

import com.demo.adventure.test.ConsoleCaptureExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class ConsolePrinterTest {

    @RegisterExtension
    final ConsoleCaptureExtension console = new ConsoleCaptureExtension();

    @Test
    void renderWrappedUsesEdgePadding() {
        String text = "alpha beta gamma";
        String wrapped = ConsolePrinter.renderWrapped(text, 10, 2);

        assertThat(wrapped).isEqualTo("alpha\nbeta\ngamma");
    }

    @Test
    void renderWrappedClampsNegativePadding() {
        String text = "alpha beta";
        String wrapped = ConsolePrinter.renderWrapped(text, 8, -5);

        assertThat(wrapped).isEqualTo("alpha\nbeta");
    }

    @Test
    void renderWrappedPreservesIndentation() {
        String text = "  alpha beta gamma";
        String wrapped = ConsolePrinter.renderWrapped(text, 10, 0);

        assertThat(wrapped).isEqualTo("  alpha\n  beta\n  gamma");
    }

    @Test
    void printAddsLeftGutter() {
        console.reset();
        ConsolePrinter.print("alpha", 10, 0);

        assertThat(console.output()).isEqualTo("  alpha\n");
    }

    @Test
    void printHonorsMutedOutput() {
        ConsolePrinter.setMuted(true);
        try {
            console.reset();
            ConsolePrinter.print("alpha", 10, 0);
        } finally {
            ConsolePrinter.setMuted(false);
        }

        assertThat(console.output()).isEmpty();
    }
}

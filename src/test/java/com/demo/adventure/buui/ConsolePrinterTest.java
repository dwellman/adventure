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
    void printAddsMarkdownIndent() {
        console.reset();
        ConsolePrinter.print("alpha", 20, 0);

        assertThat(console.output()).isEqualTo("  alpha\n");
    }

    @Test
    void printlnKeepsPlainIndent() {
        console.reset();
        ConsolePrinter.println("alpha", 10, 0);

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

    @Test
    void printNarrationUsesParagraphBullets() {
        String previousGutter = System.getProperty("BUUI_GUTTER");
        AnsiStyle.setEnabledOverride(false);
        try {
            System.setProperty("BUUI_GUTTER", "2");
            console.reset();
            ConsolePrinter.printNarration("Alpha beta gamma delta\n\nEpsilon zeta eta theta", 20, 0);

            String bullet = ListRenderer.BULLET;
            String expected = bullet + " Alpha beta gamma\n"
                    + "  delta\n"
                    + "\n"
                    + bullet + " Epsilon zeta eta\n"
                    + "  theta\n";

            assertThat(console.output()).isEqualTo(expected);
        } finally {
            AnsiStyle.setEnabledOverride(null);
            if (previousGutter == null) {
                System.clearProperty("BUUI_GUTTER");
            } else {
                System.setProperty("BUUI_GUTTER", previousGutter);
            }
        }
    }
}

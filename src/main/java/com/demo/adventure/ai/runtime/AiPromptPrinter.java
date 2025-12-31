package com.demo.adventure.ai.runtime;

import com.demo.adventure.buui.BuuiConsole;

public final class AiPromptPrinter extends BuuiConsole {
    private static final AiConfig CONFIG = AiConfig.load();
    private static final boolean PRINT_PROMPTS = CONFIG.getBoolean("ai.prompts.print", false);

    private AiPromptPrinter() {
    }

    public static void printChatPrompt(String label, String systemPrompt, String userPrompt, boolean debug) {
        if (!shouldPrint(debug)) {
            return;
        }
        String prefix = normalizeLabel(label);
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            printText("~ " + prefix + " system prompt:\n" + systemPrompt);
        }
        if (userPrompt != null && !userPrompt.isBlank()) {
            printText("~ " + prefix + " user prompt:\n" + userPrompt);
        }
    }

    private static boolean shouldPrint(boolean debug) {
        return debug || PRINT_PROMPTS;
    }

    private static String normalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return "prompt";
        }
        return label.trim();
    }
}

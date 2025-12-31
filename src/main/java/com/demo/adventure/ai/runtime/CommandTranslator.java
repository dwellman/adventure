package com.demo.adventure.ai.runtime;

import com.demo.adventure.ai.client.AiChatClient;
import com.demo.adventure.ai.client.AiChatMessage;
import com.demo.adventure.ai.client.AiChatRequest;
import com.demo.adventure.ai.client.AiChatResponse;
import com.demo.adventure.ai.client.OpenAiChatClient;
import com.demo.adventure.buui.BuuiConsole;

import java.time.Duration;
import java.util.List;

/**
 * Lightweight OpenAI chat client for the command translator prompt.
 */
public final class CommandTranslator extends BuuiConsole {
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String SYSTEM_PROMPT = "You are the Command Translator for a turn-based CLI adventure game. Follow the output contract exactly.";
    private static final String MODEL = CONFIG.getString("ai.translator.model", "gpt-4o-mini");
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final double TEMPERATURE = CONFIG.getDouble("ai.translator.temperature", 0.0);
    private static final double TOP_P = CONFIG.getDouble("ai.translator.top_p", 1.0);
    private static final boolean ENABLE_LOGPROBS = CONFIG.getBoolean("ai.translator.logprobs", false);
    private static final int TOP_LOGPROBS = CONFIG.getInt("ai.translator.top_logprobs", 3);
    private static final AiChatClient CHAT_CLIENT = new OpenAiChatClient();

    private CommandTranslator() {}

    // Pattern: Orchestration + Grounding
    // - Issues a single, explicitly configured call so translation is deterministic and prompt-driven.
    public static String translate(String apiKey, String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        AiPromptPrinter.printChatPrompt("translator", SYSTEM_PROMPT, prompt, false);
        // Orchestration + grounding: deterministic API call, no hidden state, model/temperature/top_p driven by config.
        AiChatRequest request = AiChatRequest.builder()
                .model(MODEL)
                .messages(List.of(
                        AiChatMessage.system(SYSTEM_PROMPT),
                        AiChatMessage.user(prompt)
                ))
                .temperature(TEMPERATURE)
                .topP(TOP_P)
                .logprobs(ENABLE_LOGPROBS)
                .topLogprobs(ENABLE_LOGPROBS ? TOP_LOGPROBS : null)
                .timeout(TIMEOUT)
                .build();
        AiChatResponse response = CHAT_CLIENT.chat(apiKey, request);
        if (response == null) {
            return null;
        }
        if (ENABLE_LOGPROBS && !response.logprobSnippet().isBlank()) {
            printText("~ translator logprobs: " + response.logprobSnippet());
        }
        return response.content();
    }
}

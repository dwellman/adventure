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
 * OpenAI narrator client backed by the internal chat client.
 */
public final class NarratorService extends BuuiConsole {
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String SYSTEM_PROMPT = "You are the Narrator for a turn-based CLI adventure game. Follow the output contract exactly.";
    private static final String MODEL = CONFIG.getString("ai.narrator.model", "gpt-4o-mini");
    private static final double TEMPERATURE = CONFIG.getDouble("ai.narrator.temperature", 0.3);
    private static final double TOP_P = CONFIG.getDouble("ai.narrator.top_p", 1.0);
    private static final boolean ENABLE_LOGPROBS = CONFIG.getBoolean("ai.narrator.logprobs", false);
    private static final int TOP_LOGPROBS = CONFIG.getInt("ai.narrator.top_logprobs", 3);
    private static final Duration TIMEOUT = Duration.ofSeconds(40);
    private static final AiChatClient CHAT_CLIENT = new OpenAiChatClient();

    private NarratorService() {}

    // Pattern: Orchestration + Trust UX
    // - Uses explicit model parameters and debug visibility to keep narration controlled and auditable.
    public static String rewrite(String apiKey, String prompt, boolean debug) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        AiPromptPrinter.printChatPrompt("narrator", SYSTEM_PROMPT, prompt, debug);
        AiChatRequest chatRequest = AiChatRequest.builder()
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
        AiChatResponse response = CHAT_CLIENT.chat(apiKey, chatRequest);
        if (response == null) {
            return null;
        }
        String text = response.content();
        if (debug) {
            printText("~ narrator response:\n" + (text == null ? "(empty)" : text.trim()));
        }
        return text;
    }
}

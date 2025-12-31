package com.demo.adventure.ai.authoring;

import com.demo.adventure.ai.client.AiChatClient;
import com.demo.adventure.ai.client.AiChatMessage;
import com.demo.adventure.ai.client.AiChatRequest;
import com.demo.adventure.ai.client.AiChatResponse;
import com.demo.adventure.ai.client.OpenAiChatClient;
import com.demo.adventure.ai.runtime.AiConfig;
import com.demo.adventure.ai.runtime.AiPromptPrinter;
import com.demo.adventure.buui.BuuiConsole;

import java.time.Duration;
import java.util.List;

/**
 * Minimal OpenAI client for authoring-time agents (constraints/story/planning/GDL).
 */
public final class AuthoringAgentClient extends BuuiConsole {
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String MODEL = CONFIG.getString("ai.authoring.model", "gpt-4o-mini");
    private static final double TEMPERATURE = CONFIG.getDouble("ai.authoring.temperature", 0.2);
    private static final double TOP_P = CONFIG.getDouble("ai.authoring.top_p", 1.0);
    private static final boolean ENABLE_LOGPROBS = CONFIG.getBoolean("ai.authoring.logprobs", false);
    private static final int TOP_LOGPROBS = CONFIG.getInt("ai.authoring.top_logprobs", 3);
    private static final Duration TIMEOUT = Duration.ofSeconds(40);
    private static final AiChatClient CHAT_CLIENT = new OpenAiChatClient();

    private AuthoringAgentClient() {}

    public static String request(String apiKey, String systemPrompt, String userPrompt, boolean debug) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        AiPromptPrinter.printChatPrompt("authoring", systemPrompt, userPrompt, debug);
        AiChatRequest request = AiChatRequest.builder()
                .model(MODEL)
                .messages(List.of(
                        AiChatMessage.system(systemPrompt),
                        AiChatMessage.user(userPrompt)
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
            printText("~ authoring logprobs: " + response.logprobSnippet());
        }
        String content = response.content();
        if (debug) {
            printText("~ authoring response:\n" + (content == null ? "(empty)" : content.trim()));
        }
        return content;
    }
}

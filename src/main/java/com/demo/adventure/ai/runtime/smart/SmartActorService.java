package com.demo.adventure.ai.runtime.smart;

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

public final class SmartActorService extends BuuiConsole {
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String MODEL = CONFIG.getString("ai.smart_actor.model", "gpt-4o-mini");
    private static final double TEMPERATURE = CONFIG.getDouble("ai.smart_actor.temperature", 0.4);
    private static final double TOP_P = CONFIG.getDouble("ai.smart_actor.top_p", 1.0);
    private static final boolean ENABLE_LOGPROBS = CONFIG.getBoolean("ai.smart_actor.logprobs", false);
    private static final int TOP_LOGPROBS = CONFIG.getInt("ai.smart_actor.top_logprobs", 3);
    private static final Duration TIMEOUT = Duration.ofSeconds(40);
    private static final AiChatClient CHAT_CLIENT = new OpenAiChatClient();

    private SmartActorService() {
    }

    public static String request(String apiKey, String systemPrompt, String userPrompt, boolean debug) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        AiPromptPrinter.printChatPrompt("smart actor", systemPrompt, userPrompt, debug);
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
            printText("~ smart actor logprobs: " + response.logprobSnippet());
        }
        String content = response.content();
        if (debug) {
            printText("~ smart actor response:\n" + (content == null ? "(empty)" : content.trim()));
        }
        return content;
    }
}

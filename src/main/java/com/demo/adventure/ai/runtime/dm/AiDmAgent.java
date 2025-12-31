package com.demo.adventure.ai.runtime.dm;

import com.demo.adventure.ai.client.AiChatClient;
import com.demo.adventure.ai.client.AiChatMessage;
import com.demo.adventure.ai.client.AiChatRequest;
import com.demo.adventure.ai.client.AiChatResponse;
import com.demo.adventure.ai.client.OpenAiChatClient;
import com.demo.adventure.ai.runtime.AiConfig;
import com.demo.adventure.ai.runtime.AiPromptPrinter;

import java.time.Duration;
import java.util.List;

/**
 * DM agent backed by an LLM. Returns a rewritten narration line or null to fall back.
 */
public final class AiDmAgent implements DmAgent {
    private static final String SYSTEM_PROMPT = """
            You rewrite short narration lines for a hopeful survival adventure. Keep facts true.
            - Stay concise (1–2 sentences).
            - Use the provided context only; do not invent items or exits.
            - Keep tone warm and curious.
            """;
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String MODEL = CONFIG.getString("ai.narrator.model", "gpt-4o-mini");
    private static final double TEMPERATURE = CONFIG.getDouble("ai.narrator.temperature", 0.3);
    private static final double TOP_P = CONFIG.getDouble("ai.narrator.top_p", 1.0);
    private static final Duration TIMEOUT = Duration.ofSeconds(40);
    private static final AiChatClient DEFAULT_CLIENT = new OpenAiChatClient();

    private final AiChatClient chatClient;
    private final String apiKey;

    public AiDmAgent(AiChatClient chatClient) {
        this(chatClient, null);
    }

    public AiDmAgent(AiChatClient chatClient, String apiKey) {
        this.chatClient = chatClient == null ? DEFAULT_CLIENT : chatClient;
        this.apiKey = apiKey;
    }

    @Override
    // Pattern: Trust UX
    // - Returns null on failure so the deterministic base narration remains authoritative.
    public String narrate(DmAgentContext context) throws Exception {
        if (chatClient == null || context == null || apiKey == null || apiKey.isBlank()) {
            return null;
        }
        try {
            String user = buildUserPrompt(context);
            AiPromptPrinter.printChatPrompt("dm", SYSTEM_PROMPT, user, false);
            AiChatRequest request = AiChatRequest.builder()
                    .model(MODEL)
                    .messages(List.of(
                            AiChatMessage.system(SYSTEM_PROMPT),
                            AiChatMessage.user(user)
                    ))
                    .temperature(TEMPERATURE)
                    .topP(TOP_P)
                    .timeout(TIMEOUT)
                    .build();
            AiChatResponse response = chatClient.chat(apiKey, request);
            return response == null ? null : response.content();
        } catch (Exception ex) {
            return null; // fail safe: do not alter narration on agent failure
        }
    }

    private static String buildUserPrompt(DmAgentContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Base: ").append(ctx.baseText()).append('\n');
        sb.append("Target: ").append(ctx.targetLabel()).append('\n');
        sb.append("TargetDescription: ").append(ctx.targetDescription()).append('\n');
        if (!ctx.fixtureSummaries().isEmpty()) {
            sb.append("Fixtures:\n");
            ctx.fixtureSummaries().forEach(f -> sb.append("- ").append(f).append('\n'));
        }
        if (!ctx.inventorySummaries().isEmpty()) {
            sb.append("Contents:\n");
            ctx.inventorySummaries().forEach(i -> sb.append("- ").append(i).append('\n'));
        }
        sb.append("Rewrite the base line, keeping it truthful.");
        return sb.toString();
    }
}

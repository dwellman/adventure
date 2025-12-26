package com.demo.adventure.ai.runtime.dm;

import org.springframework.ai.chat.client.ChatClient;

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

    private final ChatClient chatClient;

    public AiDmAgent(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    // Pattern: Trust UX
    // - Returns null on failure so the deterministic base narration remains authoritative.
    public String narrate(DmAgentContext context) throws Exception {
        if (chatClient == null || context == null) {
            return null;
        }
        try {
            String user = buildUserPrompt(context);
            return chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(user)
                    .call()
                    .content();
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

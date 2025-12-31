package com.demo.adventure.ai.client;

public record AiChatResponse(String content, String logprobSnippet) {
    public AiChatResponse {
        logprobSnippet = logprobSnippet == null ? "" : logprobSnippet;
    }
}

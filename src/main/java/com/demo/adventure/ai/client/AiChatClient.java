package com.demo.adventure.ai.client;

public interface AiChatClient {
    AiChatResponse chat(String apiKey, AiChatRequest request) throws Exception;
}

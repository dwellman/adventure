package com.demo.adventure.ai.client;

public record AiChatMessage(Role role, String content) {
    public enum Role {SYSTEM, USER, ASSISTANT}

    public AiChatMessage {
        role = role == null ? Role.USER : role;
        content = content == null ? "" : content;
    }

    public static AiChatMessage system(String content) {
        return new AiChatMessage(Role.SYSTEM, content);
    }

    public static AiChatMessage user(String content) {
        return new AiChatMessage(Role.USER, content);
    }

    public static AiChatMessage assistant(String content) {
        return new AiChatMessage(Role.ASSISTANT, content);
    }
}

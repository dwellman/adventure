package com.demo.adventure.ai.runtime.smart;

public record SmartActorPrompt(String systemPrompt, String userPrompt) {
    public SmartActorPrompt {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt is required");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("userPrompt is required");
        }
    }
}

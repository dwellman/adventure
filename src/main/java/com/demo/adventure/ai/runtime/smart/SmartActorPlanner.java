package com.demo.adventure.ai.runtime.smart;

public final class SmartActorPlanner {
    @FunctionalInterface
    public interface DecisionClient {
        String request(String apiKey, String systemPrompt, String userPrompt, boolean debug) throws Exception;
    }

    private final boolean aiEnabled;
    private final String apiKey;
    private final boolean debug;
    private final DecisionClient client;

    public SmartActorPlanner(boolean aiEnabled, String apiKey, boolean debug) {
        this(aiEnabled, apiKey, debug, SmartActorService::request);
    }

    SmartActorPlanner(boolean aiEnabled, String apiKey, boolean debug, DecisionClient client) {
        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        this.debug = debug;
        this.client = client == null ? SmartActorService::request : client;
    }

    public SmartActorDecisionParser.Result decide(SmartActorPrompt prompt) {
        if (!aiEnabled) {
            return SmartActorDecisionParser.Result.error("smart actor AI disabled");
        }
        if (prompt == null) {
            return SmartActorDecisionParser.Result.error("missing prompt");
        }
        try {
            String response = client.request(apiKey, prompt.systemPrompt(), prompt.userPrompt(), debug);
            if (response == null || response.isBlank()) {
                return SmartActorDecisionParser.Result.error("smart actor returned empty response");
            }
            return SmartActorDecisionParser.parse(response);
        } catch (Exception ex) {
            return SmartActorDecisionParser.Result.error(ex.getMessage());
        }
    }
}

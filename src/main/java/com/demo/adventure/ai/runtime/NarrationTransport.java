package com.demo.adventure.ai.runtime;

final class NarrationTransport {
    private final boolean aiEnabled;
    private final String apiKey;
    private final boolean debug;
    private final NarrationService.NarratorRewriter rewriter;

    NarrationTransport(boolean aiEnabled, String apiKey, boolean debug, NarrationService.NarratorRewriter rewriter) {
        this.aiEnabled = aiEnabled;
        this.apiKey = apiKey;
        this.debug = debug;
        this.rewriter = rewriter;
    }

    boolean isEnabled() {
        return aiEnabled;
    }

    boolean isDebug() {
        return debug;
    }

    String rewrite(String prompt) throws Exception {
        if (!aiEnabled || prompt == null || prompt.isBlank()) {
            return null;
        }
        return rewriter.rewrite(apiKey, prompt, debug);
    }
}

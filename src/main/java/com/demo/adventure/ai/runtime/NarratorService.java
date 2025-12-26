package com.demo.adventure.ai.runtime;

import com.demo.adventure.buui.BuuiConsole;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;

import java.util.List;

/**
 * OpenAI narrator client backed by Spring AI.
 */
public final class NarratorService extends BuuiConsole {
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String MODEL = CONFIG.getString("ai.narrator.model", "gpt-4o-mini");
    private static final double TEMPERATURE = CONFIG.getDouble("ai.narrator.temperature", 0.3);
    private static final double TOP_P = CONFIG.getDouble("ai.narrator.top_p", 1.0);
    private static final boolean ENABLE_LOGPROBS = CONFIG.getBoolean("ai.narrator.logprobs", false);
    private static final int TOP_LOGPROBS = CONFIG.getInt("ai.narrator.top_logprobs", 3);

    private NarratorService() {}

    // Pattern: Orchestration + Trust UX
    // - Uses explicit model parameters and debug visibility to keep narration controlled and auditable.
    public static String rewrite(String apiKey, String prompt, boolean debug) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        OpenAiApi api = OpenAiApi.builder()
                .apiKey(apiKey)
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(MODEL)
                .temperature(TEMPERATURE)
                .topP(TOP_P)
                .logprobs(ENABLE_LOGPROBS)
                .topLogprobs(ENABLE_LOGPROBS ? TOP_LOGPROBS : null)
                .build();
        OpenAiChatModel model = OpenAiChatModel.builder()
                .openAiApi(api)
                .defaultOptions(options)
                .toolCallingManager(DefaultToolCallingManager.builder().build())
                .retryTemplate(RetryTemplate.builder().build())
                .observationRegistry(ObservationRegistry.NOOP)
                .build();
        Prompt chatPrompt = new Prompt(List.of(
                new SystemMessage("You are the Narrator for a turn-based CLI adventure game. Follow the output contract exactly."),
                new UserMessage(prompt)
        ));
        if (debug) {
            printText("~ narrator prompt:\n" + prompt);
        }
        ChatResponse response = model.call(chatPrompt);
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        String text = response.getResult().getOutput().getText();
        if (debug) {
            printText("~ narrator response:\n" + (text == null ? "(empty)" : text.trim()));
        }
        return text;
    }
}

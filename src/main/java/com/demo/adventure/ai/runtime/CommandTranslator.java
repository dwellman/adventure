package com.demo.adventure.ai.runtime;

import com.demo.adventure.buui.BuuiConsole;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Lightweight OpenAI chat client for the command translator prompt.
 */
public final class CommandTranslator extends BuuiConsole {
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String MODEL = CONFIG.getString("ai.translator.model", "gpt-4o-mini");
    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions");
    private static final Duration TIMEOUT = Duration.ofSeconds(20);
    private static final double TEMPERATURE = CONFIG.getDouble("ai.translator.temperature", 0.0);
    private static final double TOP_P = CONFIG.getDouble("ai.translator.top_p", 1.0);
    private static final boolean ENABLE_LOGPROBS = CONFIG.getBoolean("ai.translator.logprobs", false);
    private static final int TOP_LOGPROBS = CONFIG.getInt("ai.translator.top_logprobs", 3);

    private CommandTranslator() {}

    // Pattern: Orchestration + Grounding
    // - Issues a single, explicitly configured call so translation is deterministic and prompt-driven.
    public static String translate(String apiKey, String prompt) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        // Orchestration + grounding: deterministic API call, no hidden state, model/temperature/top_p driven by config.
        String logprobBlock = ENABLE_LOGPROBS
                ? ",\n  \"logprobs\": true,\n  \"top_logprobs\": " + TOP_LOGPROBS
                : "";
        String topPBlock = ",\n  \"top_p\": " + TOP_P;
        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role":"system","content":%s},
                    {"role":"user","content":%s}
                  ],
                  "temperature": %s%s%s
                }
                """.formatted(
                MODEL,
                jsonEscape("You are the Command Translator for a turn-based CLI adventure game. Follow the output contract exactly."),
                jsonEscape(prompt),
                TEMPERATURE,
                topPBlock,
                logprobBlock
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(ENDPOINT)
                .timeout(TIMEOUT)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("status " + response.statusCode());
        }
        return extractContent(response.body());
    }

    private static String jsonEscape(String text) {
        String escaped = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
        return "\"" + escaped + "\"";
    }

    private static String extractContent(String response) {
        String content = extractJsonString(response, "content");
        String logprobSnippet = extractLogprobSnippet(response);
        if (!logprobSnippet.isBlank()) {
            printText("~ translator logprobs: " + logprobSnippet);
        }
        return content;
    }

    private static String extractLogprobSnippet(String response) {
        int logIdx = response.indexOf("\"top_logprobs\"");
        if (logIdx < 0) {
            return "";
        }
        int brace = response.indexOf('{', logIdx);
        int close = response.indexOf('}', brace);
        if (brace > 0 && close > brace) {
            return response.substring(brace, Math.min(response.length(), brace + 200));
        }
        return "";
    }

    private static String extractJsonString(String json, String key) {
        if (json == null || key == null) {
            return null;
        }
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return null;
        }
        int colon = json.indexOf(':', keyIndex);
        if (colon < 0) {
            return null;
        }
        int quote = json.indexOf('"', colon);
        if (quote < 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        boolean escape = false;
        for (int i = quote + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escape) {
                switch (c) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    case '"' -> sb.append('"');
                    case '\\' -> sb.append('\\');
                    default -> sb.append(c);
                }
                escape = false;
                continue;
            }
            if (c == '\\') {
                escape = true;
                continue;
            }
            if (c == '"') {
                break;
            }
            sb.append(c);
        }
        return sb.toString();
    }
}

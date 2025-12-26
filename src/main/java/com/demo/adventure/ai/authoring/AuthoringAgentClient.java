package com.demo.adventure.ai.authoring;

import com.demo.adventure.ai.runtime.AiConfig;
import com.demo.adventure.buui.BuuiConsole;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Minimal OpenAI client for authoring-time agents (constraints/story/planning/GDL).
 */
public final class AuthoringAgentClient extends BuuiConsole {
    private static final AiConfig CONFIG = AiConfig.load();
    private static final String MODEL = CONFIG.getString("ai.authoring.model", "gpt-4o-mini");
    private static final double TEMPERATURE = CONFIG.getDouble("ai.authoring.temperature", 0.2);
    private static final double TOP_P = CONFIG.getDouble("ai.authoring.top_p", 1.0);
    private static final boolean ENABLE_LOGPROBS = CONFIG.getBoolean("ai.authoring.logprobs", false);
    private static final int TOP_LOGPROBS = CONFIG.getInt("ai.authoring.top_logprobs", 3);
    private static final URI ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions");
    private static final Duration TIMEOUT = Duration.ofSeconds(40);

    private AuthoringAgentClient() {}

    public static String request(String apiKey, String systemPrompt, String userPrompt, boolean debug) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }
        String logprobBlock = ENABLE_LOGPROBS
                ? ",\n  \"logprobs\": true,\n  \"top_logprobs\": " + TOP_LOGPROBS
                : "";
        String body = """
                {
                  "model": "%s",
                  "messages": [
                    {"role":"system","content":%s},
                    {"role":"user","content":%s}
                  ],
                  "temperature": %s,
                  "top_p": %s%s
                }
                """.formatted(
                MODEL,
                jsonEscape(systemPrompt),
                jsonEscape(userPrompt),
                TEMPERATURE,
                TOP_P,
                logprobBlock
        );

        if (debug) {
            printText("~ authoring system prompt:\n" + systemPrompt);
            printText("~ authoring user prompt:\n" + userPrompt);
        }

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
        String content = extractContent(response.body());
        if (debug) {
            printText("~ authoring response:\n" + (content == null ? "(empty)" : content.trim()));
        }
        return content;
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
            printText("~ authoring logprobs: " + logprobSnippet);
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

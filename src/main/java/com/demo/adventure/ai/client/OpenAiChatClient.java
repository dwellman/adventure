package com.demo.adventure.ai.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class OpenAiChatClient implements AiChatClient {
    private static final URI DEFAULT_ENDPOINT = URI.create("https://api.openai.com/v1/chat/completions");
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    @Override
    public AiChatResponse chat(String apiKey, AiChatRequest request) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            return new AiChatResponse(null, "");
        }
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        URI endpoint = request.endpoint() == null ? DEFAULT_ENDPOINT : request.endpoint();
        Duration timeout = request.timeout() == null ? Duration.ofSeconds(30) : request.timeout();
        String body = buildBody(request);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(endpoint)
                .timeout(timeout)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("status " + response.statusCode());
        }
        String raw = response.body();
        String content = AiJson.extractJsonString(raw, "content");
        String logprobSnippet = request.logprobs() ? AiJson.extractLogprobSnippet(raw) : "";
        return new AiChatResponse(content, logprobSnippet);
    }

    private static String buildBody(AiChatRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"model\": ").append(AiJson.escape(nullToEmpty(request.model()))).append(",\n");
        sb.append("  \"messages\": [\n");
        List<AiChatMessage> messages = request.messages().stream()
                .filter(Objects::nonNull)
                .toList();
        for (int i = 0; i < messages.size(); i++) {
            AiChatMessage message = messages.get(i);
            sb.append("    {\"role\":\"")
                    .append(role(message))
                    .append("\",\"content\":")
                    .append(AiJson.escape(message.content()))
                    .append("}");
            if (i < messages.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("  ],\n");
        sb.append("  \"temperature\": ").append(request.temperature()).append(",\n");
        sb.append("  \"top_p\": ").append(request.topP());
        if (request.logprobs()) {
            sb.append(",\n  \"logprobs\": true");
            if (request.topLogprobs() != null) {
                sb.append(",\n  \"top_logprobs\": ").append(request.topLogprobs());
            }
        }
        AiChatRequest.ResponseFormat responseFormat = request.responseFormat();
        if (responseFormat != null && !responseFormat.type().isBlank()) {
            sb.append(",\n  \"response_format\": {\"type\": ")
                    .append(AiJson.escape(responseFormat.type()))
                    .append("}");
        }
        sb.append("\n}");
        return sb.toString();
    }

    private static String role(AiChatMessage message) {
        if (message == null || message.role() == null) {
            return "user";
        }
        return message.role().name().toLowerCase(Locale.ROOT);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

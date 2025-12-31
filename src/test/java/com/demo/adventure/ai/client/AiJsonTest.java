package com.demo.adventure.ai.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiJsonTest {

    @Test
    void escapeHandlesBackslashes() {
        String escaped = AiJson.escape("C:\\path");
        assertThat(escaped).isEqualTo("\"C:\\\\path\"");
    }

    @Test
    void escapeHandlesQuotesAndNewlines() {
        String escaped = AiJson.escape("Line\n\"Quote\"");
        assertThat(escaped).isEqualTo("\"Line\\n\\\"Quote\\\"\"");
    }

    @Test
    void escapeHandlesTabs() {
        String escaped = AiJson.escape("tab\t\"quote\"");
        assertThat(escaped).isEqualTo("\"tab\\t\\\"quote\\\"\"");
    }

    @Test
    void extractJsonStringHandlesEscapes() {
        String json = "{\"content\":\"Line1\\nLine2\"}";
        String content = AiJson.extractJsonString(json, "content");
        assertThat(content).isEqualTo("Line1\nLine2");
    }

    @Test
    void extractJsonStringReturnsNullWhenMissing() {
        String content = AiJson.extractJsonString("{}", "content");
        assertThat(content).isNull();
    }

    @Test
    void extractLogprobSnippetReturnsEmptyWhenMissing() {
        String snippet = AiJson.extractLogprobSnippet("{}");
        assertThat(snippet).isEqualTo("");
    }

    @Test
    void extractLogprobSnippetReturnsSnippet() {
        String response = "{\"top_logprobs\":{\"token\":\"A\"},\"choices\":[{\"message\":{\"content\":\"ok\"}}]}";
        String snippet = AiJson.extractLogprobSnippet(response);
        assertThat(snippet).contains("{").contains("token");
    }
}

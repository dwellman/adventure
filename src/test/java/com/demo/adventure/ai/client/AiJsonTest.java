package com.demo.adventure.ai.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiJsonTest {

    @Test
    void escapeAddsQuotesAndEscapesControlChars() {
        String escaped = AiJson.escape("a\"b\nc\\");

        assertThat(escaped).startsWith("\"").endsWith("\"");
        assertThat(escaped).contains("\\\"");
        assertThat(escaped).contains("\\n");
        assertThat(escaped).contains("\\\\");
    }

    @Test
    void escapeHandlesNull() {
        assertThat(AiJson.escape(null)).isEqualTo("\"\"");
    }

    @Test
    void extractJsonStringReadsEscapes() {
        String json = "{\"content\":\"line1\\nline2\\tend\"}";

        String content = AiJson.extractJsonString(json, "content");

        assertThat(content).isEqualTo("line1\nline2\tend");
    }

    @Test
    void extractJsonStringReturnsNullWhenMissing() {
        String json = "{\"other\":\"value\"}";

        assertThat(AiJson.extractJsonString(json, "content")).isNull();
        assertThat(AiJson.extractJsonString(null, "content")).isNull();
        assertThat(AiJson.extractJsonString(json, null)).isNull();
    }

    @Test
    void extractLogprobSnippetFindsBlock() {
        String json = "{\"top_logprobs\":{\"foo\":1,\"bar\":2}}";

        String snippet = AiJson.extractLogprobSnippet(json);

        assertThat(snippet).contains("\"foo\"");
        assertThat(AiJson.extractLogprobSnippet("{\"content\":\"x\"}")).isEmpty();
        assertThat(AiJson.extractLogprobSnippet(null)).isEmpty();
    }
}

package com.demo.adventure.ai.authoring;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AuthoringAgentClientTest {

    @Test
    void requestReturnsNullWhenApiKeyMissing() throws Exception {
        String result = AuthoringAgentClient.request("", "system", "user", false);
        assertThat(result).isNull();
    }

    @Test
    void jsonEscapeEscapesQuotesAndNewlines() throws Exception {
        String escaped = (String) invoke("jsonEscape", new Class<?>[]{String.class}, "Line\n\"Quote\"");

        assertThat(escaped).isEqualTo("\"Line\\n\\\"Quote\\\"\"");
    }

    @Test
    void extractsContentAndLogprobSnippet() throws Exception {
        String response = "{\"choices\":[{\"message\":{\"content\":\"Hello\\nWorld\"}}],\"top_logprobs\":{\"token\":\"X\"}}";
        String content = (String) invoke("extractContent", new Class<?>[]{String.class}, response);
        String snippet = (String) invoke("extractLogprobSnippet", new Class<?>[]{String.class}, response);

        assertThat(content).isEqualTo("Hello\nWorld");
        assertThat(snippet).contains("{").contains("token");
    }

    @Test
    void extractJsonStringHandlesMissingKey() throws Exception {
        String content = (String) invoke("extractJsonString", new Class<?>[]{String.class, String.class}, "{}", "content");

        assertThat(content).isNull();
    }

    private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = AuthoringAgentClient.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}

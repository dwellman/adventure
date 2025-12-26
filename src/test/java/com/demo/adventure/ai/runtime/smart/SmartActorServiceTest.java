package com.demo.adventure.ai.runtime.smart;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorServiceTest {

    @Test
    void requestReturnsNullWhenApiKeyMissing() throws Exception {
        String result = SmartActorService.request("", "system", "user", false);
        assertThat(result).isNull();
    }

    @Test
    void jsonEscapeHandlesTabsAndQuotes() throws Exception {
        String escaped = (String) invoke("jsonEscape", new Class<?>[]{String.class}, "tab\t\"quote\"");

        assertThat(escaped).isEqualTo("\"tab\\t\\\"quote\\\"\"");
    }

    @Test
    void extractJsonStringHandlesMissingKey() throws Exception {
        String content = (String) invoke("extractJsonString", new Class<?>[]{String.class, String.class}, "{\"x\":1}", "content");
        assertThat(content).isNull();
    }

    @Test
    void extractLogprobSnippetReturnsSnippet() throws Exception {
        String response = "{\"top_logprobs\":{\"token\":\"A\"},\"choices\":[{\"message\":{\"content\":\"ok\"}}]}";
        String snippet = (String) invoke("extractLogprobSnippet", new Class<?>[]{String.class}, response);
        assertThat(snippet).contains("{").contains("token");
    }

    private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = SmartActorService.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}

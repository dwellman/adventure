package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class CommandTranslatorTest {

    @Test
    void translateReturnsNullWhenApiKeyMissing() throws Exception {
        String result = CommandTranslator.translate("", "prompt");
        assertThat(result).isNull();
    }

    @Test
    void jsonEscapeEscapesBackslashes() throws Exception {
        String escaped = (String) invoke("jsonEscape", new Class<?>[]{String.class}, "C:\\path");

        assertThat(escaped).isEqualTo("\"C:\\\\path\"");
    }

    @Test
    void extractJsonStringHandlesEscapes() throws Exception {
        String json = "{\"content\":\"Line1\\nLine2\"}";
        String content = (String) invoke("extractJsonString", new Class<?>[]{String.class, String.class}, json, "content");

        assertThat(content).isEqualTo("Line1\nLine2");
    }

    @Test
    void extractLogprobSnippetReturnsEmptyWhenMissing() throws Exception {
        String snippet = (String) invoke("extractLogprobSnippet", new Class<?>[]{String.class}, "{}");
        assertThat(snippet).isEqualTo("");
    }

    private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = CommandTranslator.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}

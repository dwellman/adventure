package com.demo.adventure.ai.client;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenAiChatClientTest {

    @Test
    void chatMessageDefaultsRoleAndContent() {
        AiChatMessage message = new AiChatMessage(null, null);

        assertThat(message.role()).isEqualTo(AiChatMessage.Role.USER);
        assertThat(message.content()).isEqualTo("");
    }

    @Test
    void chatResponseDefaultsLogprobSnippet() {
        AiChatResponse response = new AiChatResponse("ok", null);

        assertThat(response.logprobSnippet()).isEqualTo("");
    }

    @Test
    void requestBuilderCopiesMessages() {
        List<AiChatMessage> messages = new ArrayList<>();
        messages.add(AiChatMessage.user("hello"));

        AiChatRequest request = AiChatRequest.builder()
                .model("gpt-test")
                .messages(messages)
                .build();

        messages.add(AiChatMessage.user("later"));

        assertThat(request.messages()).hasSize(1);
    }

    @Test
    void chatReturnsEmptyWhenApiKeyMissing() throws Exception {
        OpenAiChatClient client = new OpenAiChatClient();
        AiChatRequest request = AiChatRequest.builder()
                .model("gpt-test")
                .messages(List.of(AiChatMessage.user("hello")))
                .build();

        AiChatResponse response = client.chat("", request);

        assertThat(response.content()).isNull();
        assertThat(response.logprobSnippet()).isEmpty();
    }

    @Test
    void chatRejectsNullRequest() {
        OpenAiChatClient client = new OpenAiChatClient();

        assertThatThrownBy(() -> client.chat("key", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request is required");
    }

    @Test
    void buildBodyIncludesLogprobsAndResponseFormat() throws Exception {
        AiChatRequest request = AiChatRequest.builder()
                .model("gpt-test")
                .messages(List.of(
                        new AiChatMessage(null, "hello"),
                        AiChatMessage.system("system")
                ))
                .temperature(0.7)
                .topP(0.9)
                .logprobs(true)
                .topLogprobs(4)
                .responseFormat(AiChatRequest.ResponseFormat.jsonObject())
                .build();

        String body = buildBody(request);

        assertThat(body).contains("\"logprobs\": true");
        assertThat(body).contains("\"top_logprobs\": 4");
        assertThat(body).contains("\"response_format\"");
        assertThat(body).contains("\"role\":\"user\"");
        assertThat(body).contains("\"role\":\"system\"");
    }

    private String buildBody(AiChatRequest request) throws Exception {
        try {
            Method method = OpenAiChatClient.class.getDeclaredMethod("buildBody", AiChatRequest.class);
            method.setAccessible(true);
            return (String) method.invoke(null, request);
        } catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof Exception causeException) {
                throw causeException;
            }
            throw ex;
        }
    }
}

package com.demo.adventure.ai.authoring;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthoringAgentClientTest {

    @Test
    void requestReturnsNullWhenApiKeyMissing() throws Exception {
        String result = AuthoringAgentClient.request("", "system", "user", false);
        assertThat(result).isNull();
    }
}

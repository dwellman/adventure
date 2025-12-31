package com.demo.adventure.ai.runtime.smart;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorServiceTest {

    @Test
    void requestReturnsNullWhenApiKeyMissing() throws Exception {
        String result = SmartActorService.request("", "system", "user", false);
        assertThat(result).isNull();
    }
}

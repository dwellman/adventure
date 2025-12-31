package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommandTranslatorTest {

    @Test
    void translateReturnsNullWhenApiKeyMissing() throws Exception {
        String result = CommandTranslator.translate("", "prompt");
        assertThat(result).isNull();
    }
}

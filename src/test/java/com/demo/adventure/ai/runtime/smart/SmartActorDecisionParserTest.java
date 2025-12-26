package com.demo.adventure.ai.runtime.smart;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmartActorDecisionParserTest {

    @Test
    void parsesUtteranceDecision() {
        String json = "{\"type\":\"UTTERANCE\",\"utterance\":\"look\",\"rule\":\"safe\"}";
        SmartActorDecisionParser.Result result = SmartActorDecisionParser.parse(json);

        assertThat(result.type()).isEqualTo(SmartActorDecisionParser.Result.Type.DECISION);
        assertThat(result.decision().type()).isEqualTo(SmartActorDecision.Type.UTTERANCE);
        assertThat(result.decision().utterance()).isEqualTo("look");
    }

    @Test
    void parsesColorDecision() {
        String json = "{\"type\":\"COLOR\",\"color\":\"A cold draft.\"}";
        SmartActorDecisionParser.Result result = SmartActorDecisionParser.parse(json);

        assertThat(result.type()).isEqualTo(SmartActorDecisionParser.Result.Type.DECISION);
        assertThat(result.decision().type()).isEqualTo(SmartActorDecision.Type.COLOR);
        assertThat(result.decision().color()).isEqualTo("A cold draft.");
    }

    @Test
    void rejectsMultilineOutput() {
        String json = "{\n\"type\":\"NONE\"}";
        SmartActorDecisionParser.Result result = SmartActorDecisionParser.parse(json);

        assertThat(result.type()).isEqualTo(SmartActorDecisionParser.Result.Type.ERROR);
        assertThat(result.error()).contains("multiple lines");
    }
}

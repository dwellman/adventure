package com.demo.adventure.ai.runtime.dm;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiDmAgentTest {

    @Test
    void narrateReturnsNullWhenClientMissing() throws Exception {
        AiDmAgent agent = new AiDmAgent(null);

        assertThat(agent.narrate(null)).isNull();
    }

    @Test
    void buildUserPromptIncludesSectionsWhenPresent() throws Exception {
        DmAgentContext context = new DmAgentContext(
                "Base line",
                "Target",
                "Target description",
                List.of("Fixture: desc"),
                List.of("Item: desc")
        );
        String prompt = (String) invoke("buildUserPrompt", new Class<?>[]{DmAgentContext.class}, context);

        assertThat(prompt).contains("Base:");
        assertThat(prompt).contains("Fixtures:");
        assertThat(prompt).contains("Contents:");
    }

    @Test
    void buildUserPromptOmitsEmptySections() throws Exception {
        DmAgentContext context = new DmAgentContext("Base", "Target", "", List.of(), List.of());
        String prompt = (String) invoke("buildUserPrompt", new Class<?>[]{DmAgentContext.class}, context);

        assertThat(prompt).contains("Base:");
        assertThat(prompt).doesNotContain("Fixtures:");
        assertThat(prompt).doesNotContain("Contents:");
    }

    private Object invoke(String name, Class<?>[] types, Object... args) throws Exception {
        Method method = AiDmAgent.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(null, args);
    }
}

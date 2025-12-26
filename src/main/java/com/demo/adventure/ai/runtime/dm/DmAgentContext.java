package com.demo.adventure.ai.runtime.dm;

import java.util.List;
import java.util.Objects;

/**
 * Structured inputs provided to a DM agent when rewriting narration.
 *
 * @param baseText          deterministic narration line (fallback)
 * @param targetLabel       label of the thing being described
 * @param targetDescription current description text for the target
 * @param fixtureSummaries  visible fixtures under the target (label + description)
 * @param inventorySummaries visible contents (label + description)
 */
public record DmAgentContext(
        String baseText,
        String targetLabel,
        String targetDescription,
        List<String> fixtureSummaries,
        List<String> inventorySummaries
) {
    public DmAgentContext {
        baseText = Objects.toString(baseText, "");
        fixtureSummaries = List.copyOf(fixtureSummaries == null ? List.of() : fixtureSummaries);
        inventorySummaries = List.copyOf(inventorySummaries == null ? List.of() : inventorySummaries);
    }
}

package com.demo.adventure.ai.runtime.dm;

/**
 * Optional AI narrator hook. Implementations may rewrite the base line of narration.
 * Callers must always provide a deterministic fallback when the agent is absent or fails.
 * Pattern: Trust UX - the deterministic line remains the authoritative default.
 */
public interface DmAgent {
    /**
     * Generate an alternate narration line for the given context.
     *
     * @param context narration context
     * @return rewritten narration text; return null or blank to use the base text instead
     * @throws Exception when the agent cannot produce a line
     */
    String narrate(DmAgentContext context) throws Exception;
}

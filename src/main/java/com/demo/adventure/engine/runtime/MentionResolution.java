package com.demo.adventure.engine.runtime;

import java.util.UUID;

public record MentionResolution(MentionResolutionType type, UUID actorId, String actorLabel, int tokensMatched) {
    public static MentionResolution none() {
        return new MentionResolution(MentionResolutionType.NONE, null, "", 0);
    }

    public static MentionResolution ambiguous() {
        return new MentionResolution(MentionResolutionType.AMBIGUOUS, null, "", 0);
    }
}

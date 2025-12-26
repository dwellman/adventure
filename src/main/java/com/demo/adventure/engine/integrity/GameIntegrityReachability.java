package com.demo.adventure.engine.integrity;

public record GameIntegrityReachability(
        boolean winFound,
        boolean searchExhausted,
        int statesVisited,
        int actionsEvaluated,
        int maxDepthReached
) {
}

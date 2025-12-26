package com.demo.adventure.engine.integrity;

import java.util.List;
import java.util.Objects;

public record GameIntegrityReport(
        String resourcePath,
        GameIntegrityReachability possibleWin,
        GameIntegrityReachability guaranteedWin,
        List<GameIntegrityIssue> issues
) {
    public GameIntegrityReport {
        resourcePath = resourcePath == null ? "" : resourcePath;
        possibleWin = Objects.requireNonNullElse(possibleWin, new GameIntegrityReachability(false, true, 0, 0, 0));
        guaranteedWin = Objects.requireNonNullElse(guaranteedWin, new GameIntegrityReachability(false, true, 0, 0, 0));
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean hasErrors() {
        return issues.stream().anyMatch(issue -> issue.severity() == GameIntegritySeverity.ERROR);
    }
}

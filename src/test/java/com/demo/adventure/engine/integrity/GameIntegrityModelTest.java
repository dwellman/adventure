package com.demo.adventure.engine.integrity;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GameIntegrityModelTest {

    @Test
    void constructsReportAndIssueRecords() {
        GameIntegrityReachability possible = new GameIntegrityReachability(true, false, 3, 7, 2);
        GameIntegrityReachability guaranteed = new GameIntegrityReachability(false, true, 1, 1, 1);
        GameIntegrityIssue issue = new GameIntegrityIssue(GameIntegritySeverity.WARNING, "W_TEST", "message", "context");
        GameIntegrityReport report = new GameIntegrityReport("path", possible, guaranteed, List.of(issue));

        assertThat(report.resourcePath()).isEqualTo("path");
        assertThat(report.possibleWin().winFound()).isTrue();
        assertThat(report.guaranteedWin().searchExhausted()).isTrue();
        assertThat(report.issues()).hasSize(1);
        assertThat(report.issues().get(0).code()).isEqualTo("W_TEST");
    }

    @Test
    void defaultsConfigValues() {
        GameIntegrityConfig config = GameIntegrityConfig.defaults();

        assertThat(config.maxDepth()).isGreaterThan(0);
        assertThat(config.maxStates()).isGreaterThan(0);
        assertThat(config.maxActionsPerState()).isGreaterThan(0);
    }
}

package com.demo.adventure.engine.integrity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IntegrityWinRequirementEvaluatorTest {

    @Test
    void addsErrorsWhenSearchExhausted() {
        List<Set<String>> requirements = List.of(Set.of("KEY"));
        GameIntegritySimulation.ReachabilityResult possible = new GameIntegritySimulation.ReachabilityResult(
                new GameIntegrityReachability(false, true, 0, 0, 0),
                Set.of(),
                new boolean[]{false}
        );
        List<GameIntegrityIssue> issues = new ArrayList<>();

        IntegrityWinRequirementEvaluator.evaluate(requirements, possible, issues);

        assertThat(issues).extracting(GameIntegrityIssue::code)
                .contains("E_REQUIRED_ITEM_UNREACHABLE", "E_REQUIRED_SET_UNSATISFIED");
        assertThat(issues).allMatch(issue -> issue.severity() == GameIntegritySeverity.ERROR);
    }

    @Test
    void addsWarningsWhenSearchNotExhausted() {
        List<Set<String>> requirements = List.of(Set.of("KEY"));
        GameIntegritySimulation.ReachabilityResult possible = new GameIntegritySimulation.ReachabilityResult(
                new GameIntegrityReachability(false, false, 0, 0, 0),
                Set.of(),
                new boolean[]{false}
        );
        List<GameIntegrityIssue> issues = new ArrayList<>();

        IntegrityWinRequirementEvaluator.evaluate(requirements, possible, issues);

        assertThat(issues).extracting(GameIntegrityIssue::code)
                .contains("E_REQUIRED_ITEM_UNREACHABLE", "E_REQUIRED_SET_UNSATISFIED");
        assertThat(issues).allMatch(issue -> issue.severity() == GameIntegritySeverity.WARNING);
    }
}

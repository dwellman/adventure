package com.demo.adventure.engine.integrity;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GameIntegrityCheckCoverageTest {

    @Test
    void evaluateFlagsIssuesForBrokenGame() throws Exception {
        GameIntegrityCheck check = new GameIntegrityCheck();
        GameIntegrityConfig config = new GameIntegrityConfig(2, 20, 5);

        GameIntegrityReport report = check.evaluate("src/test/resources/integrity/mini.yaml", config);
        List<String> codes = report.issues().stream().map(GameIntegrityIssue::code).toList();

        assertThat(codes).contains("E_TRIGGER_TARGET_MISSING", "E_TRIGGER_ACTION_TARGET_MISSING");
        assertThat(codes).contains("E_HIDDEN_NO_REVEAL");
        assertThat(codes).contains("E_KEYEXPR_PARSE");
        assertThat(codes).contains("E_KEYEXPR_REF");
        assertThat(codes).contains("E_CRAFTING_SKILL_MISSING");
    }

    @Test
    void utilityMethodsHandleLabelsAndTargets() throws Exception {
        String reversed = IntegrityLabels.reverseGateLabel("A -> B");
        String revealed = IntegrityLabels.resolveRevealTarget("@OBJECT", "Target", "Object");
        String normalized = IntegrityLabels.normalizeLabel("  Key  ");
        boolean special = IntegrityLabels.isSpecialTarget("@PLAYER");

        assertThat(reversed).isEqualTo("B -> A");
        assertThat(revealed).isEqualTo("OBJECT");
        assertThat(normalized).isEqualTo("KEY");
        assertThat(special).isTrue();
    }

    @Test
    void runReachabilityHandlesNullGame() throws Exception {
        GameIntegritySimulation.ReachabilityResult result = GameIntegritySimulation.runReachability(
                null,
                GameIntegrityConfig.defaults(),
                GameIntegritySimulation.DiceMode.MIN,
                List.of(Set.of())
        );

        GameIntegrityReachability summary = result.summary();
        boolean winFound = summary.winFound();
        boolean searchExhausted = summary.searchExhausted();

        assertThat(winFound).isFalse();
        assertThat(searchExhausted).isTrue();
    }

}

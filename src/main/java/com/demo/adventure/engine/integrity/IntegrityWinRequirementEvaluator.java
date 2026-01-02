package com.demo.adventure.engine.integrity;

import java.util.List;
import java.util.Set;

final class IntegrityWinRequirementEvaluator {
    private IntegrityWinRequirementEvaluator() {
    }

    static void evaluate(
            List<Set<String>> winRequirements,
            GameIntegritySimulation.ReachabilityResult possible,
            List<GameIntegrityIssue> issues
    ) {
        if (winRequirements == null || winRequirements.isEmpty() || possible == null || issues == null) {
            return;
        }
        boolean searchExhausted = possible.summary().searchExhausted();
        for (Set<String> required : winRequirements) {
            if (required == null || required.isEmpty()) {
                continue;
            }
            for (String label : required) {
                if (!possible.reachableItems().contains(label)) {
                    issues.add(new GameIntegrityIssue(
                            searchExhausted ? GameIntegritySeverity.ERROR : GameIntegritySeverity.WARNING,
                            "E_REQUIRED_ITEM_UNREACHABLE",
                            "Required item not reachable: " + label + ".",
                            label
                    ));
                }
            }
        }
        boolean[] satisfied = possible.requiredSatisfied();
        for (int i = 0; i < winRequirements.size(); i++) {
            Set<String> required = winRequirements.get(i);
            if (required == null || required.isEmpty()) {
                continue;
            }
            boolean ok = i < satisfied.length && satisfied[i];
            if (!ok) {
                issues.add(new GameIntegrityIssue(
                        searchExhausted ? GameIntegritySeverity.ERROR : GameIntegritySeverity.WARNING,
                        "E_REQUIRED_SET_UNSATISFIED",
                        "Required items never held together: " + String.join(", ", required) + ".",
                        String.join(", ", required)
                ));
            }
        }
    }
}

package com.demo.adventure.ai.runtime;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PromptEvaluationHarnessTest {

    @Test
    void comparisonEvaluationPrefersHigherScore() {
        TranslatorService service = new TranslatorService(true, "test");
        String baseline = buildPrompt(service);
        String degraded = baseline.replace("VISIBLE_ITEMS:", "MISSING_ITEMS:");

        PromptScorecard scorecard = new PromptScorecard(List.of(
                "VISIBLE_FIXTURES:",
                "VISIBLE_ITEMS:",
                "INVENTORY_ITEMS:",
                "PLAYER_TEXT:",
                "SCENE_CONTEXT",
                "Valid player commands"
        ));
        PromptEvaluationHarness harness = new PromptEvaluationHarness(scorecard);

        PromptEvaluationHarness.Result result = harness.compare("baseline", baseline, "degraded", degraded);
        assertThat(result.winnerId()).isEqualTo("baseline");
        assertThat(result.leftScore()).isGreaterThan(result.rightScore());
    }

    @Test
    void driftGuardFlagsMissingAnchors() {
        TranslatorService service = new TranslatorService(true, "test");
        String prompt = buildPrompt(service);

        PromptScorecard scorecard = new PromptScorecard(List.of(
                "VISIBLE_FIXTURES:",
                "VISIBLE_ITEMS:",
                "INVENTORY_ITEMS:",
                "PLAYER_TEXT:",
                "SCENE_CONTEXT",
                "Valid player commands"
        ));

        int score = scorecard.score(prompt);
        assertThat(score).isEqualTo(scorecard.requiredTokens().size());
    }

    private String buildPrompt(TranslatorService service) {
        try {
            Method m = TranslatorService.class.getDeclaredMethod(
                    "buildTranslatorPrompt",
                    String.class,
                    List.class,
                    List.class,
                    List.class,
                    String.class
            );
            m.setAccessible(true);
            return (String) m.invoke(
                    service,
                    "attack goblin",
                    List.of("Lantern"),
                    List.of("Stick"),
                    List.of("Coin"),
                    "Cave\nExits: north"
            );
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private record PromptScorecard(List<String> requiredTokens) {
        private int score(String prompt) {
            int totalScore = 0;
            for (String token : requiredTokens) {
                if (prompt.contains(token)) {
                    totalScore++;
                }
            }
            return totalScore;
        }
    }

    private record PromptEvaluationHarness(PromptScorecard scorecard) {
        private Result compare(String leftId, String leftPrompt, String rightId, String rightPrompt) {
            int leftScore = scorecard.score(leftPrompt);
            int rightScore = scorecard.score(rightPrompt);
            String winner = leftScore >= rightScore ? leftId : rightId;
            return new Result(winner, leftScore, rightScore);
        }

        private record Result(String winnerId, int leftScore, int rightScore) {
        }
    }
}

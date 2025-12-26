package com.demo.adventure.engine.integrity;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class GameIntegrityTest {

    @Test
    void integrityChecksAllStructuredGames() throws Exception {
        Assumptions.assumeTrue(shouldRunIntegrity(), "Integrity checks disabled (set -DrunIntegrity=true to enable).");
        GameIntegrityCheck check = new GameIntegrityCheck();
        GameIntegrityConfig config = GameIntegrityConfig.defaults();
        for (Path game : listGameYamls()) {
            GameIntegrityReport report = check.evaluate(game.toString(), config);
            List<GameIntegrityIssue> errors = report.issues().stream()
                    .filter(issue -> issue.severity() == GameIntegritySeverity.ERROR)
                    .toList();
            assertThat(errors)
                    .as("Integrity errors for %s: %s", game, summarize(errors))
                    .isEmpty();
            if (report.possibleWin().searchExhausted()) {
                assertThat(report.possibleWin().winFound())
                        .as("No reachable win for %s", game)
                        .isTrue();
            }
        }
    }

    private static List<Path> listGameYamls() throws Exception {
        Path root = Path.of("src/main/resources/games");
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase("game.yaml"))
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    private static String summarize(List<GameIntegrityIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return "";
        }
        return issues.stream()
                .map(issue -> issue.code() + ": " + issue.message())
                .collect(Collectors.joining(" | "));
    }

    private static boolean shouldRunIntegrity() {
        String flag = System.getProperty("runIntegrity");
        if (flag == null || flag.isBlank()) {
            flag = System.getenv("RUN_INTEGRITY");
        }
        return flag != null && flag.equalsIgnoreCase("true");
    }
}

package com.demo.adventure.authoring.cli;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GamePlanCliTest {

    @Test
    void buildInterviewPayloadDefaultsToEmptyCollections() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) invoke(cli,
                "buildInterviewPayload",
                new Class<?>[]{Map.class, String.class, List.class, List.class},
                null,
                null,
                null,
                null
        );

        assertThat(payload.get("constraints_brief")).isInstanceOf(Map.class);
        assertThat(payload.get("question_history")).isInstanceOf(List.class);
        assertThat(payload.get("guardrails")).isInstanceOf(List.class);
    }

    @Test
    void readConstraintsReturnsEmptyWhenMissing() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        Path temp = Files.createTempFile("constraints", ".yaml");
        Files.writeString(temp, "foo: bar\n");

        @SuppressWarnings("unchecked")
        Map<String, Object> constraints = (Map<String, Object>) invoke(cli,
                "readConstraints",
                new Class<?>[]{Path.class},
                temp
        );

        assertThat(constraints).isEmpty();
    }

    @Test
    void readConstraintsReturnsBriefWhenPresent() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        Path temp = Files.createTempFile("constraints", ".yaml");
        Files.writeString(temp, "constraints_brief:\n  title: Demo\n");

        @SuppressWarnings("unchecked")
        Map<String, Object> constraints = (Map<String, Object>) invoke(cli,
                "readConstraints",
                new Class<?>[]{Path.class},
                temp
        );

        assertThat(constraints).containsEntry("title", "Demo");
    }

    @Test
    void compileAndValidateReportsFailureForInvalidGdl() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        Path outDir = Files.createTempDirectory("gameplan");

        Object outcome = invoke(cli,
                "compileAndValidate",
                new Class<?>[]{Path.class, String.class},
                outDir,
                "BAD GDL"
        );

        boolean success = (boolean) invoke(outcome, "success", new Class<?>[]{});
        assertThat(success).isFalse();
        assertThat(Files.exists(outDir.resolve("build-report.yaml"))).isTrue();
    }

    @Test
    void compileAndValidateSucceedsForValidGdl() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        Path outDir = Files.createTempDirectory("gameplan");
        String gdl = Files.readString(Path.of("src/main/resources/games/gdl-demo/game.gdl"));

        Object outcome = invoke(cli,
                "compileAndValidate",
                new Class<?>[]{Path.class, String.class},
                outDir,
                gdl
        );

        boolean success = (boolean) invoke(outcome, "success", new Class<?>[]{});
        assertThat(success).isTrue();
        assertThat(Files.exists(outDir.resolve("build-report.yaml"))).isTrue();
    }

    @Test
    void runAgentStepWritesFailureReceiptOnEmptyResponse() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        Path outDir = Files.createTempDirectory("gameplan");
        Path outPath = outDir.resolve("story.yaml");

        Object result = invoke(cli,
                "runAgentStep",
                new Class<?>[]{String.class, boolean.class, String.class, Map.class, String.class, Path.class, String.class},
                "",
                false,
                "agents/storyteller.md",
                Map.of("constraints_brief", Map.of()),
                "story_spec",
                outPath,
                "story_spec"
        );

        assertThat(result).isNull();
        assertThat(Files.exists(outDir.resolve("receipt.yaml"))).isTrue();
    }

    @Test
    void writeReceiptAppendsSteps() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        Path outDir = Files.createTempDirectory("gameplan");

        invoke(cli, "writeReceipt", new Class<?>[]{Path.class, String.class, String.class, String.class}, outDir, "step1", "ok", "detail");
        invoke(cli, "writeReceipt", new Class<?>[]{Path.class, String.class, String.class, String.class}, outDir, "step2", "ok", "detail");

        String yaml = Files.readString(outDir.resolve("receipt.yaml"));
        assertThat(yaml).contains("step: step1");
        assertThat(yaml).contains("step: step2");
    }

    @Test
    void formatProblemsIncludesEntries() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        com.demo.adventure.authoring.save.build.WorldBuildReport report = new com.demo.adventure.authoring.save.build.WorldBuildReport();
        report.add(new com.demo.adventure.authoring.save.build.WorldBuildProblem("E_TEST", "message", "WORLD", null));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> problems = (List<Map<String, Object>>) invoke(cli, "formatProblems", new Class<?>[]{com.demo.adventure.authoring.save.build.WorldBuildReport.class}, report);

        assertThat(problems).hasSize(1);
        assertThat(problems.get(0)).containsEntry("code", "E_TEST");
    }

    @Test
    void loadYamlReturnsNullForInvalidInput() throws Exception {
        GamePlanCli cli = new GamePlanCli();

        Object loaded = invoke(cli, "loadYaml", new Class<?>[]{String.class}, ":::bad:::");

        assertThat(loaded).isNull();
    }

    @Test
    void listAtReturnsEmptyWhenNotList() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> steps = (List<Map<String, Object>>) invoke(cli, "listAt", new Class<?>[]{Map.class, String.class}, Map.of("steps", "nope"), "steps");

        assertThat(steps).isEmpty();
    }

    @Test
    void writeStructuredExportIncludesPaths() throws Exception {
        GamePlanCli cli = new GamePlanCli();
        Path outDir = Files.createTempDirectory("gameplan");

        invoke(cli,
                "writeStructuredExport",
                new Class<?>[]{Path.class, String.class, String.class, Map.class},
                outDir,
                "completed",
                "ok",
                Map.of("game_yaml", "path")
        );

        String yaml = Files.readString(outDir.resolve("structured-export.yaml"));
        assertThat(yaml).contains("game_yaml");
    }
    @Test
    void loadPromptThrowsWhenMissing() {
        GamePlanCli cli = new GamePlanCli();

        assertThatThrownBy(() -> invoke(cli, "loadPromptOrThrow", new Class<?>[]{String.class}, "missing.md"))
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    private Object invoke(Object target, String name, Class<?>[] types, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}

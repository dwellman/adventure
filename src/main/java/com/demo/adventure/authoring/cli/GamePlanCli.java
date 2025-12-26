package com.demo.adventure.authoring.cli;

import com.demo.adventure.ai.runtime.PromptTemplates;
import com.demo.adventure.ai.authoring.AuthoringAgentClient;
import com.demo.adventure.authoring.lang.gdl.GdlLoader;
import com.demo.adventure.authoring.save.build.GameSaveAssembler;
import com.demo.adventure.authoring.save.build.WorldBuildProblem;
import com.demo.adventure.authoring.save.build.WorldBuildReport;
import com.demo.adventure.authoring.save.build.WorldBuildResult;
import com.demo.adventure.authoring.save.io.GameSaveYamlWriter;
import com.demo.adventure.buui.BuuiConsole;
import com.demo.adventure.domain.save.GameSave;
import com.demo.adventure.support.exceptions.GameBuilderException;
import com.demo.adventure.support.exceptions.GdlCompileException;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * CLI pipeline for AI-assisted GamePlan authoring.
 */
public final class GamePlanCli extends BuuiConsole {
    private static final List<String> DEFAULT_GUARDRAILS = List.of(
            "do_not_rewrite_story",
            "no_ad_hoc_parsing",
            "use_command_scanner_compiler_interpreter"
    );

    public static void main(String[] args) throws Exception {
        String gameId = null;
        Path outRoot = Path.of("logs/gameplan");
        Path seedPath = null;
        boolean debug = false;
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--game", "-g" -> gameId = args[++i];
                case "--out", "-o" -> outRoot = Path.of(args[++i]);
                case "--seed", "-s" -> seedPath = Path.of(args[++i]);
                case "--debug" -> debug = true;
                case "--help", "-h" -> {
                    printHelp();
                    return;
                }
                default -> {
                    System.err.println("Unknown arg: " + args[i]);
                    printHelp();
                    return;
                }
            }
        }
        if (gameId == null || gameId.isBlank()) {
            printHelp();
            return;
        }
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("Missing OPENAI_API_KEY for GamePlan.");
            return;
        }
        new GamePlanCli().run(gameId.trim(), outRoot, seedPath, apiKey, debug);
    }

    private static void printHelp() {
        printText("GamePlan CLI");
        printText("Usage: mvn -q -Dexec.mainClass=com.demo.adventure.authoring.cli.GamePlanCli ");
        printText("  -Dexec.args=\"--game <id> [--out <dir>] [--seed <yaml>] [--debug]\" exec:java");
    }

    private void run(String gameId, Path outRoot, Path seedPath, String apiKey, boolean debug) throws IOException {
        Path outDir = outRoot.resolve(gameId);
        Files.createDirectories(outDir);

        Map<String, Object> constraints = seedPath == null ? new LinkedHashMap<>() : readConstraints(seedPath);
        List<String> questionHistory = new ArrayList<>();
        String lastAnswer = null;

        String constraintsPrompt = loadPromptOrThrow("agents/constraints-interviewer.md");
        Yaml yaml = yaml();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
            while (true) {
                String userPrompt = yaml.dump(buildInterviewPayload(constraints, lastAnswer, questionHistory, DEFAULT_GUARDRAILS));
                String response = AuthoringAgentClient.request(apiKey, constraintsPrompt, userPrompt, debug);
                Map<String, Object> result = loadYaml(response);
                Map<String, Object> interview = mapAt(result, "interview");
                if (interview == null) {
                    writeReceipt(outDir, "constraints_interview", "failed", "Missing interview output");
                    return;
                }
                constraints = mapAt(interview, "constraints_brief");
                if (constraints == null) {
                    writeReceipt(outDir, "constraints_interview", "failed", "Missing constraints_brief output");
                    return;
                }
                String status = stringAt(interview, "status");
                if ("done".equalsIgnoreCase(status)) {
                    break;
                }
                Map<String, Object> question = mapAt(interview, "question");
                if (question == null) {
                    writeReceipt(outDir, "constraints_interview", "failed", "Missing question output");
                    return;
                }
                String questionText = stringAt(question, "text");
                String questionId = stringAt(question, "id");
                if (questionText == null || questionText.isBlank()) {
                    writeReceipt(outDir, "constraints_interview", "failed", "Empty question text");
                    return;
                }
                print("Q" + (questionId == null ? "" : " " + questionId) + ": " + questionText);
                String answer = reader.readLine();
                if (answer == null) {
                    writeReceipt(outDir, "constraints_interview", "failed", "No answer provided");
                    return;
                }
                if ("q".equalsIgnoreCase(answer.trim()) || "quit".equalsIgnoreCase(answer.trim())) {
                    writeReceipt(outDir, "constraints_interview", "aborted", "User quit");
                    return;
                }
                lastAnswer = answer.trim();
                questionHistory.add(questionText + " => " + lastAnswer);
            }
        } catch (Exception ex) {
            writeReceipt(outDir, "constraints_interview", "failed", ex.getMessage());
            return;
        }

        Path constraintsPath = outDir.resolve("constraints-brief.yaml");
        writeYaml(constraintsPath, Map.of("constraints_brief", constraints));
        writeReceipt(outDir, "constraints_interview", "completed", constraintsPath.toString());

        Map<String, Object> storySpec = runAgentStep(
                apiKey,
                debug,
                "agents/storyteller.md",
                Map.of("constraints_brief", constraints),
                "story_spec",
                outDir.resolve("story-spec.yaml"),
                "story_spec"
        );
        if (storySpec == null) {
            return;
        }

        Map<String, Object> worldPlan = runAgentStep(
                apiKey,
                debug,
                "agents/world-planner.md",
                Map.of("constraints_brief", constraints, "story_spec", storySpec),
                "world_plan",
                outDir.resolve("world-plan.yaml"),
                "world_plan"
        );
        if (worldPlan == null) {
            return;
        }

        Map<String, Object> gdlSource = runAgentStep(
                apiKey,
                debug,
                "agents/gdl-synthesizer.md",
                Map.of("constraints_brief", constraints, "story_spec", storySpec, "world_plan", worldPlan),
                "gdl_source",
                outDir.resolve("gdl-source.yaml"),
                "gdl_source"
        );
        if (gdlSource == null) {
            return;
        }

        String gdlText = stringAt(gdlSource, "source");
        if (gdlText == null || gdlText.isBlank()) {
            writeBuildReport(outDir, "failed", "Missing gdl_source.source", List.of());
            writeReceipt(outDir, "compile_validate", "failed", "build-report.yaml");
            return;
        }
        Path gdlPath = outDir.resolve("game.gdl");
        Files.writeString(gdlPath, gdlText, StandardCharsets.UTF_8);

        BuildOutcome buildOutcome = compileAndValidate(outDir, gdlText);
        if (!buildOutcome.success()) {
            writeReceipt(outDir, "compile_validate", "failed", "build-report.yaml");
            return;
        }
        writeReceipt(outDir, "compile_validate", "completed", "build-report.yaml");

        String title = stringAt(constraints, "title");
        if (title == null || title.isBlank()) {
            writeStructuredExport(outDir, "failed", "Missing constraints_brief.title", Map.of());
            writeReceipt(outDir, "structured_export", "failed", "structured-export.yaml");
            return;
        }
        Path structuredDir = outDir.resolve("structured");
        try {
            GameStructExporter.export(buildOutcome.save(), gameId, title, null, structuredDir);
        } catch (Exception ex) {
            writeStructuredExport(outDir, "failed", ex.getMessage(), Map.of());
            writeReceipt(outDir, "structured_export", "failed", "structured-export.yaml");
            return;
        }
        String backstory = stringAt(storySpec, "backstory_summary");
        if (backstory != null && !backstory.isBlank()) {
            Path narrativeDir = structuredDir.resolve("narrative");
            Files.createDirectories(narrativeDir);
            Files.writeString(narrativeDir.resolve("backstory.md"), backstory.trim() + "\n", StandardCharsets.UTF_8);
        }
        Map<String, Object> exportPaths = new LinkedHashMap<>();
        exportPaths.put("game_yaml", structuredDir.resolve("game.yaml").toString());
        exportPaths.put("map", structuredDir.resolve("world/map.yaml").toString());
        exportPaths.put("fixtures", structuredDir.resolve("world/fixtures.yaml").toString());
        exportPaths.put("items", structuredDir.resolve("world/items.yaml").toString());
        exportPaths.put("actors", structuredDir.resolve("world/actors.yaml").toString());
        exportPaths.put("descriptions", structuredDir.resolve("narrative/descriptions.yaml").toString());
        exportPaths.put("backstory_md", structuredDir.resolve("narrative/backstory.md").toString());
        writeStructuredExport(outDir, "completed", "structured export complete", exportPaths);
        writeReceipt(outDir, "structured_export", "completed", "structured-export.yaml");

        writeYaml(outDir.resolve("verification.yaml"), Map.of(
                "verification", Map.of("status", "skipped", "reason", "tests not wired yet")
        ));
        writeYaml(outDir.resolve("iterate.yaml"), Map.of(
                "iterate", Map.of("status", "pending", "deltas", List.of(), "next_actions", List.of())
        ));

        writeReceipt(outDir, "runtime_verification", "skipped", "verification.yaml");
        writeReceipt(outDir, "iterate", "pending", "iterate.yaml");

        writePipelineReceipt(outDir, gameId);
        print("GamePlan outputs written to: " + outDir);
    }

    private Map<String, Object> runAgentStep(String apiKey,
                                             boolean debug,
                                             String promptPath,
                                             Map<String, Object> payload,
                                             String rootKey,
                                             Path outPath,
                                             String receiptKey) {
        String systemPrompt = loadPromptOrThrow(promptPath);
        String userPrompt = yaml().dump(payload);
        String response;
        try {
            response = AuthoringAgentClient.request(apiKey, systemPrompt, userPrompt, debug);
        } catch (Exception ex) {
            writeReceipt(outPath.getParent(), receiptKey, "failed", ex.getMessage());
            return null;
        }
        Map<String, Object> result = loadYaml(response);
        if (result == null || result.containsKey("error")) {
            writeReceipt(outPath.getParent(), receiptKey, "failed", "Agent returned error");
            return null;
        }
        Map<String, Object> section = mapAt(result, rootKey);
        if (section == null) {
            writeReceipt(outPath.getParent(), receiptKey, "failed", "Missing " + rootKey + " output");
            return null;
        }
        writeYaml(outPath, Map.of(rootKey, section));
        writeReceipt(outPath.getParent(), receiptKey, "completed", outPath.toString());
        return section;
    }

    private Map<String, Object> buildInterviewPayload(Map<String, Object> constraints,
                                                      String lastAnswer,
                                                      List<String> questionHistory,
                                                      List<String> guardrails) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("constraints_brief", constraints == null ? Map.of() : constraints);
        payload.put("last_answer", lastAnswer == null ? "" : lastAnswer);
        payload.put("question_history", questionHistory == null ? List.of() : questionHistory);
        payload.put("guardrails", guardrails == null ? List.of() : guardrails);
        return payload;
    }

    private Map<String, Object> readConstraints(Path seedPath) throws IOException {
        String raw = Files.readString(seedPath, StandardCharsets.UTF_8);
        Map<String, Object> loaded = loadYaml(raw);
        Map<String, Object> brief = mapAt(loaded, "constraints_brief");
        return brief == null ? new LinkedHashMap<>() : brief;
    }

    private void writePipelineReceipt(Path outDir, String gameId) {
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("game_id", gameId);
        receipt.put("created_at", Instant.now().toString());
        receipt.put("notes", "See step receipts in receipt.yaml");
        writeYaml(outDir.resolve("pipeline-receipt.yaml"), receipt);
    }

    private BuildOutcome compileAndValidate(Path outDir, String gdlText) {
        String compileStatus = "passed";
        String compileError = null;
        GameSave save;
        try {
            save = GdlLoader.load(gdlText);
        } catch (GdlCompileException ex) {
            compileStatus = "failed";
            compileError = ex.getMessage();
            writeBuildReport(outDir, compileStatus, compileError, List.of());
            return new BuildOutcome(null, false);
        } catch (Exception ex) {
            compileStatus = "failed";
            compileError = ex.getMessage();
            writeBuildReport(outDir, compileStatus, compileError, List.of());
            return new BuildOutcome(null, false);
        }

        try {
            GameSaveYamlWriter.write(save, outDir.resolve("game-save.yaml"));
        } catch (Exception ex) {
            // best-effort
        }

        WorldBuildReport report = null;
        boolean validationOk = true;
        try {
            WorldBuildResult result = new GameSaveAssembler().apply(save);
            report = result.report();
        } catch (GameBuilderException ex) {
            validationOk = false;
            report = ex.getReport();
            if (report == null) {
                report = new WorldBuildReport();
                report.add(new WorldBuildProblem("E_BUILD_FAILED", ex.getMessage(), "WORLD", null));
            }
        } catch (Exception ex) {
            validationOk = false;
            report = new WorldBuildReport();
            report.add(new WorldBuildProblem("E_BUILD_EXCEPTION", ex.getMessage(), "WORLD", null));
        }

        List<Map<String, Object>> problems = formatProblems(report);
        writeBuildReport(outDir, compileStatus, compileError, problems, validationOk);
        return new BuildOutcome(save, validationOk && problems.isEmpty());
    }

    private void writeBuildReport(Path outDir, String compileStatus, String compileError, List<Map<String, Object>> problems) {
        writeBuildReport(outDir, compileStatus, compileError, problems, false);
    }

    private void writeBuildReport(Path outDir,
                                  String compileStatus,
                                  String compileError,
                                  List<Map<String, Object>> problems,
                                  boolean validationOk) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("compile_status", compileStatus);
        if (compileError != null && !compileError.isBlank()) {
            report.put("compile_error", compileError);
        }
        report.put("validation_status", compileStatus.equals("passed") ? (validationOk ? "passed" : "failed") : "skipped");
        report.put("problems", problems == null ? List.of() : problems);
        writeYaml(outDir.resolve("build-report.yaml"), Map.of("build_report", report));
    }

    private void writeStructuredExport(Path outDir, String status, String detail, Map<String, Object> paths) {
        Map<String, Object> export = new LinkedHashMap<>();
        export.put("status", status);
        export.put("detail", detail);
        if (paths != null && !paths.isEmpty()) {
            export.put("paths", paths);
        }
        writeYaml(outDir.resolve("structured-export.yaml"), Map.of("structured_export", export));
    }

    private List<Map<String, Object>> formatProblems(WorldBuildReport report) {
        if (report == null || report.getProblems().isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> out = new ArrayList<>();
        for (WorldBuildProblem problem : report.getProblems()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("code", problem.code());
            entry.put("message", problem.message());
            entry.put("entityType", problem.entityType());
            entry.put("entityId", problem.entityId() == null ? "" : problem.entityId().toString());
            out.add(entry);
        }
        return out;
    }

    private record BuildOutcome(GameSave save, boolean success) {}

    private void writeReceipt(Path outDir, String step, String status, String details) {
        Path receiptPath = outDir.resolve("receipt.yaml");
        Map<String, Object> receipt = Files.exists(receiptPath) ? loadYaml(readFile(receiptPath)) : new LinkedHashMap<>();
        if (receipt == null) {
            receipt = new LinkedHashMap<>();
        }
        List<Map<String, Object>> steps = listAt(receipt, "steps");
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("step", step);
        entry.put("status", status);
        entry.put("details", details);
        steps.add(entry);
        receipt.put("steps", steps);
        writeYaml(receiptPath, receipt);
    }

    private String readFile(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            return "";
        }
    }

    private Map<String, Object> loadYaml(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            Object loaded = yaml().load(raw);
            if (loaded instanceof Map<?, ?> map) {
                Map<String, Object> out = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (!(entry.getKey() instanceof String key)) {
                        return null;
                    }
                    if (!isSafeKey(key)) {
                        return null;
                    }
                    out.put(key, entry.getValue());
                }
                return out;
            }
        } catch (Exception ignored) {
            return null;
        }
        return null;
    }

    private boolean isSafeKey(String key) {
        if (key == null) {
            return false;
        }
        String trimmed = key.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            if ((ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '_') {
                continue;
            }
            return false;
        }
        return true;
    }

    private Map<String, Object> mapAt(Map<String, Object> root, String key) {
        if (root == null) {
            return null;
        }
        Object value = root.get(key);
        if (value instanceof Map<?, ?> map) {
            return new LinkedHashMap<>((Map<String, Object>) map);
        }
        return null;
    }

    private List<Map<String, Object>> listAt(Map<String, Object> root, String key) {
        if (root == null) {
            return new ArrayList<>();
        }
        Object value = root.get(key);
        if (value instanceof List<?> list) {
            List<Map<String, Object>> out = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    out.add(new LinkedHashMap<>((Map<String, Object>) map));
                }
            }
            return out;
        }
        return new ArrayList<>();
    }

    private String stringAt(Map<String, Object> root, String key) {
        if (root == null) {
            return null;
        }
        Object value = root.get(key);
        return value == null ? null : value.toString();
    }

    private String loadPromptOrThrow(String resourcePath) {
        String prompt = PromptTemplates.load(resourcePath);
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalStateException("Missing prompt: " + resourcePath);
        }
        return prompt;
    }

    private Yaml yaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        return new Yaml(options);
    }

    private void writeYaml(Path path, Object data) {
        try {
            Files.createDirectories(path.getParent());
            String text = yaml().dump(data);
            Files.writeString(path, text, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            System.err.println("Failed to write " + path + ": " + ex.getMessage());
        }
    }

    private static String resolveApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("OPENAI_API_KEY");
        }
        return key;
    }
}

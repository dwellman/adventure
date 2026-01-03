# Journal

## 2026-01-02 — GDL demo + cookbook alignment
- Scope: Verified the game with a full test suite run.
- Tests: `mvn -q test`
- Scope: Verified Spy Adventure with a short walkabout and reran the structured load test.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`; `./adventure --mode=1980` (Spy Adventure walkabout)
- Scope: Verified Spy Adventure in AI mode with a short walkabout.
- Tests: `./adventure --mode=2025` (Spy Adventure walkabout)
- Scope: Verified 2025-mode translation using longer statements in Spy Adventure.
- Tests: `./adventure --mode=2025` (long statement walkabout)
- Scope: Verified Island Adventure in AI mode with long-statement pickup/craft/gated-move flow and updated translator prompt/tests.
- Tests: `./adventure --mode=2025` (Island Adventure walkabout); `mvn -q -Dtest=TranslatorPromptGoldenTest,TranslatorPromptCoverageTest test`
- Scope: Refined translator prompt to drop trailing words on pickup targets and re-verified long pick-up phrasing.
- Tests: `./adventure --mode=2025` (Island Adventure pickup phrasing walkabout)
- Scope: Moved the GDL demo into test resources and updated tests/docs to reference the new path.
- Tests: `mvn -q -Dtest=RuntimeLoaderTest,GdlCliSmokeTest,GamePlanCliTest test`
- Scope: Renamed cookbook YAMLs to the `gardened-<game>` pattern and updated docs/tests/examples.
- Tests: `mvn -q -Dtest=GameSaveYamlWriterTest,GameSaveAssemblerTest test`
- Scope: Modularized GameRuntime and split GameIntegrityCheck into helper classes with updated coverage tests.
- Tests: `mvn -q -Dtest=GameIntegrityCheckCoverageTest,GameIntegrityTest,GameRuntimeCoverageTest,InteractionStateTest,MentionResolutionTest test`
- Scope: Split GameRuntime navigation/trigger handling into helpers and delegated move/turn/reset flow.
- Tests: `mvn -q -Dtest=GameRuntimeCoverageTest,GameCliCombatErrorTest,RuntimeCombatEdgeCaseTest,SmartActorRuntimeTest,GameIntegrityCheckCoverageTest,IntegrityWinRequirementEvaluatorTest test`
- Scope: Added YAML game catalog + footprint rules, removed crafting fallbacks, and migrated Java sample games/tests to YAML loads.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,GameCliHelperTest,GameCliWalkaboutFlowTest,GameCliQuitFlowTest,CraftingTableFlowTest,LoopRuntimeTest,GameSaveAssemblerTest,IslandAdventureIntegrationTest,ClueMansionIntegrationTest,GardenerClueMansionProofTest test`
- Tests: `mvn -q test`; `printf "q\n" | ./adventure --mode=1980`
- Scope: Updated playbook harness + CLI crafting test to use catalog entry + YAML recipes.
- Scope: Redesigned docs navigation, removed ADR references, and corrected prompt/test path mentions across docs.
- Tests: Not run (docs cleanup).

## 2025-12-18 — BUJO setup and validation
- Scope: Added BUJO folders/log, AI config knobs, shared wrapper helper, structured load test.
- Tests: `mvn -q test`

## 2025-12-20 — Command compiler integration
- Scope: Added command AST/compiler/interpreter pipeline, wired GameCli parsing, added compiler tests, documented GDL/GEL/PIL spec, expanded GEL compiler/evaluator and tests, enforced no nested function calls in GEL, wired registry attribute resolution with strict/fallback policy, added GDL compiler scaffolding and tests.
- Tests: `mvn -q -Dtest=CommandCompilerTest test`; `mvn -q -Dtest=KeyExpressionScannerTest,KeyExpressionCompilerTest,KeyExpressionEvaluatorTest test`; `mvn -q -Dtest=GdlCompilerTest test`

## 2025-12-21 — GDL CLI wiring
- Scope: Wired .gdl loading into GameCli and GameBuilderCli with explicit extension/flag handling, updated classpath backstory resolution, documented quoted-expression rule in the DSL spec, added .gdl support to other CLIs and a GDL demo game entry, printed backstory at game start, added a GDL CLI smoke test, tuned the demo item label/start, implemented structured PIL parsing plus new OPEN/USE/PUT/INSPECT verbs with diagnostics and tests, normalized GO to MOVE at the scanner level with updated help/tests, added a verb alias registry, updated AI prompts to prefer MOVE, wired alias listing into CLI help, added a storybook YAML conversion for the GDL demo with scaffolding files, added a StorybookValidateCli to run GameSaveAssembler validation for storybook YAMLs, documented/added a smoke test for StorybookValidateCli, audited embedded content (world defs, generator templates, minis, fallback recipes) for relocation, externalized description/zone templates to `src/main/resources/storybook/shared` YAML with a shared loader, and removed heuristic fixture description fallback in favor of a no-op expander.
- Tests: `mvn -q -Dtest=GdlCompilerTest test`; `mvn -q -Dtest=GdlCliSmokeTest test`; `mvn -q -Dtest=CommandCompilerTest,CommandScannerTest test`; `mvn -q -Dtest=TranslatorServiceTest test`; `mvn -q -Dtest=VerbAliasesTest test`; manual playtest via GameCli (PIL verbs); `mvn -q -Dexec.mainClass=com.demo.adventure.authoring.cli.WorldFingerprintDump -Dexec.args="src/main/resources/storybook/gdl-demo/game.yaml" exec:java`; `jshell --class-path "target/classes:~/.m2/repository/org/yaml/snakeyaml/2.0/snakeyaml-2.0.jar"` (GameSaveAssembler validation); `java -cp "target/classes:$HOME/.m2/repository/org/yaml/snakeyaml/2.0/snakeyaml-2.0.jar" com.demo.adventure.authoring.cli.StorybookValidateCli src/main/resources/storybook/gdl-demo/game.yaml`; not run (StorybookValidateCliSmokeTest); not run (ZoneGraphBuilderTest); not run (Gardener/GardenerDump changes).
- Scope: Reorganized world package into model/build/io/gardener/samples subpackages and updated imports across main/test sources.
- Tests: `mvn -q test`
- Scope: Removed unused private helpers, aligned Thing.isOpen/isVisible with default key-expression resolvers, and adjusted gardener proof test to use an explicit expander.
- Tests: `mvn -q clean test`; `mvn -q test`
- Scope: Restored KeyExpressionScanner.readString for upcoming string parsing work.
- Tests: `mvn -q test`
- Scope: Repo-wide deep clean (housekeeping docs/process/logs, README/docs updates, stale TODO cleanup, reserved scanner helper noted).
- Tests: `mvn -q test`
- Scope: Docs cleanup (moved zone-input sample into src/test/resources/zone-demo, moved Island backstory into `src/main/resources/storybook/island` with narrative-only placeholders, added Key Expression syntax doc under docs/reference/design, updated references).
- Tests: Not run (docs/storybook refactor).
- Scope: Docs reorg (moved design docs into docs/reference/design, moved walkthrough into docs/howto/guides, kept docs root to README/journal), and converted prompt templates from .txt to .md with updated gardener expander path.
- Tests: Not run (docs/prompt refactor).
- Scope: Docs reorg (renamed docs/reference/architecture to docs/reference/design, moved index content to docs/index.md, simplified docs/readme.md).
- Tests: Not run (docs index/design refactor).
- Scope: Removed docs/archive (legacy docs) and updated index/README references.
- Tests: Not run (docs archive removal).
- Scope: Rewrote README with a comprehensive developer overview, quick start, and authoring/dynamics guide.
- Tests: Not run (README overhaul).

## 2025-12-22 — GDL CLI smoke coverage
- Scope: Added GDL CLI smoke coverage for GameStructExporter, WorldFingerprintDump, and WorldIdDump, plus shared output capture helpers; removed the GDL demo entry from the GameCli menu; moved storybook bundles under `src/main/resources/storybook` and switched shared config loading to classpath resources, with docs/tests updated; moved prompt markdown files into `src/main/resources/agents` and updated the gardener expander to load its system prompt from classpath resources; standardized markdown filenames to lowercase hyphenated names, shortened long design doc names, and shortened long prompt filenames with references updated.
- Tests: `mvn -q -Dtest=GdlCliSmokeTest test`; `mvn -q -Dtest=StorybookValidateCliSmokeTest,ZoneGraphBuilderTest test`; `mvn -q -Dtest=StorybookValidateCliSmokeTest test`
- Scope: Implemented cell maps (model/ops/receipts), wired DSL attribute access, and added YAML load/save support across structured + monolithic paths.
- Tests: `mvn -q -Dtest=CellOpsTest,KeyExpressionEvaluatorTest test`
- Scope: Wired SEARCH/USE/crafting to consume/transfer cells and added sample cells to island actors/items.
- Tests: `mvn -q -Dtest=CellOpsTest,KeyExpressionEvaluatorTest,CraftingTableFlowTest,GameCliCraftTest test`
- Scope: Added Combat v1 runtime/receipts + CLI attack/flee actions, extended item/actor recipes for weapon/armor/equipment fields, and added combat/command parsing tests.
- Tests: `mvn -q -Dtest=CraftingTableFlowTest,GameCliCraftTest,CombatEngineTest,CommandCompilerTest test`
- Scope: Routed item/actor recipe construction through builders across loaders, samples, and tests.
- Tests: `mvn -q -Dtest=CommandCompilerTest,CommandScannerTest,VerbAliasesTest,CombatEngineTest,GameMenuStructuredLoadTest test`
- Scope: Documented design patterns with usage guidelines and testing patterns.
- Tests: Not run (docs only).
- Scope: Linked design docs to the pattern testing strategies.
- Tests: Not run (docs only).
- Scope: Added TODO testing gaps list and updated translator/narrator prompts to align with CLI behavior.
- Tests: Not run (docs + prompt updates).
- Scope: Adjusted key-expression test helper to accept strict unknown-reference exceptions.
- Tests: `mvn -q -Dtest=KeyExpressionEvaluatorTest test`
- Scope: Aligned narrator prompt placeholders with the updated engine inputs.
- Tests: `mvn -q -Dtest=TranslatorPromptCoverageTest,TranslatorRoutingGoldenTest,NarratorPromptSelectionTest,NarratorFallbackTest test`
- Scope: Added combat receipt/invariant tests, CLI combat error-path coverage, prompt sanitization checks, and marked completed testing gaps.
- Tests: `mvn -q -Dtest=CombatEngineTest,GameCliCombatErrorTest,TranslatorPromptCoverageTest,NarratorPromptSelectionTest test`
- Scope: Replaced direct Item/Actor/Plot/Gate construction with builders and aligned tests to builder defaults/owner requirements.
- Tests: `mvn -q -Dtest=CraftingTableFlowTest,KeyExpressionEvaluatorTest,CellOpsTest,DmNarratorTest,MovementGateTest,WorldOwnershipValidationTest,OpenAiHttpFixtureDescriptionExpanderTest,GameCliCraftTest,SearchTest,LookTest,DescribeTest,MoveTest,InventoryPackingTest,OpenKernelTest,CloseTest,GameCliExitFormattingTest test`
- Scope: Added builder overloads for Thing references, relaxed PlotBuilder id requirement, and updated tests to pass owners as Things.
- Tests: `mvn -q -Dtest=CraftingTableFlowTest,KeyExpressionEvaluatorTest,CellOpsTest,DmNarratorTest,MovementGateTest,WorldOwnershipValidationTest,OpenAiHttpFixtureDescriptionExpanderTest,GameCliCraftTest,SearchTest,LookTest,DescribeTest,MoveTest,InventoryPackingTest,OpenKernelTest,CloseTest,GameCliExitFormattingTest,BuilderDefaultsTest test`
- Scope: Enforced non-null Thing ownership, defaulted plot owners to Miliarium, and attached gate owners to plot references.
- Tests: `mvn -q -Dtest=BuilderDefaultsTest,CraftingTableFlowTest,KeyExpressionEvaluatorTest,CellOpsTest,DmNarratorTest,MovementGateTest,WorldOwnershipValidationTest,OpenAiHttpFixtureDescriptionExpanderTest,GameCliCraftTest,SearchTest,LookTest,DescribeTest,MoveTest,InventoryPackingTest,OpenKernelTest,CloseTest,GameCliExitFormattingTest test`
- Scope: Swept builder usage to drop redundant defaults/UUIDs and use Thing references where available.
- Tests: `mvn -q -Dtest=BuilderDefaultsTest,WorldBuilderTest,WorldAssemblerMapOnlyTest,WorldOwnershipValidationTest,GameCliExitFormattingTest,GameCliCraftTest,KeyExpressionEvaluatorTest,SearchTest,LookTest,DescribeTest,MoveTest,InventoryPackingTest,OpenKernelTest,CloseTest,CellOpsTest,OpenAiHttpFixtureDescriptionExpanderTest,CraftingTableFlowTest test`
- Scope: Swept remaining builder calls to use Thing references and defaults, removed extra UUID imports, and cleaned owner assignments in tests.
- Tests: `mvn -q -Dtest=CombatEngineTest,GameCliCombatErrorTest,BuilderDefaultsTest,WorldBuilderTest,WorldAssemblerMapOnlyTest,WorldOwnershipValidationTest,GameCliExitFormattingTest,GameCliCraftTest,KeyExpressionEvaluatorTest,SearchTest,LookTest,DescribeTest,MoveTest,InventoryPackingTest,OpenKernelTest,CloseTest,CellOpsTest,OpenAiHttpFixtureDescriptionExpanderTest,CraftingTableFlowTest test`
- Scope: Full test suite run.
- Tests: `mvn -q test`
- Scope: Full test suite run (repeat).
- Tests: `mvn -q test`
- Scope: Wired TranslationOrchestrator into GameCli, added orchestration/grounding/learning/mutation tests, and added prompt golden/replay fixtures.
- Tests: `mvn -q -Dtest=TranslationOrchestratorTest,NarratorFallbackTest,TranslatorPromptGoldenTest,NarratorPromptGoldenTest,PromptEvaluationHarnessTest,CellMutationReceiptMutationTest,GameCliCombatErrorTest,GameCliExitFormattingTest,GameCliCraftTest test`
- Scope: Added full-name direction aliases and updated orchestration test helpers for full-name directions + non-abbreviated variable names.
- Tests: `mvn -q -Dtest=TranslationOrchestratorTest,PromptEvaluationHarnessTest,NarratorPromptGoldenTest,TranslatorPromptGoldenTest test`
- Scope: Restored classic fallback behind scanner-only logic, expanded direction/preposition scanning, and aligned RUN AWAY handling plus new fallback/scanner/compiler tests.
- Tests: `mvn -q -Dtest=CommandScannerTest,CommandCompilerTest,ClassicCommandFallbackTest test`
- Scope: Updated verb alias tests to align RUN canonicalization with MOVE.
- Tests: `mvn -q -Dtest=VerbAliasesTest,CommandScannerTest,CommandCompilerTest,ClassicCommandFallbackTest test`
- Scope: Added regression test for RUN + direction mapping to GO in the command compiler.
- Tests: `mvn -q -Dtest=CommandCompilerTest test`
- Scope: Added scanner regression test for RUN + direction tokenization.
- Tests: `mvn -q -Dtest=CommandScannerTest test`
- Scope: Documented the AI-enabled runbook, updated the docs index, and added a temporary root TODO for tomorrow.
- Tests: Not run (docs/todo only).
- Scope: Added a verification checklist to the AI-enabled runbook.
- Tests: Not run (docs only).
- Scope: Mapped the runbook verification checklist to grounding/orchestration/verification/trust UX/learning pattern tests.
- Tests: Not run (docs only).
- Scope: Added a pattern coverage matrix to the pattern tests doc.
- Tests: Not run (docs only).

## 2025-12-23 — AI runbook/doc alignment
- Scope: Updated AI runbook/CLI docs to match GameCli/prompt paths and runtime contracts, added a skill doc plus today/tomorrow/someday list, removed the temporary root TODO, aligned the translator prompt with scanner/command-compiler command shapes, loaded per-game crafting recipes in GameCli, replaced manual HOW CRAFT parsing with scanner tokens, printed crafting summaries directly in AI mode to preserve list output, and removed tracked target artifacts after adding target/ to .gitignore.
- Tests: `mvn -q -Dtest=TranslatorPromptGoldenTest,TranslatorPromptCoverageTest,TranslatorRoutingGoldenTest,PromptEvaluationHarnessTest,GameCliCraftTest,CraftingTableFlowTest test`; `printf "1\nlook\ninventory\ncraft\nhow craft torch\nquit\n" | ./adventure --mode=1980 --quiet`; `OPENAI_API_KEY=(from ../../.secret) ./adventure --mode=2025 --quiet` (scripted); `mvn -q clean test`.
- Scope: Reprioritized the BUJO today list around Island Adventure 10/10 kernel mechanics based on the story backstory.
- Tests: Not run (planning docs only).
- Scope: Updated BUJO schedule to focus Island today, Mansion/Western/Spy tomorrow, and My Adventure someday; added per-game alias table work to today.
- Tests: Not run (planning docs only).
- Scope: Added loop runtime infrastructure (world clock + reset + persistent item snapshotting), a loop config loader, and an Island `loop.yaml` to enable 24-hour resets with notebook persistence.
- Tests: `mvn -q -Dtest=LoopRuntimeTest test`.
- Scope: Added trigger YAML loader + engine, wired ON_TAKE/ON_ENTER/ON_TURN/ON_USE into GameCli with reset support, and added trigger action coverage test.
- Tests: `mvn -q -Dtest=TriggerEngineTest test`.
- Scope: Added Island Time Stone trigger set (green flash reset + watch description + tick-rate curse), introduced `WorldState.TICK_RATE`, and synced loop tick rate from world state.
- Tests: `mvn -q -Dtest=LoopRuntimeTest,TriggerEngineTest,GameMenuStructuredLoadTest test`.
- Scope: Implemented Monkey Grove banana gate triggers, set iPad/banana footprints for the carry tradeoff, and moved starter kit ownership to the castaway.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`.
- Scope: Added treehouse skeleton use trigger to reveal the hatchet and Scratch's ghost, with a fixture cell to avoid no-op use messaging.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`.
- Scope: Added cave web burn state tracking, moved the angry spiders to the cave mouth after burn, and enforced return-death logic.
- Tests: `mvn -q -Dtest=LoopRuntimeTest,GameMenuStructuredLoadTest test`.
- Scope: Added plane wreck turn counter triggers with warning/death rules and collapse-on-exit gate teardown.
- Tests: `mvn -q -Dtest=LoopRuntimeTest,GameMenuStructuredLoadTest test`.
- Scope: Added raft assembly items/recipe, bamboo harvest trigger, scratch-freed tracking, and END_GAME escape triggers with the green-flash ending.
- Tests: `mvn -q -Dtest=TriggerEngineTest,LoopRuntimeTest,GameMenuStructuredLoadTest test`.
- Scope: Added per-game verb alias YAML loader and CommandScanner/CommandCompiler wiring with CLI help merging, plus Island aliases, loader tests, and doc updates.
- Tests: `mvn -q -Dtest=CommandCompilerTest,CommandScannerTest,VerbAliasLoaderTest,GameMenuStructuredLoadTest test`.
- Scope: Aligned AI runbook/CLI docs with GameCli flow and agent prompt placement, updated the AI runbook checklist and skill doc, refreshed AI CLI docs, fixed the docs index backstory path, and updated the today/tomorrow/someday list.
- Tests: Not run (docs only).
- Scope: Defined the smart actor architecture (persona, memory, policies, engine boundary) and linked it from design docs.
- Tests: Not run (docs only).
- Scope: Added Mansion smart-actor roster data, generic smart actor prompt scaffolding, and new suspect actors for the whodunit setup.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Scope: Added smart-actor backstory + RAG history metadata in the design spec and Mansion roster; updated the generic smart-actor prompt inputs.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Scope: Defined the smart-actor RAG retrieval contract and context injection format.
- Tests: Not run (docs only).
- Scope: Checked in the user-provided Island backstory edit and added game art assets.
- Tests: Not run (assets/content only).
- Scope: Implemented smart-actor RAG store + loader with deterministic retrieval and unit tests.
- Tests: `mvn -q -Dtest=SmartActorHistoryStoreTest,SmartActorSpecLoaderTest test`
- Scope: Implemented SmartActorContextBuilder for explicit tag merging + history snippet retrieval with tests.
- Tests: `mvn -q -Dtest=SmartActorContextBuilderTest test`
- Scope: Added explicit tag sources (`tags.yaml`) plus tag loader/index and context input builder for smart-actor RAG.
- Tests: `mvn -q -Dtest=SmartActorTagLoaderTest,SmartActorContextInputBuilderTest test`
- Scope: Relaxed smart-actor tag tests to avoid ordering assumptions; ran a clean test suite.
- Tests: `mvn -q test`
- Scope: Simplified 2025 translator flow to local-parse-first, command-only JSON; removed question/color handling; updated translator prompt/goldens, orchestrator tests, and AI docs/pattern index.
- Tests: `mvn -q -Dtest=TranslatorServiceTest,TranslationOrchestratorTest,TranslatorPromptGoldenTest,TranslatorPromptCoverageTest,PromptEvaluationHarnessTest,TranslatorRoutingGoldenTest test`
- Scope: Simplified translator output to a single command line (no JSON), removed style overrides from narrator prompting, and switched OpenAiNarrator to Spring AI; updated prompt goldens and docs accordingly.
- Tests: `mvn -q -Dtest=TranslatorServiceTest,TranslationOrchestratorTest,TranslatorPromptGoldenTest,TranslatorPromptCoverageTest,PromptEvaluationHarnessTest,TranslatorRoutingGoldenTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest,NarratorFallbackTest test`
- Scope: Moved mini games into YAML fixtures with backstory/crafting files and added script-driven CLI integration tests for the mini game flows.
- Tests: `mvn -q -Dtest=MiniGameScriptIntegrationTest test`; `mvn -q test`
- Scope: Added mini-game playbook YAML/MD files, switched the integration test to per-step playbook validation, and removed the old script fixtures.
- Tests: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest test`
- Scope: Added a BUUI ConsolePrinter utility to wrap text before the screen edge using the COLUMNS width hint.
- Tests: `mvn -q -Dtest=ConsolePrinterTest test`
- Scope: Routed GameCli/NarrationService output through ConsolePrinter for word-wrapped display while keeping table renders raw.
- Tests: `mvn -q -Dtest=ConsolePrinterTest,GameCliExitFormattingTest,MiniGamePlaybookIntegrationTest test`
- Scope: Added BuuiConsole helpers and refactored CLI tool output to use print/printRaw wrappers around ConsolePrinter.
- Tests: `mvn -q -Dtest=ConsolePrinterTest,MiniGamePlaybookIntegrationTest test`
- Scope: Added BUUI markdown rendering for tables/lists/headings and routed `print` to detect markdown output.
- Tests: `mvn -q -Dtest=ConsolePrinterTest,MarkdownRendererTest test`
- Scope: Added BuuiMenu for standardized CLI menu tables and wired GameCli menu rendering to it.
- Tests: `mvn -q -Dtest=BuuiMenuTest test`
- Scope: Added BuuiMenu prompt helper to standardize CLI menu prompts for game/mini selection.
- Tests: `mvn -q -Dtest=BuuiMenuTest test`
- Scope: Applied novel-style BUUI markdown formatting (paragraph indent, centered headings, scene breaks, bullet dots).
- Tests: `mvn -q -Dtest=ConsolePrinterTest,MarkdownRendererTest test`
- Scope: Added blank line insertion after markdown paragraphs for improved block spacing.
- Tests: `mvn -q -Dtest=MarkdownRendererTest test`
- Scope: Added a left gutter to response text via ConsolePrinter output.
- Tests: `mvn -q -Dtest=ConsolePrinterTest,MarkdownRendererTest test`
- Scope: Updated narrator fallback test expectations to include the response gutter.
- Tests: `mvn -q -Dtest=NarratorFallbackTest test`
- Scope: Added a blank line after the game header banner before narrative output.
- Tests: `mvn -q -Dtest=GameCliExitFormattingTest test`
- Scope: Reduced repeated scene narration by passing a minimal scene header for action outputs without exits.
- Tests: Not run (AI output shaping only).
- Scope: Added SCENE_DETAIL_LEVEL to narrator prompts to suppress extra description when only location + exits are available.
- Tests: `mvn -q -Dtest=NarratorPromptGoldenTest,NarratorPromptSelectionTest test`
- Scope: Removed the mini-game menu/loader from GameCli so mini games run only in tests.
- Tests: Not run (code change only).
- Scope: Added per-command handler classes and a dispatcher to organize CLI command logic.
- Tests: Not run (refactor only).
- Scope: Split CLI packages into runtime/command/ai, moved GameCli + AI services, extracted CommandContext/UseResult, regrouped command parsing under CommandInterpreter, and updated tests/docs/scripts for new paths.
- Tests: `mvn -q test`
- Scope: Extracted GameRuntime/SceneNarrator/RuntimeLoader, moved stateful command logic out of GameCli, and updated CLI tests to use runtime helpers.
- Tests: `mvn -q test`
- Scope: Reorganized packages under engine/domain/authoring/ai (engine command/flow/mechanics/session/cli, authoring save/lang/zone/gardener), updated imports/tests/docs/scripts.
- Tests: `mvn -q test`
- Scope: Moved runtime prompt templates into `src/main/resources/agents`, updated prompt loader paths, and refreshed docs to match.
- Tests: `mvn -q -Dtest=TranslatorPromptCoverageTest,TranslatorPromptGoldenTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest test`
- Scope: Removed empty legacy package directories from `src/main/java` and `src/test/java` after reorg.
- Tests: Not run (directory cleanup only).
- Scope: Moved `ZoneGraphBuilderTest` into `authoring/zone` to match the production package layout.
- Tests: Not run (test relocation only).
- Scope: Moved mini-game playbooks into `src/test/resources/minigames` and updated integration test paths.
- Tests: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest test`
- Scope: Removed the stale `authoring/builder` test path after relocating ZoneGraphBuilderTest.
- Tests: Not run (path cleanup only).
- Scope: Renamed OpenAi translator/narrator classes to `CommandTranslator` and `NarratorService`.
- Tests: `mvn -q -Dtest=TranslationOrchestratorTest,NarratorFallbackTest test`
- Scope: Documented the AI-assisted GDL design protocol with role-based inputs/process/outputs.
- Tests: Not run (doc update only).
- Scope: Added the Western Adventure GamePlan template with step-by-step inputs/process/outputs and updated the docs index.
- Tests: Not run (doc update only).
- Scope: Added timebound Western attributes and one-question interview guidance to the GamePlan protocol/template.
- Tests: Not run (doc update only).
- Scope: Moved the GamePlan plan into the Today list for 2025-12-24.
- Tests: Not run (planning update only).
- Scope: Added GamePlan role contracts/output schemas and new prompts for constraints/storyteller/world planner/GDL synthesis.
- Tests: Not run (docs/prompts only).
- Scope: Added GamePlanCli orchestration with receipts, authoring agent client, and CLI docs/index updates.
- Tests: `mvn -q -DskipTests compile`
- Scope: Wired GamePlanCli compile/validate + structured export and aligned GamePlan prompts/docs/templates to the updated schema.
- Tests: `mvn -q -DskipTests compile`
- Scope: Added Western Adventure playbook fixtures + integration test coverage using the structured Western bundle.
- Tests: `mvn -q -Dtest=WesternAdventurePlaybookIntegrationTest test`
- Scope: Renamed the Vibe Builder authoring component to GamePlan across CLI/docs/prompts/templates.
- Tests: `mvn -q -DskipTests compile`
- Scope: Expanded Western Adventure structure (plots/items/fixtures/actor) and added loop/triggers for time pressure and win/lose states.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Scope: Tuned Western inventory/trigger markers (starter saddlebag + bridge signal token) and refreshed the Western playbook win path.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,WesternAdventurePlaybookIntegrationTest test`
- Scope: Expanded Western Adventure map to 16 plots and added fixtures for the new locations.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,WesternAdventurePlaybookIntegrationTest test`
- Scope: Expanded Island Adventure map to 16 plots with new side locations and gates.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Scope: Added Island side-clue fixtures/items and ON_USE triggers for the new plots; updated Western GamePlan doc to reflect the 16-plot layout.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Scope: Added an Island Adventure side-clue playbook (YAML + MD) and integration test coverage.
- Tests: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest test`
- Scope: Seeded Mansion corridor plots with clue fixtures/items and updated tags.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Scope: Added Spy Adventure fixtures/items/actors, smart-actor specs, and tags for the large map.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Scope: Gated world build BOM output behind an opt-in system property to avoid CLI debug output.
- Tests: `mvn -q -Dtest=WorldAssemblerPrintsBomOnSuccessTest test`
- Scope: Refreshed player inventory from the registry after trigger outcomes to reflect moved/consumed items.
- Tests: `mvn -q -Dtest=TriggerEngineTest,GameCliCraftTest test`
- Scope: Added Mansion/Spy win triggers and wired their trigger YAML into the structured game bundles.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Scope: Allowed taking/inspecting items stored in open fixtures at the current plot and surfaced them in visible item lists.
- Tests: `mvn -q -Dtest=GameRuntimeTakeFixtureItemTest test`
- Scope: Added a Mansion Adventure playbook (YAML/MD) and integration test for the win path; ensured GameCli suppresses key-expression debug output when constructed directly.
- Tests: `mvn -q -Dtest=MansionAdventurePlaybookIntegrationTest test`
- Scope: Added a mansion case-board pinning flow (case board fixture, clue evidence cells, detective tracking) and updated triggers/playbook for the pin-to-win sequence.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,MansionAdventurePlaybookIntegrationTest test`
- Scope: Simplified Mansion pin-to-win flow by relying on case-board evidence cell transfers and removing manual clue counters.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,MansionAdventurePlaybookIntegrationTest test`
- Scope: Ran a manual 1980 Mansion playthrough to validate the case-board pinning flow end-to-end.
- Tests: `printf '2\nlook\nwest\nwest\ntake basement key\nsouth\ntake ink note\nsouth\nsouth\ntake pressed leaf\nsouth\neast\ntake glass shard\neast\nnorth\nnorth\ndown\ntake cellar ledger\nup\neast\ntake chalk smudge\neast\nsouth\ntake torn menu\nnorth\nnorth\nnorth\nwest\ntake silk thread\nwest\nuse torn menu on case board\nuse glass shard on case board\nuse chalk smudge on case board\nuse pressed leaf on case board\nuse ink note on case board\nuse cellar ledger on case board\nuse silk thread on case board\nquit\n' | ./adventure --mode=1980 --quiet`
- Scope: Ran a manual 1980 Mansion pass with alternate verbs/aliases and confirmed the pin-to-win sequence; verified that EXAMINE is not recognized without a Mansion alias file.
- Tests:   ~ AI disabled (mode=Z1980, apiKey=missing)
┌───────────────────────────────────────────────────────────────────┐
│                       BUUI Adventure (1980)                       │
├─────┬───────────────────┬─────────────────────────────────────────┤
│   # │ Game              │ Description                             │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   1 │ Island Adventure  │ CASTAWAY THRILLS. Jungle traps,         │
│     │                   │ cliffside caves, and a desperate        │
│     │                   │ raft-for-freedom dash before the        │
│     │                   │ adventure claims you for good.          │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   2 │ Mansion Adventure │ THE MYSTERY HOUSE. Secret doors.        │
│     │                   │ Creaking halls. One wrong turn and the  │
│     │                   │ mansion keeps you forever.              │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   3 │ Western Adventure │ TERROR AT DEAD-MAN’S GULCH. TNT on the  │
│     │                   │ rails. The Iron Rattler thundering in.  │
│     │                   │ One last chance at Rattlesnake Bridge.  │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   4 │ Spy Adventure     │ TRAUMA-BOND, INTERNATIONAL MAN OF       │
│     │                   │ MAYHEM. Glamour, gadgets, and a race to │
│     │                   │ disarm the WOPR before it lights the    │
│     │                   │ world up.                               │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   q │ Quit              │                                         │
└─────┴───────────────────┴─────────────────────────────────────────┘

Select a game (1-4, or q to quit): 
  === Mansion Adventure ===

  You are the detective in the Hall.
  You chased the Coldwater killer for a decade; every witness ended up
  swallowed by the same mansion. Tonight the owner invited you in, promising
  the house will “talk” if you listen.
  Rain hammers boarded windows, power flickers, and the front door locked
  itself behind you. The floorplan shifts like it resents your footprints. You
  have one night to pry the truth out before the house keeps you, too.

_ 
  You focus on the hall. It stands out just as described.

_ 
      Hall The Hall stretches ahead, adorned with shadows that dance playfully
  along the walls, whispering secrets of the past and inviting curious
  souls to explore further. Each step echoes with the promise of mysteries
  waiting to be uncovered, leading the way to the next adventure beyond
  this enchanting threshold to: Fixtures:

  • Case Board
      Exits: EAST, SOUTH, WEST

_ 
      Corridor Study-Hall The Corridor Study-Hall whispers secrets of the
  past, with each creaking floorboard inviting curious souls to wander
  deeper into its shadowy recesses. Fixtures:

  • Corridor Umbrella Stand
      Exits: EAST, WEST

_ 
  You poke around and stir up dust, but find nothing useful.

_ 
      Study The Study beckons with its antique writing desk, where shadows
  dance playfully among the scattered papers, hinting at secrets waiting
  to be uncovered. Fixtures:

  • Study Desk
  • Study Desk Drawer 1
      Items:

  • Revolver
  • Basement Key
      Exits: EAST, SOUTH, WEST

_ 
  Study Desk: This antique writing desk, with its intricate carvings and a
  hint of dust, whispers secrets of the past, inviting curious minds to
  uncover its hidden treasures.

_ 
  It's already open.

_ 
  You take the Basement Key.

_ 
  You are carrying:
  • Pocket Watch
  • Basement Key

_ 
      Corridor Study-Library The Corridor Study-Library stretches before you,
  lined with dusty tomes and curious artifacts, whispering secrets of
  forgotten tales and hidden mysteries yet to be uncovered. Fixtures:

  • Corridor Notice Board
      Items:

  • Ink Note
      Exits: NORTH, SOUTH

_ 
  You take the Ink Note.

_ 
      Library The Library looms with an air of mystery, where towering shelves
  whisper secrets of the past and the flicker of candlelight dances across
  worn pages, inviting curious minds to explore its hidden tales. You see:

  • Professor Hale
      Exits: EAST, NORTH, SOUTH, WEST

_ 
  You scour the area; tracks point EAST.

_ 
      Corridor Library-Conservatory The Corridor Library-Conservatory is a
  curious blend of knowledge and nature, where dusty tomes whisper secrets
  beneath the soft rustle of leaves. Dim light filters through tall
  windows, casting playful shadows across the rich wooden shelves,
  inviting explorers to uncover the mysteries hidden within. Fixtures:

  • Corridor Plant Stand
      Items:

  • Pressed Leaf
      Exits: EAST, NORTH, SOUTH

_ 
  You take the Pressed Leaf.

_ 
      Conservatory The Conservatory is a whimsical escape, filled with curious
  plants and hidden corners, where every leaf could hold a secret waiting
  to be discovered. You see:

  • Mara Thorn
      Exits: EAST, NORTH, WEST

_ 
      Corridor Conservatory-Ballroom The Corridor Conservatory-Ballroom is a
  whimsical fusion of nature and elegance, where vibrant plants peek
  through ornate windows, and the soft sound of music seems to dance
  through the air. Each step reveals hidden secrets of a grand past,
  inviting explorers to uncover the stories woven into its enchanting
  corners. Fixtures:

  • Corridor Frosted Window
      Items:

  • Glass Shard
      Exits: EAST, WEST

_ 
  You take the Glass Shard.

_ 
      Ballroom The grand Ballroom, with its sparkling chandeliers and polished
  floors, echoes with the whispers of a thousand unseen guests, each
  hinting at secrets yet to be unveiled. Items:

  • Wrench
      You see:

  • Vivian Coldwater
      Exits: EAST, NORTH, WEST

_ 
      Corridor Billiard-Ballroom The Corridor Billiard-Ballroom beckons with
  its inviting atmosphere, where the sound of clinking balls and laughter
  echoes softly. Shadows dance playfully across the walls, hinting at
  secrets waiting to be uncovered as one ventures deeper into the
  mysterious whims of the mansion. Fixtures:

  • Corridor Mirror
      Exits: NORTH, SOUTH

_ 
      Billiard Room The Billiard Room, a whimsical space adorned with antique
  cues and a vibrant green felt table, invites curious souls to unravel
  its secrets, while the shadows seem to whisper tales of the past. Items:

  • Lead Pipe
      Exits: DOWN, EAST, NORTH, SOUTH, WEST

_ 
  It's already open.

_ 
      Cellar The Cellar whispers secrets from the shadows, its damp air
  carrying the scent of aged wood and forgotten treasures, inviting
  curious souls to uncover what lies beneath. Fixtures:

  • Cellar Wine Rack
      Items:

  • Cellar Ledger
      Exits: EAST, UP

_ 
  You take the Cellar Ledger.

_ 
      Billiard Room The Billiard Room, a whimsical space adorned with antique
  cues and a vibrant green felt table, invites curious souls to unravel
  its secrets, while the shadows seem to whisper tales of the past. Items:

  • Lead Pipe
      Exits: DOWN, EAST, NORTH, SOUTH, WEST

_ 
      Corridor Billiard-Dining The Corridor Billiard-Dining is a curious blend
  of leisure and mystery, where the tables are set for games of chance and
  the walls whisper secrets of past gatherings. Shadows dance playfully
  along the edges, inviting explorers to uncover the tales hidden within
  this whimsical space. Fixtures:

  • Corridor Cue Rack
      Items:

  • Chalk Smudge
      Exits: EAST, WEST

_ 
  You take the Chalk Smudge.

_ 
      Dining Room A grand Dining Room awaits, its long table set for a feast
  forgotten, with dusty plates and a flickering candlestick casting
  playful shadows that dance upon the walls. Items:

  • Candlestick
      You see:

  • Elias Crane
      Exits: NORTH, SOUTH, WEST

_ 
      Corridor Dining-Kitchen The Corridor Dining-Kitchen whispers of past
  feasts, where the clatter of dishes and flicker of candlelight linger
  like a playful ghost, inviting the curious to explore its hidden
  secrets. Fixtures:

  • Corridor Pass Shelf
      Items:

  • Torn Menu
      Exits: EAST, NORTH, SOUTH

_ 
  You take the Torn Menu.

_ 
      Dining Room A grand Dining Room awaits, its long table set for a feast
  forgotten, with dusty plates and a flickering candlestick casting
  playful shadows that dance upon the walls. Items:

  • Candlestick
      You see:

  • Elias Crane
      Exits: NORTH, SOUTH, WEST

_ 
      Corridor Lounge-Dining The Corridor Lounge-Dining is an enchanting space
  where shadows dance along the walls, hinting at the secrets hidden
  within the flickering candlelight. An eclectic mix of plush seating and
  ornate table settings invites exploration, promising delightful
  discoveries at every turn. Fixtures:

  • Corridor Coat Stand
      Exits: NORTH, SOUTH, WEST

_ 
      Lounge The Lounge is a cozy yet curious space, filled with mismatched
  furniture that seems to beckon tired souls to sit and share secrets,
  while shadows dance playfully across the walls. Items:

  • Rope
      Exits: EAST, SOUTH, WEST

_ 
      Corridor Hall-Lounge The Corridor Hall-Lounge stretches ahead, its walls
  adorned with portraits that seem to watch with a knowing smirk, inviting
  curious souls to explore the mysteries that await just beyond the next
  door. Each step echoes with whispers of the past, leading adventurers
  toward hidden secrets and playful enigmas. Fixtures:

  • Corridor Portrait Frame
      Items:

  • Silk Thread
      Exits: EAST, SOUTH, WEST

_ 
  You take the Silk Thread.

_ 
      Hall The Hall stretches ahead, adorned with shadows that dance playfully
  along the walls, whispering secrets of the past and inviting curious
  souls to explore further. Each step echoes with the promise of mysteries
  waiting to be uncovered, leading the way to the next adventure beyond
  this enchanting threshold to: Fixtures:

  • Case Board
      Exits: EAST, SOUTH, WEST

_ 
  Case Board: A corkboard laced with string and pins, ready to hold the
  night's evidence.

_ 
  You use torn menu on case board.

_ 
  You use glass shard on case board.

_ 
  You use chalk smudge on case board.

_ 
  You use pressed leaf on case board.

_ 
  You use ink note on case board.

_ 
  You use cellar ledger on case board.

_ 
  You use silk thread on case board.
  You assemble the clues on the case board and the pattern clicks into place.
  The case breaks, the mansion exhales, and the truth comes into focus.
- Tests:   ~ AI disabled (mode=Z1980, apiKey=missing)
┌───────────────────────────────────────────────────────────────────┐
│                       BUUI Adventure (1980)                       │
├─────┬───────────────────┬─────────────────────────────────────────┤
│   # │ Game              │ Description                             │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   1 │ Island Adventure  │ CASTAWAY THRILLS. Jungle traps,         │
│     │                   │ cliffside caves, and a desperate        │
│     │                   │ raft-for-freedom dash before the        │
│     │                   │ adventure claims you for good.          │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   2 │ Mansion Adventure │ THE MYSTERY HOUSE. Secret doors.        │
│     │                   │ Creaking halls. One wrong turn and the  │
│     │                   │ mansion keeps you forever.              │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   3 │ Western Adventure │ TERROR AT DEAD-MAN’S GULCH. TNT on the  │
│     │                   │ rails. The Iron Rattler thundering in.  │
│     │                   │ One last chance at Rattlesnake Bridge.  │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   4 │ Spy Adventure     │ TRAUMA-BOND, INTERNATIONAL MAN OF       │
│     │                   │ MAYHEM. Glamour, gadgets, and a race to │
│     │                   │ disarm the WOPR before it lights the    │
│     │                   │ world up.                               │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   q │ Quit              │                                         │
└─────┴───────────────────┴─────────────────────────────────────────┘

Select a game (1-4, or q to quit): 
  === Mansion Adventure ===

  You are the detective in the Hall.
  You chased the Coldwater killer for a decade; every witness ended up
  swallowed by the same mansion. Tonight the owner invited you in, promising
  the house will “talk” if you listen.
  Rain hammers boarded windows, power flickers, and the front door locked
  itself behind you. The floorplan shifts like it resents your footprints. You
  have one night to pry the truth out before the house keeps you, too.

_ 
  Unknown command. Type help for commands.

_ 
  Thanks for playing.
- Scope: Added per-game Mansion verb aliases (EXAMINE/INVESTIGATE/PIN) and confirmed EXAMINE in 1980 mode.
- Tests:   ~ AI disabled (mode=Z1980, apiKey=missing)
┌───────────────────────────────────────────────────────────────────┐
│                       BUUI Adventure (1980)                       │
├─────┬───────────────────┬─────────────────────────────────────────┤
│   # │ Game              │ Description                             │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   1 │ Island Adventure  │ CASTAWAY THRILLS. Jungle traps,         │
│     │                   │ cliffside caves, and a desperate        │
│     │                   │ raft-for-freedom dash before the        │
│     │                   │ adventure claims you for good.          │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   2 │ Mansion Adventure │ THE MYSTERY HOUSE. Secret doors.        │
│     │                   │ Creaking halls. One wrong turn and the  │
│     │                   │ mansion keeps you forever.              │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   3 │ Western Adventure │ TERROR AT DEAD-MAN’S GULCH. TNT on the  │
│     │                   │ rails. The Iron Rattler thundering in.  │
│     │                   │ One last chance at Rattlesnake Bridge.  │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   4 │ Spy Adventure     │ TRAUMA-BOND, INTERNATIONAL MAN OF       │
│     │                   │ MAYHEM. Glamour, gadgets, and a race to │
│     │                   │ disarm the WOPR before it lights the    │
│     │                   │ world up.                               │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   q │ Quit              │                                         │
└─────┴───────────────────┴─────────────────────────────────────────┘

Select a game (1-4, or q to quit): 
  === Mansion Adventure ===

  You are the detective in the Hall.
  You chased the Coldwater killer for a decade; every witness ended up
  swallowed by the same mansion. Tonight the owner invited you in, promising
  the house will “talk” if you listen.
  Rain hammers boarded windows, power flickers, and the front door locked
  itself behind you. The floorplan shifts like it resents your footprints. You
  have one night to pry the truth out before the house keeps you, too.

_ 
  Case Board: A corkboard laced with string and pins, ready to hold the
  night's evidence.

_ 
  Thanks for playing.
- Scope: Added motif verb alias files for all four games and expanded Island/Mansion aliases for theme-specific verbs.
- Tests:   ~ AI disabled (mode=Z1980, apiKey=missing)
┌───────────────────────────────────────────────────────────────────┐
│                       BUUI Adventure (1980)                       │
├─────┬───────────────────┬─────────────────────────────────────────┤
│   # │ Game              │ Description                             │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   1 │ Island Adventure  │ CASTAWAY THRILLS. Jungle traps,         │
│     │                   │ cliffside caves, and a desperate        │
│     │                   │ raft-for-freedom dash before the        │
│     │                   │ adventure claims you for good.          │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   2 │ Mansion Adventure │ THE MYSTERY HOUSE. Secret doors.        │
│     │                   │ Creaking halls. One wrong turn and the  │
│     │                   │ mansion keeps you forever.              │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   3 │ Western Adventure │ TERROR AT DEAD-MAN’S GULCH. TNT on the  │
│     │                   │ rails. The Iron Rattler thundering in.  │
│     │                   │ One last chance at Rattlesnake Bridge.  │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   4 │ Spy Adventure     │ TRAUMA-BOND, INTERNATIONAL MAN OF       │
│     │                   │ MAYHEM. Glamour, gadgets, and a race to │
│     │                   │ disarm the WOPR before it lights the    │
│     │                   │ world up.                               │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   q │ Quit              │                                         │
└─────┴───────────────────┴─────────────────────────────────────────┘

Select a game (1-4, or q to quit): 
  === Island Adventure ===

  Salt stings your lips as you come to on Wreck Beach, a sand-scratched canvas
  backpack digging into your shoulder like the last proof you’re still alive.
  You crawled onto Wreck Beach with a battered pack and salt-stung lungs
  No rescue is coming. The island is littered with rusted crates, broken
  masts, and half-hidden traps that look older than the tides. Every scrap you
  salvage buys another hour above the undertow.

_ 
  You scour the area; tracks point EAST.

_ 
  Thanks for playing.;   ~ AI disabled (mode=Z1980, apiKey=missing)
┌───────────────────────────────────────────────────────────────────┐
│                       BUUI Adventure (1980)                       │
├─────┬───────────────────┬─────────────────────────────────────────┤
│   # │ Game              │ Description                             │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   1 │ Island Adventure  │ CASTAWAY THRILLS. Jungle traps,         │
│     │                   │ cliffside caves, and a desperate        │
│     │                   │ raft-for-freedom dash before the        │
│     │                   │ adventure claims you for good.          │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   2 │ Mansion Adventure │ THE MYSTERY HOUSE. Secret doors.        │
│     │                   │ Creaking halls. One wrong turn and the  │
│     │                   │ mansion keeps you forever.              │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   3 │ Western Adventure │ TERROR AT DEAD-MAN’S GULCH. TNT on the  │
│     │                   │ rails. The Iron Rattler thundering in.  │
│     │                   │ One last chance at Rattlesnake Bridge.  │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   4 │ Spy Adventure     │ TRAUMA-BOND, INTERNATIONAL MAN OF       │
│     │                   │ MAYHEM. Glamour, gadgets, and a race to │
│     │                   │ disarm the WOPR before it lights the    │
│     │                   │ world up.                               │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   q │ Quit              │                                         │
└─────┴───────────────────┴─────────────────────────────────────────┘

Select a game (1-4, or q to quit): 
  === Mansion Adventure ===

  You are the detective in the Hall.
  You chased the Coldwater killer for a decade; every witness ended up
  swallowed by the same mansion. Tonight the owner invited you in, promising
  the house will “talk” if you listen.
  Rain hammers boarded windows, power flickers, and the front door locked
  itself behind you. The floorplan shifts like it resents your footprints. You
  have one night to pry the truth out before the house keeps you, too.

_ 
  You poke around and stir up dust, but find nothing useful.

_ 
  Thanks for playing.;   ~ AI disabled (mode=Z1980, apiKey=missing)
┌───────────────────────────────────────────────────────────────────┐
│                       BUUI Adventure (1980)                       │
├─────┬───────────────────┬─────────────────────────────────────────┤
│   # │ Game              │ Description                             │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   1 │ Island Adventure  │ CASTAWAY THRILLS. Jungle traps,         │
│     │                   │ cliffside caves, and a desperate        │
│     │                   │ raft-for-freedom dash before the        │
│     │                   │ adventure claims you for good.          │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   2 │ Mansion Adventure │ THE MYSTERY HOUSE. Secret doors.        │
│     │                   │ Creaking halls. One wrong turn and the  │
│     │                   │ mansion keeps you forever.              │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   3 │ Western Adventure │ TERROR AT DEAD-MAN’S GULCH. TNT on the  │
│     │                   │ rails. The Iron Rattler thundering in.  │
│     │                   │ One last chance at Rattlesnake Bridge.  │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   4 │ Spy Adventure     │ TRAUMA-BOND, INTERNATIONAL MAN OF       │
│     │                   │ MAYHEM. Glamour, gadgets, and a race to │
│     │                   │ disarm the WOPR before it lights the    │
│     │                   │ world up.                               │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   q │ Quit              │                                         │
└─────┴───────────────────┴─────────────────────────────────────────┘

Select a game (1-4, or q to quit): 
  === Western Adventure ===

  The Iron Rattler hisses at the platform; the payroll strongbox must be
  secured before sundown, and someone has wired Rattlesnake Bridge with TNT.
  You once rode with the Iron Rattler gang; now Dry Gulch pays you to keep it
  alive. A payroll strongbox rolls in with the dusk train, and so do old
  partners carrying fresh dynamite and older grudges.
  The bridge is creaking, the telegraph is cut, and the sheriff's badge is on
  your chest for one night only. Save the strongbox, keep the bridge intact,
  and get the train out before sundown closes the deal.

_ 
  You rummage around and uncover: Bridge Signal

_ 
  Thanks for playing.;   ~ AI disabled (mode=Z1980, apiKey=missing)
┌───────────────────────────────────────────────────────────────────┐
│                       BUUI Adventure (1980)                       │
├─────┬───────────────────┬─────────────────────────────────────────┤
│   # │ Game              │ Description                             │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   1 │ Island Adventure  │ CASTAWAY THRILLS. Jungle traps,         │
│     │                   │ cliffside caves, and a desperate        │
│     │                   │ raft-for-freedom dash before the        │
│     │                   │ adventure claims you for good.          │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   2 │ Mansion Adventure │ THE MYSTERY HOUSE. Secret doors.        │
│     │                   │ Creaking halls. One wrong turn and the  │
│     │                   │ mansion keeps you forever.              │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   3 │ Western Adventure │ TERROR AT DEAD-MAN’S GULCH. TNT on the  │
│     │                   │ rails. The Iron Rattler thundering in.  │
│     │                   │ One last chance at Rattlesnake Bridge.  │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   4 │ Spy Adventure     │ TRAUMA-BOND, INTERNATIONAL MAN OF       │
│     │                   │ MAYHEM. Glamour, gadgets, and a race to │
│     │                   │ disarm the WOPR before it lights the    │
│     │                   │ world up.                               │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   q │ Quit              │                                         │
└─────┴───────────────────┴─────────────────────────────────────────┘

Select a game (1-4, or q to quit): 
  === Spy Adventure ===

  Neon drips off wet Kowloon streets; retrieve the WOPR before the
  harbor handoff.
  1970 Hong Kong: humidity, neon, triads, and three-letter agencies tripping
  over each other in back alleys. You are off-book, tasked to intercept the
  WOPR before it moves across the harbor.
  The Capsule is said to carry the WOPR drive schematics—proof that someone
  intends to light a global fuse. Rival spooks, smugglers, and local crews are
  all hunting it; your only edge is getting there first.
  Safehouses burn fast, phones click with taps, and the harbor timetable is
  tightening by the minute. The Capsule’s courier changes routes when spooked,
  so every alley clue matters.

_ 
  You poke around and stir up dust, but find nothing useful.

_ 
  Thanks for playing.
- Scope: Ran a 1980 Spy Adventure alias pass to confirm motif verbs and help alias listing.
- Tests:   ~ AI disabled (mode=Z1980, apiKey=missing)
┌───────────────────────────────────────────────────────────────────┐
│                       BUUI Adventure (1980)                       │
├─────┬───────────────────┬─────────────────────────────────────────┤
│   # │ Game              │ Description                             │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   1 │ Island Adventure  │ CASTAWAY THRILLS. Jungle traps,         │
│     │                   │ cliffside caves, and a desperate        │
│     │                   │ raft-for-freedom dash before the        │
│     │                   │ adventure claims you for good.          │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   2 │ Mansion Adventure │ THE MYSTERY HOUSE. Secret doors.        │
│     │                   │ Creaking halls. One wrong turn and the  │
│     │                   │ mansion keeps you forever.              │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   3 │ Western Adventure │ TERROR AT DEAD-MAN’S GULCH. TNT on the  │
│     │                   │ rails. The Iron Rattler thundering in.  │
│     │                   │ One last chance at Rattlesnake Bridge.  │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   4 │ Spy Adventure     │ TRAUMA-BOND, INTERNATIONAL MAN OF       │
│     │                   │ MAYHEM. Glamour, gadgets, and a race to │
│     │                   │ disarm the WOPR before it lights the    │
│     │                   │ world up.                               │
├─────┼───────────────────┼─────────────────────────────────────────┤
│   q │ Quit              │                                         │
└─────┴───────────────────┴─────────────────────────────────────────┘

Select a game (1-4, or q to quit): 
  === Spy Adventure ===

  Neon drips off wet Kowloon streets; retrieve the WOPR before the
  harbor handoff.
  1970 Hong Kong: humidity, neon, triads, and three-letter agencies tripping
  over each other in back alleys. You are off-book, tasked to intercept the
  WOPR before it moves across the harbor.
  The Capsule is said to carry the WOPR drive schematics—proof that someone
  intends to light a global fuse. Rival spooks, smugglers, and local crews are
  all hunting it; your only edge is getting there first.
  Safehouses burn fast, phones click with taps, and the harbor timetable is
  tightening by the minute. The Capsule’s courier changes routes when spooked,
  so every alley clue matters.

_ 
  Commands:
    move (go) <direction> Move (N,S,E,W,NE,NW,SE,SW,UP,DOWN)
    n|s|e|w|u|d|ne|nw|se|sw Shortcut movement
    look (l)       Describe the current plot
    look <thing>   Inspect an item/fixture/actor you can see or carry
    inspect <thing> Look closely at an item/fixture/actor
    listen         Quick re-read of the current scene (alias of look)
    take <item>    Pick up a visible item here
    drop <item>    Drop an item from your inventory
    put <item>     Drop an item from your inventory
    open <thing>   Try opening a visible item or gate
    use <thing>    Try using an item
    attack <target> Attack a visible actor
    flee/run away  Try to escape combat
    inventory (i)  Show what you're carrying
    craft <item>   Craft an item if you have the ingredients
    how craft <item> Show required skill and ingredients
    search/explore Poke around for hidden items
    help (h, ?)    Show this help
    quit (q)       Exit the game
  Aliases: go -> move, climb -> move, craft -> make, grab -> take, explore ->
  search, strike -> attack, exit -> quit, run -> move, sneak -> move, tail ->
  move, infiltrate -> move, slip -> move, scan -> search, surveil -> search,
  recon -> search, bug -> use, hack -> use, tap -> use, plant -> use

_ 
  You scour the area; tracks point EAST.

_ 
  Flicker Sign Stack
  One vertical sign sputters in a steady rhythm. The “wrong” strokes look
  intentional, like a code.
  Exits: EAST, NORTH, SOUTH, WEST

_ 
  You poke around and stir up dust, but find nothing useful.

_ 
  Nathan Road Neon Canyon
  Neon stacks climb like cliffs. The sidewalk is a moving river of shoulders,
  reflections, and flickering characters. Listening, you catch tram bells and
  a low-high-low taxi horn motif repeating in the hum.
  Exits: EAST, NORTH, SOUTH

_ 
  Flicker Sign Stack
  One vertical sign sputters in a steady rhythm. The “wrong” strokes look
  intentional, like a code.
  Exits: EAST, NORTH, SOUTH, WEST

_ 
  You don't see that here.

_ 
  Thanks for playing.

## 2025-12-25 — Island Adventure Scratch warning
- Scope: Clarified Scratch as the Time Stone warden and added an uppercase warning on skeleton interaction.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,IslandAdventurePlaybookIntegrationTest test`

## 2025-12-25 — Island Adventure loop + hatchet fixes
- Scope: Moved the hatchet under the Treehouse Skeleton fixture (use interaction reveals it and frees Scratch) and removed the Time Stone tick-rate acceleration.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,IslandAdventurePlaybookIntegrationTest test`

## 2025-12-25 — Spy Adventure mechanics fixes
- Scope: Added missing Spy Adventure fixtures/items (postbox, latch, relief, shards, WOPR) and ON_USE triggers for phrase/pass/stencil interactions.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,SpyAdventurePlaybookIntegrationTest test`

## 2025-12-25 — Island/Mansion/Western 1980 runs (scoreboard)
- Scope: Logged full 1980 playthroughs for Island, Mansion, and Western adventures in `docs/process/scoreboard.md` with runtime and blocking issues.
- Tests: Manual runs via `./adventure --mode=1980` (screen hardcopy logs: `/tmp/island1980.log`, `/tmp/mansion1980.log`, `/tmp/western1980.log`).

## 2025-12-25 — Spy Adventure 1980 full run (scoreboard)
- Scope: Logged a full 1980 Spy Adventure playthrough in `docs/process/scoreboard.md` with runtime and blocking issues.
- Tests: Manual run via `./adventure --mode=1980` (screen hardcopy, local `/tmp/spy1980.log`).

## 2025-12-25 — Spy Adventure playbook deep clean
- Scope: Updated the Spy Adventure playbook YAML/MD to follow explicit gates and win trigger expectations, added the playbook integration test fixture, removed stray log artifacts, and refreshed housekeeping logs.
- Tests: `mvn -q -Dtest=SpyAdventurePlaybookIntegrationTest test`; `mvn -q test`
- Scope: Ran 1980 spot checks for Island/Mansion/Western/Spy using basic commands.
- Tests: `printf '1\nlook\ninventory\nsearch\neast\nlook\nwest\nquit\n' | ./adventure --mode=1980 --quiet`; `printf '2\nlook\ninventory\nsearch\nwest\nlook\neast\nquit\n' | ./adventure --mode=1980 --quiet`; `printf '3\nlook\ninventory\nsearch\neast\nlook\nwest\nquit\n' | ./adventure --mode=1980 --quiet`; `printf '4\nlook\ninventory\nsearch\neast\nlook\nwest\nquit\n' | ./adventure --mode=1980 --quiet`
- Scope: Ran 1980 motif-verb spot checks across Island/Mansion/Western/Spy to validate alias parsing.
- Tests: `printf '1\nlook\nscavenge\nread rags\npaddle east\nlook\nrow west\nquit\n' | ./adventure --mode=1980 --quiet`; `printf '2\nlook\nexamine case board\nprobe\nwest\nlook\neast\nquit\n' | ./adventure --mode=1980 --quiet`; `printf '3\nlook\nscout\nride east\nlook\ntrot west\nquit\n' | ./adventure --mode=1980 --quiet`; `printf '4\nlook\nsurveil\nsneak east\nscan\ntail west\nquit\n' | ./adventure --mode=1980 --quiet`
- Scope: Captured ASCII-normalized 1980 motif-verb spot check transcripts for all adventures under `docs/process/playtests/1980-motif-spotcheck-2025-12-25.md`.
- Tests: `./adventure --mode=1980 --quiet` runs captured in `docs/process/playtests/1980-motif-spotcheck-2025-12-25.md`
- Scope: Reorganized Island/Mansion/Western/Spy resources into world/narrative/motif/assets folders, updated game.yaml includes and RuntimeLoader/CraftingTable paths, refreshed GameStructExporter/GamePlan outputs, and added per-game README.md notes.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest,GdlCliSmokeTest,SmartActorSpecLoaderTest test`

## 2025-12-25 — Smart actor runtime + combat sim
- Scope: Wired smart actor runtime (prompt builder/planner/registry + actor-scoped execution), added smart-actor config defaults and runbook updates, and added a combat sim minigame with deterministic dice in playbook tests.
- Tests: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest,SmartActorDecisionParserTest,SmartActorSpecLoaderTest,SmartActorTagLoaderTest test`

## 2025-12-25 — Smart actor combat constraints + warded sim
- Scope: Enabled smart actors to act in combat with ATTACK/FLEE constraints and hesitation fallback, added warded combat sim playbook, and added a smart-actor combat playbook/test harness.
- Tests: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest,SmartActorCombatPlaybookIntegrationTest test`

## 2025-12-25 — Playbook scripted smart-actor decisions
- Scope: Added playbook YAML support for scripted smart-actor decisions so combat tests can drive deterministic outputs.
- Tests: `mvn -q -Dtest=SmartActorCombatPlaybookIntegrationTest,MiniGamePlaybookIntegrationTest test`

## 2025-12-25 — Playbook expectations + smart-actor combat turn fix
- Scope: Added shared playbook parsing support with smartActor expectations, updated playbook tests, added flee playbook coverage, and fixed smart-actor combat turn advancement to avoid cooldown stalls.
- Tests: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,MansionAdventurePlaybookIntegrationTest,WesternAdventurePlaybookIntegrationTest,SpyAdventurePlaybookIntegrationTest,MiniGamePlaybookIntegrationTest,SmartActorCombatPlaybookIntegrationTest test`

## 2025-12-25 — Smart-actor minigame playbook variants
- Scope: Added smart-actor playbook variants + smart-actor specs for combat-sim minigames and fixed playbook segment slicing to avoid multibyte truncation.
- Tests: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest,SmartActorCombatPlaybookIntegrationTest test`

## 2025-12-25 — Smart-actor combat targeting fix
- Scope: Updated player plot ownership on movement so smart actors can target the player in combat, then aligned smart-actor minigame playbooks with real attack output.
- Tests: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest,SmartActorCombatPlaybookIntegrationTest test`

## 2025-12-25 — Testing strategy doc
- Scope: Documented the testing strategy and agent validation workflow for game verification.
- Tests: none (doc-only).

## 2025-12-25 — Game integrity check
- Scope: Added GameIntegrityCheck (static validation + bounded reachability), config/report types, a GameIntegrityTest gate, and updated the testing strategy doc.
- Tests: `mvn -q -Dtest=GameIntegrityTest test`

## 2025-12-25 — Island Scratch description
- Scope: Softened Scratch's description to avoid early spoilers about the Time Stone.
- Tests: none

## 2025-12-25 — Time Stone tick-rate
- Scope: Restored the Time Stone trigger to speed up time by setting the world TICK_RATE on take.
- Tests: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest test`

## 2025-12-25 — BuuiConsole output suppression
- Scope: Added BuuiConsole/ConsolePrinter mute toggle to silence CLI output in tests.
- Tests: `mvn -q -Dtest=ConsolePrinterTest test`

## 2025-12-25 — Playbook console capture
- Scope: Added a shared ConsoleCaptureExtension and updated playbook integration tests to use per-suite output capture.
- Tests: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,MansionAdventurePlaybookIntegrationTest,WesternAdventurePlaybookIntegrationTest,SpyAdventurePlaybookIntegrationTest,MiniGamePlaybookIntegrationTest,SmartActorCombatPlaybookIntegrationTest test`

## 2025-12-25 — Console capture expansion
- Scope: Extended ConsoleCaptureExtension to capture stderr and refit remaining CLI-output tests to use the shared helper.
- Tests: `mvn -q -Dtest=NarratorFallbackTest,GameCliCombatErrorTest,GameCliExitFormattingTest,WorldAssemblerPrintsBomOnSuccessTest,WorldAssemblerPrintsReportOnFailureTest,GdlCliSmokeTest,StorybookValidateCliSmokeTest,KeyExpressionEvaluatorTest test`

## 2025-12-25 — Console capture cleanup
- Scope: Updated ConsolePrinterTest to use ConsoleCaptureExtension instead of manual System.out swapping.
- Tests: `mvn -q -Dtest=ConsolePrinterTest test`

## 2025-12-25 — Island crafting audit + raft trace
- Scope: Pruned unused Island crafting recipes and traced the raft chain (drop Dead iPad to pick up Bamboo Poles/Parachute); integrity report still hits max-state bounds.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`

## 2025-12-25 — Island raft setup + integrity sweep
- Scope: Enlarged the Canvas Backpack to carry raft ingredients, added an Island raft playbook, and widened integrity defaults.
- Tests: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,GameIntegrityTest test`; `mvn -q test`

## 2025-12-25 — Island test-game win gate
- Scope: Added a test-only Island game YAML with reduced clock gate, plus an escape playbook to validate the win path.
- Tests: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,GameIntegrityTest test`; `mvn -q test`

## 2025-12-26 — Dungeon adventure minigame
- Scope: Added a 4x4 dungeon adventure minigame with monster combat plus a playbook for integration tests.
- Tests: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest test`

## 2025-12-26 — Integrity test gating
- Scope: Skipped GameIntegrityTest by default and documented the opt-in flag for slower runs.
- Tests: `mvn -q -Dtest=GameIntegrityTest test`

## 2025-12-26 — Mansion/Western win playbooks
- Scope: Enforced full win paths in Mansion/Western playbooks, aligned win expectations with trigger output, and relaxed quit requirements for win outcomes in playbook tests.
- Tests: `mvn -q -Dtest=MansionAdventurePlaybookIntegrationTest,WesternAdventurePlaybookIntegrationTest test`

## 2025-12-26 — 1980 wins recorded
- Scope: Captured scripted 1980 win transcripts for Island, Mansion, and Western and updated the scoreboard.
- Tests: `./adventure --mode=1980 --quiet < /tmp/island-1980-win-input.txt > docs/process/playtests/2025-12-26-island-1980-walkthrough.md`; `./adventure --mode=1980 --quiet < /tmp/mansion-1980-win-input.txt > docs/process/playtests/2025-12-26-mansion-1980-walkthrough.md`; `./adventure --mode=1980 --quiet < /tmp/western-1980-win-input.txt > docs/process/playtests/2025-12-26-western-1980-walkthrough.md`

## 2025-12-26 — Integrity sweep (opt-in)
- Scope: Ran the full integrity sweep with `-DrunIntegrity=true` and refreshed the testing strategy verification date.
- Tests: `mvn -q -Dtest=GameIntegrityTest -DrunIntegrity=true test`

## 2025-12-26 — Full suite
- Scope: Ran the full test suite for release verification.
- Tests: `mvn -q test`

## 2025-12-26 — Spy 1980 win recorded
- Scope: Captured a scripted 1980 win for Spy Adventure and updated the scoreboard.
- Tests: `./adventure --mode=1980 --quiet < /tmp/spy-1980-win-input.txt > docs/process/playtests/2025-12-26-spy-1980-walkthrough.md`

## 2025-12-26 — Docs/runbook reorg
- Scope: Moved root-level docs into `docs/`, added a logs hub, and consolidated AI/CLI runbooks under `docs/howto/runbooks/`.
- Tests: none

## 2025-12-26 — Island test game relocation
- Scope: Moved the Island test-only game bundle into `src/test/resources/games/` and repointed Island playbooks/testing strategy.
- Tests: none

## 2025-12-26 — Adventure playbook harness
- Scope: Added shared adventure playbook runner and refit Island/Mansion/Western/Spy playbook integration tests.
- Tests: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,MansionAdventurePlaybookIntegrationTest,WesternAdventurePlaybookIntegrationTest,SpyAdventurePlaybookIntegrationTest test`

## 2025-12-26 — Package cleanup + Spy id
- Scope: Moved AI runtime/authoring and engine runtime packages and standardized Spy game id/path.
- Tests: `mvn -q test`

## 2025-12-26 — Docs structure + package-info
- Scope: Added architecture/games/glossary/ADR docs, runbook index, and package-info summaries for core packages.
- Tests: not run (docs-only + package-info)

## 2025-12-26 — Workspace process doc
- Scope: Added parent-level `PROCESS.md` with Java/Markdown standards and documentation workflow.
- Tests: not run (workspace doc only)

## 2025-12-26 — SmartActorRuntime test fixes
- Scope: Added missing SmartActorRuntime import, aligned trimLines to count non-empty lines, and updated exits expectations to Direction long names.
- Tests: `mvn -q -Dtest=SmartActorRuntimeTest test`

## 2025-12-26 — GamePlan/ListRenderer test fixes
- Scope: Added stricter YAML key validation for GamePlan CLI and fixed ListRendererTest to allow null list items.
- Tests: `mvn -q -Dtest=GamePlanCliTest,ListRendererTest test`

## 2025-12-27 — AI client refactor
- Scope: Added a shared AI chat client + JSON utilities, removed Spring AI dependencies, rewired narrator/translator/smart actor/authoring/gardener/DM calls, and updated AI prompt docs/tests.
- Tests: `mvn -q -Dtest=AiJsonTest,CommandTranslatorTest,AuthoringAgentClientTest,SmartActorServiceTest,AiDmAgentTest,AiFixtureDescriptionExpanderTest,OpenAiHttpFixtureDescriptionExpanderTest test`

## 2025-12-29 — Markdown compiler pipeline
- Scope: Added MarkdownCompiler + MarkdownDocument for tokenized markdown compilation, wired MarkdownRenderer through the compiler, and added printCompiled helpers for precompiled output.
- Tests: `mvn -q -Dtest=MarkdownScannerTest,MarkdownCompilerTest,MarkdownRendererTest,BuuiMarkdownTest,ConsolePrinterTest test`

## 2025-12-29 — Markdown style sheets
- Scope: Added CSS-like markdown style sheets with a parser, standard.style template, and style-aware compilation (defaults load standard.style when present and otherwise render plain text); inline color tags are invalid and error with reporting.
- Tests: `mvn -q -Dtest=InlineMarkdownTest,MarkdownRendererTest,MarkdownRendererComprehensiveTest,MarkdownStyleSheetTest,MarkdownCompilerTest,MarkdownScannerTest,BuuiMarkdownTest,ConsolePrinterTest test`

## 2025-12-29 — Markdown-first printing
- Scope: Added MarkdownScanner detection, switched print() to markdown-first with new println() for plain text, and updated authoring CLIs + runbook.
- Tests: `mvn -q -Dtest=ConsolePrinterTest,MarkdownRendererTest,BuuiMarkdownTest test`

## 2025-12-29 — Buui list helper
- Scope: Added a BuuiList renderer for numbered title + options output and test coverage.
- Tests: `mvn -q -Dtest=BuuiListTest test`

## 2025-12-29 — Buui ANSI styling
- Scope: Added ANSI markdown styling (bold/italic/colors) with NO_COLOR handling across BUUI renderers, plus BUUI styling docs (architecture + runbook), BuuiMarkup helpers, and a BuuiStyleDemo sample.
- Tests: `mvn -q -Dtest=AsciiRendererTest,BuuiMenuTest,ConsolePrinterTest,ListRendererTest,MarkdownRendererTest,TextUtilsTest,InlineMarkdownTest,BuuiListTest test`; `mvn -q -Dtest=BuuiMarkupTest test`

## 2025-12-29 — Markdown style map coverage
- Scope: Mapped markdown inline/paragraph/list/table defaults through MarkdownStyleMap before ANSI encoding and updated BUUI runbook mapping notes.
- Tests: `mvn -q -Dtest=InlineMarkdownTest,MarkdownRendererTest,AsciiRendererTest test`

## 2025-12-29 — Inline markdown base toggles
- Scope: Ensured inline bold/italic markers toggle correctly even when base styles (e.g., blockquotes/headings) are active.
- Tests: `mvn -q -Dtest=InlineMarkdownTest,MarkdownRendererTest,AsciiRendererTest test`

## 2025-12-29 — ListRenderer ANSI trim fix
- Scope: Avoided stripping ANSI escape codes by replacing list-item `trim()` with whitespace-only trimming.
- Tests: `mvn -q -Dtest=ListRendererTest,TextUtilsTest,MarkdownRendererTest,InlineMarkdownTest,AsciiRendererTest test`

## 2025-12-29 — Comprehensive markdown coverage
- Scope: Added a comprehensive markdown rendering test covering headings, lists, tables, code fences, scene breaks, and inline styles.
- Tests: `mvn -q -Dtest=MarkdownRendererComprehensiveTest test`

## 2025-12-29 — Markdown narrator prompts + help
- Scope: Switched CLI help text to markdown formatting and updated narrator prompt templates to request markdown output.
- Tests: `mvn -q -Dtest=NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-29 — Preamble narration formatting
- Scope: Routed game preambles/backstory through a markdown narration printer, using blockquotes for plain text narration.
- Tests: `mvn -q -Dtest=ConsolePrinterTest test`

## 2025-12-29 — BuuiMarkdown render
- Scope: Added BuuiMarkdown.render to centralize narration markdown formatting and kept ConsolePrinter narration aligned.
- Tests: `mvn -q -Dtest=BuuiMarkdownTest,ConsolePrinterTest test`

## 2025-12-29 — Markdown section formatting
- Scope: Split markdown narration from section labels (Items/Exits) and kept exits flush-left for cleaner CLI output.
- Tests: `mvn -q -Dtest=MarkdownRendererTest test`

## 2025-12-29 — Markdown style validation
- Scope: Tightened markdown style parsing, made standard.style loading strict/lazy, and report invalid style sheets as markdown validation errors.
- Tests: `mvn -q -Dtest=MarkdownStyleSheetTest,InlineMarkdownTest,MarkdownRendererTest test`

## 2025-12-29 — Narrator raw fallback output
- Scope: Used println for AI-disabled narrator fallbacks so raw output remains plain.
- Tests: `mvn -q -Dtest=NarratorFallbackTest test`

## 2025-12-29 — BUUI style selection + table width
- Scope: Made BUUI style selection opt-in via BUUI_STYLE, centralized layout defaults (gutter/edge padding), and added max-width table rendering with header truncation.
- Tests: `mvn -q -Dtest=MarkdownStyleSheetTest,InlineMarkdownTest,MarkdownRendererTest,AsciiRendererTest,ConsolePrinterTest,BuuiListTest test`

## 2025-12-29 — Adventure launcher BUUI_STYLE default
- Scope: Defaulted the `./adventure` launcher to use `standard.style` when present.
- Tests: not run (launcher script change only)

## 2025-12-29 — Narration gutter dot
## 2025-12-29 — Narration gutter bullets
- Scope: Dropped blockquote indentation, added a paragraph-leading bullet gutter, and normalized narration paragraphs to a single blank line.
- Tests: `mvn -q -Dtest=BuuiMarkdownTest,MarkdownRendererTest,ConsolePrinterTest test`

## 2025-12-29 — Markdown narrator fallback formatting
- Scope: Rendered AI-disabled narration through markdown, stripped heading markers for fallback headers, and switched exit separators to bullets with section/exit styling in standard.style.
- Tests: `mvn -q -Dtest=ConsolePrinterTest,NarratorFallbackTest,GameCliExitFormattingTest test`

## 2025-12-29 — Adventure launcher default quiet
- Scope: Defaulted `./adventure` to quiet Maven output; `--verbose` enables logs.
- Tests: not run (script change only)

## 2025-12-29 — Narrator exits normalization
- Scope: Updated narrator prompt to copy the exact "Exits:" line and normalized AI output to enforce it; fallback now preserves the Exits line verbatim.
- Tests: `mvn -q -Dtest=NarratorPromptGoldenTest,NarratorFallbackTest,NarratorPromptSelectionTest test`

## 2025-12-29 — Scene snapshot priming
- Scope: Prime scene snapshots on game start so AI narration always has exits available to render.
- Tests: `mvn -q -Dtest=GameRuntimeCoverageTest,NarratorFallbackTest test`

## 2025-12-29 — Smart actor local scope
- Scope: In CLI, smart actors now run only when co-located with the player to avoid long post-narration pauses; scope is configurable via `ai.smart_actor.scope`.
- Tests: `mvn -q -Dtest=SmartActorRuntimeTest test`

## 2025-12-29 — Gate descriptions per side
- Scope: Preserve per-side gate descriptions from YAML so directional looks reflect the correct direction.
- Tests: `mvn -q -Dtest=GameRuntimeCoverageTest test`

## 2025-12-29 — Look around normalization
- Scope: Normalized LOOK commands that start with "around" and updated the translator prompt to map direction questions to LOOK.
- Tests: `mvn -q -Dtest=CommandCompilerTest,TranslatorPromptGoldenTest test`

## 2025-12-29 — Narrator location anchoring
- Scope: Anchored narrator output to the engine’s location line when AI output drifts and suppressed snapshot context when raw output already includes exits.
- Tests: `mvn -q -Dtest=NarrationServiceTest test`

## 2025-12-29 — Narrator multi-scene fallback
- Scope: Restored multi-scene error handling so snapshot fallback preserves color narration.
- Tests: `mvn -q -Dtest=NarratorFallbackTest,NarrationServiceTest test`

## 2025-12-29 — Narrator action-result enforcement
- Scope: Appended missing action-result text (e.g., look-direction gate descriptions) to AI narration output.
- Tests: `mvn -q -Dtest=NarrationServiceTest test`

## 2025-12-29 — Look-target narration focus
- Scope: Focused look-target narration on the action result using scanner tokens and updated the narrator prompt to require concise look responses.
- Tests: `mvn -q -Dtest=NarrationServiceTest,NarratorPromptGoldenTest test`

## 2025-12-29 — Compiled narrator prompt
- Scope: Replaced the narrator template with a per-context compiled prompt and removed narrator prompt files from resources.
- Tests: `mvn -q -Dtest=NarrationServiceTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-29 — Narrator prompt minimal mode
- Scope: Simplified the compiled narrator prompt to a smaller, mode-focused contract.
- Tests: `mvn -q -Dtest=NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-29 — Narrator prompt rewrite focus
- Scope: Reframed the narrator prompt as a compact rewrite instruction with only key facts.
- Tests: `mvn -q -Dtest=NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Narrator recent actions context
- Scope: Added a RECENT_ACTIONS block to narrator prompts using the last 2 canonical commands and sanitized engine outputs.
- Tests: `mvn -q -Dtest=NarrationServiceTest,NarratorFallbackTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Exits list omits gate hints
- Scope: Exits list now renders directions only, with gate descriptions reserved for look-direction results.
- Tests: `mvn -q -Dtest=GameCliExitFormattingTest test`

## 2025-12-30 — Look around misspelling alias
- Scope: Normalized "look arround" to the look-around behavior to avoid noisy action-result narration.
- Tests: `mvn -q -Dtest=CommandCompilerTest test`

## 2025-12-30 — Blocked move narration cleanup
- Scope: Blocked movement now emits only the gate description (or a generic message when no gate) to avoid duplicate narration.
- Tests: `mvn -q -Dtest=GoCommandTest test`

## 2025-12-30 — Narrator recent narration context
- Scope: Added RECENT_NARRATION (last 2 narrator outputs, sanitized) to the narrator prompt for continuity.
- Tests: `mvn -q -Dtest=NarrationServiceTest,NarratorFallbackTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest,GoCommandTest test`

## 2025-12-30 — Look-direction prefix
- Scope: Look-direction responses now prefix gate descriptions with the direction label (e.g., "To the north:").
- Tests: `mvn -q -Dtest=GoCommandTest test`

## 2025-12-30 — Translator direction grounding
- Scope: Reject translated direction commands unless the player input contains the same direction token; updated translator prompt guidance.
- Tests: `mvn -q -Dtest=TranslationOrchestratorTest,TranslatorPromptGoldenTest test`

## 2025-12-30 — Translator grounding guard (all tokens)
- Scope: Translator outputs are rejected unless argument tokens (directions/identifiers/strings) appear in player input; adds a second guard test.
- Tests: `mvn -q -Dtest=TranslationOrchestratorTest,TranslatorPromptGoldenTest test`

## 2025-12-30 — LLM prompt printing toggle
- Scope: Added prompt printing hooks for all LLM callers (including gardener expanders) and documented the `ai.prompts.print` toggle.
- Tests: Not run (config/runbook + prompt logging wiring).

## 2025-12-30 — Adventure launcher prompt logging
- Scope: Set `AI_PROMPTS_PRINT=true` in `./adventure` to always print LLM prompts.
- Tests: Not run (launcher env change).

## 2025-12-30 — Narrator prompt cadence update
- Scope: Shifted narrator prompt to second-person present tense with tighter cadence, removed verbatim ACTION_RESULT guidance, and added destination name fidelity rules.
- Tests: Not run (prompt text changes).

## 2025-12-30 — Emote narration path
- Scope: Translator can emit `EMOTE: ...`, CLI routes emotes directly to narration without advancing state, and narrator prompts enforce emote-specific output rules.
- Tests: `mvn -q -Dtest=TranslatorPromptGoldenTest,TranslationOrchestratorTest,TranslatorServiceTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Emote dice checks
- Scope: Added DICE command parsing/handling, emote check requests/results in engine output, and narrator prompt modes for CHECK_REQUEST/CHECK_RESULT.
- Tests: `mvn -q -Dtest=CommandCompilerTest,CommandScannerTest,VerbAliasesTest,TranslatorPromptGoldenTest,TranslatorPromptCoverageTest,TranslatorServiceTest,TranslationOrchestratorTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest,NarrationServiceTest test`

## 2025-12-30 — Emote/check prompt tightening
- Scope: Forbade scene-source detail use in EMOTE/CHECK outputs and forced a direct roll sentence format.
- Tests: `mvn -q -Dtest=NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Narrator look formatting fixes
- Scope: Suppressed location sentences for look/inspect/listen narration, appended fixtures/items blocks from scene output, normalized dice call spacing, and refreshed narrator prompt goldens/tests.
- Tests: `mvn -q -Dtest=NarrationServiceTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Narrator plain language pass
- Scope: Tightened narrator prompt rules to require plain, literal wording, avoid figurative language, and avoid blank lines inside the narration paragraph.
- Tests: `mvn -q -Dtest=NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Plain world descriptions pass
- Scope: Rewrote mansion/island/western/spy world descriptions to plain, literal sentences across plots, gates, fixtures, and items.
- Tests: `mvn -q -Dtest=GameMenuStructuredLoadTest test`; `mvn -q -Dtest=WesternAdventurePlaybookIntegrationTest test`

## 2025-12-30 — Narrator literal gating fallback
- Scope: Enforced exact-wording narrator prompts, removed location sentences across modes, and added deterministic fallback when LLM output drifts.
- Tests: `mvn -q -Dtest=NarrationServiceTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Command-specific narrator prompts
- Scope: Added per-command narrator prompt modes (scene/look-target/look-direction/action/color/emote/check), trimmed prompt content, and updated runbook/design docs.
- Tests: `mvn -q -Dtest=NarrationServiceTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Narrator grounding leniency
- Scope: Relaxed narrator grounding thresholds by prompt mode, allowed emote/check paraphrase with direction-token blocking, and gated fallback logs behind the debug flag.
- Tests: `mvn -q -Dtest=NarrationServiceTest test`

## 2025-12-30 — Adventure launcher prompt output mute
- Scope: Removed default `AI_PROMPTS_PRINT=true` from `./adventure` so prompts stay quiet unless explicitly enabled.
- Tests: Not run (launcher change).

## 2025-12-30 — Translator location question mapping
- Scope: Added translator prompt rule to map “where am I/are we” to `look`, and removed the scanner/compiler alias and help entry.
- Tests: `mvn -q -Dtest=TranslatorPromptGoldenTest,TranslatorPromptCoverageTest,CommandCompilerTest test`

## 2025-12-30 — Narrator prompt intent context
- Scope: Added PLAYER_TEXT/LAST_COMMAND context blocks to narrator prompts (v0.30) and added deterministic translator handling for “where am I/are we” to short-circuit to `look`.
- Tests: `mvn -q -Dtest=TranslatorPromptGoldenTest,TranslatorPromptCoverageTest,TranslatorServiceTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest test`

## 2025-12-30 — Emote tightening + gate tag cleanup
- Scope: Tightened emote narration to one sentence with no ambient/backstory drift unless in recent actions, added location-question intent guidance to narrator prompts (v0.31), and stripped trailing `to:` destination tags from look-direction/blocked-move gate descriptions.
- Tests: `mvn -q -Dtest=NarratorPromptGoldenTest,NarratorPromptSelectionTest,GoCommandTest test`

## 2025-12-30 — Translator grounding with visible labels
- Scope: Relaxed translator grounding to accept visible fixture/item/inventory labels not present in player input and updated translator prompt to allow using visible labels for partial references.
- Tests: `mvn -q -Dtest=TranslationOrchestratorTest,TranslatorPromptGoldenTest,TranslatorPromptCoverageTest test`

## 2025-12-30 — Exits only on scene narration
- Scope: Stopped appending exits for action/inspect/search narration, updated narrator prompt v0.32 intent guidance, and adjusted fallbacks/tests accordingly.
- Tests: `mvn -q -Dtest=NarrationServiceTest,NarratorFallbackTest,NarratorPromptGoldenTest,NarratorPromptSelectionTest,GoCommandTest test`

## 2025-12-30 — Translator label overlap gating
- Scope: Restricted visible-label grounding to cases where player input shares a word with the label, preventing reactions like “oh cool” from becoming inspect commands.
- Tests: `mvn -q -Dtest=TranslationOrchestratorTest,TranslatorPromptGoldenTest,TranslatorPromptCoverageTest test`

## 2025-12-30 — Smart actor conversation (@mention)
- Scope: Added TALK command and @mention conversation flow, wired smart-actor replies with player utterance context/history, updated translator prompt to include talk, and moved the mansion butler to the Hall.
- Tests: `mvn -q -Dtest=CommandCompilerTest,SmartActorRuntimeTest,TranslatorPromptGoldenTest,TranslatorPromptCoverageTest,GameMenuStructuredLoadTest test`

## 2025-12-30 — Mention resolution + identity routing
- Scope: Expanded @mention parsing to allow token-prefix/unique single-token actor matches and mid-line mentions; added deterministic translator routing for “who is <name>” to `look <name>`.
- Tests: `mvn -q -Dtest=TranslatorServiceTest,MentionResolutionTest test`

## 2025-12-30 — Interaction state gating
- Scope: Added InteractionState (awaiting dice) to gate input routing before conversation/commands and documented deterministic routing order.
- Tests: `mvn -q -Dtest=TranslatorServiceTest,MentionResolutionTest,InteractionStateTest test`

## 2025-12-30 — v1.0 deep clean
- Scope: Deep clean sweep to align docs/README/runbook with current narration exit rules, interaction gating, and conversation routing; archived conversation TODO list.
- Tests: `mvn -q test`

## 2025-12-30 — Quit-to-menu + coverage lift
- Scope: Quit now returns to the main menu, menu quit exits the program; added CLI flow/helper tests plus RuntimeLoader/AiJson coverage to push engine.cli and ai.client over 80%.
- Tests: `mvn -q test`

## 2026-01-02 — Key expression tests + runtime/integrity helpers
- Scope: Split KeyExpressionEvaluatorTest into focused suites with shared helpers; added SmartActorRuntime command/snapshot helpers and integrity simulation action/state helpers.
- Tests: `mvn -q -Dtest='KeyExpression*Test,SmartActorRuntimeTest,SmartActorCombatPlaybookIntegrationTest,GameIntegrityCheckCoverageTest,GameIntegrityTest' test`

## 2026-01-02 — Integrity key-expression helpers
- Scope: Extracted key-expression spec/AST/validator helpers for integrity checks and kept validation behavior the same.
- Tests: `mvn -q -Dtest=GameIntegrityCheckCoverageTest,GameIntegrityTest test`

## 2026-01-02 — Runtime combat/use helpers + win evaluation
- Scope: Split GameRuntime combat/use logic into helpers, added win-requirement evaluation helper with tests, and extracted smart-actor history recording.
- Tests: `mvn -q -Dtest=GameRuntimeCoverageTest,SmartActorRuntimeTest,GameIntegrityCheckCoverageTest,GameIntegrityTest,IntegrityWinRequirementEvaluatorTest test`

## 2026-01-02 — Runtime command actions + combat edge cases
- Scope: Moved explore/open/put into RuntimeCommandActions and added combat edge-case tests.
- Tests: `mvn -q -Dtest=GameRuntimeCoverageTest,GameCliCombatErrorTest,RuntimeCombatEdgeCaseTest,SmartActorRuntimeTest,GameIntegrityCheckCoverageTest,IntegrityWinRequirementEvaluatorTest test`

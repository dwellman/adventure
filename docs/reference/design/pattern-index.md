# Pattern Index (Adventure)

## Grounding
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java` - `TranslatorService.buildTranslatorPrompt(...)` (fixtures/items/inventory + scene context)
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java` - `TranslatorService.translate(...)` (fixtures/items/inventory + scene context)
- `src/main/java/com/demo/adventure/ai/runtime/NarratorPromptBuilder.java` - `NarratorPromptBuilder.buildEngine(...)` (raw engine output + scene snapshot)
- `src/main/java/com/demo/adventure/ai/runtime/NarratorPromptBuilder.java` - `NarratorPromptBuilder.buildSnapshot(...)` (raw engine output + scene snapshot)
- `src/main/java/com/demo/adventure/ai/runtime/dm/DmNarrator.java` - `DmNarrator.describe(...)` (deterministic Describe base before AI rewrite)
- `src/test/java/com/demo/adventure/authoring/GameMenuStructuredLoadTest.java` - `assertStructuredGameLoads(...)` (structured loader as ground truth)
- `src/main/resources/agents/translator.md` - translation rules and command surface

## Orchestration
- `src/main/java/com/demo/adventure/engine/cli/GameCli.java` - `GameCli.walkabout(...)` (local parse first, single translation pass)
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java` - `TranslatorService.translate(...)` (prompt -> LLM -> parse)
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java` - `NarrationService.narrateEngine(...)`
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java` - `NarrationService.narrateSnapshot(...)`
- `src/main/java/com/demo/adventure/ai/runtime/CommandTranslator.java` - `CommandTranslator.translate(...)`
- `src/main/java/com/demo/adventure/ai/runtime/NarratorService.java` - `NarratorService.rewrite(...)`
- `src/main/java/com/demo/adventure/authoring/gardener/Gardener.java` - `Gardener.garden(...)` (deterministic checks then AI expander)

## Verification
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java` - `parseTranslationOutput(...)` (single-line command output)
- `src/main/java/com/demo/adventure/authoring/save/build/GameSaveAssembler.java` - `apply(...)` (build + validation, fail loud)
- `src/main/java/com/demo/adventure/authoring/gardener/GardenerPatchValidator.java` - `validate(...)` (caps, IDs, warnings)
- `src/main/java/com/demo/adventure/authoring/cli/GardenerCli.java` - `run(...)` (validation + PASS/WARN coverage)
- `src/main/java/com/demo/adventure/authoring/cli/GardenerCli.java` - `computeCoverage(...)` (validation + PASS/WARN coverage)
- `src/main/java/com/demo/adventure/authoring/cli/ZoneBuilderCli.java` - `validate(...)` (world build + validator report)
- `src/test/java/com/demo/adventure/authoring/GameMenuStructuredLoadTest.java` - `assertStructuredGameLoads(...)` (verification of structured load)

## Trust UX
- `src/main/java/com/demo/adventure/engine/cli/GameCli.java` - `main(...)` (debug suppression)
- `src/main/java/com/demo/adventure/engine/cli/GameCli.java` - `GameCli.walkabout(...)` (AI disabled fallback path)
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java` - `narrateEngine(...)` (debug visibility + fallbacks)
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java` - `narrateSnapshot(...)` (debug visibility + fallbacks)
- `src/main/java/com/demo/adventure/ai/runtime/NarratorService.java` - `rewrite(...)` (explicit config + debug logging)
- `src/main/java/com/demo/adventure/ai/runtime/dm/AiDmAgent.java` - `narrate(...)` (fail-safe null for fallback)
- `src/main/java/com/demo/adventure/authoring/gardener/ai/AiFixtureDescriptionExpander.java` - `expand(...)` (fallback to deterministic expander)
- `src/main/java/com/demo/adventure/ai/runtime/dm/DmAgent.java` - `DmAgent.narrate(...)` (contract requires deterministic fallback)

## Learning
- `src/main/resources/agents/player.md` - OODA loop + introspection loops guidance (thinking styles + loop counts)
- `src/main/java/com/demo/adventure/authoring/gardener/ai/AiFixtureDescriptionExpander.java` - `applyResponse(...)` (uses history and records new descriptions)
- `src/main/java/com/demo/adventure/domain/model/Thing.java` - `getDescriptionHistory(...)` (description history as feedback)
- `src/main/java/com/demo/adventure/domain/model/Thing.java` - `recordDescription(...)` (description history as feedback)
- `src/main/java/com/demo/adventure/authoring/gardener/GardenerPatch.java` - `GardenerPatch.Metadata` (modelId, promptVersion, worldFingerprint)
- `src/main/java/com/demo/adventure/authoring/gardener/WorldFingerprint.java` - `fingerprint(...)` (pair patches with a world snapshot)

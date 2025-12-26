# Pattern Index (Adventure)

## Grounding
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java:86` - `TranslatorService.buildTranslatorPrompt(...)` (fixtures/items/inventory + scene context)
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java:41` - `TranslatorService.translate(...)` (fixtures/items/inventory + scene context)
- `src/main/java/com/demo/adventure/ai/runtime/NarratorPromptBuilder.java:8` - `NarratorPromptBuilder.buildEngine(...)` (raw engine output + scene snapshot)
- `src/main/java/com/demo/adventure/ai/runtime/NarratorPromptBuilder.java:30` - `NarratorPromptBuilder.buildSnapshot(...)` (raw engine output + scene snapshot)
- `src/main/java/com/demo/adventure/ai/runtime/dm/DmNarrator.java:34` - `DmNarrator.describe(...)` (deterministic Describe base before AI rewrite)
- `src/test/java/com/demo/adventure/authoring/GameMenuStructuredLoadTest.java:33` - `assertStructuredGameLoads(...)` (structured loader as ground truth)
- `src/main/resources/agents/translator.md:59` - translation rules and command surface

## Orchestration
- `src/main/java/com/demo/adventure/engine/cli/GameCli.java:210` - `GameCli.walkabout(...)` (local parse first, single translation pass)
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java:31` - `TranslatorService.translate(...)` (prompt -> LLM -> parse)
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java:28` - `NarrationService.narrateEngine(...)`
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java:63` - `NarrationService.narrateSnapshot(...)`
- `src/main/java/com/demo/adventure/ai/runtime/CommandTranslator.java:27` - `CommandTranslator.translate(...)`
- `src/main/java/com/demo/adventure/ai/runtime/NarratorService.java:27` - `NarratorService.rewrite(...)`
- `src/main/java/com/demo/adventure/authoring/gardener/Gardener.java:47` - `Gardener.garden(...)` (deterministic checks then AI expander)

## Verification
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java:63` - `parseTranslationOutput(...)` (single-line command output)
- `src/main/java/com/demo/adventure/authoring/save/build/GameSaveAssembler.java:30` - `apply(...)` (build + validation, fail loud)
- `src/main/java/com/demo/adventure/authoring/gardener/GardenerPatchValidator.java:19` - `validate(...)` (caps, IDs, warnings)
- `src/main/java/com/demo/adventure/authoring/cli/GardenerCli.java:64` - `run(...)` (validation + PASS/WARN coverage)
- `src/main/java/com/demo/adventure/authoring/cli/GardenerCli.java:274` - `computeCoverage(...)` (validation + PASS/WARN coverage)
- `src/main/java/com/demo/adventure/authoring/cli/ZoneBuilderCli.java:549` - `validate(...)` (world build + validator report)
- `src/test/java/com/demo/adventure/authoring/GameMenuStructuredLoadTest.java:33` - `assertStructuredGameLoads(...)` (verification of structured load)

## Trust UX
- `src/main/java/com/demo/adventure/engine/cli/GameCli.java:81` - `main(...)` (debug suppression)
- `src/main/java/com/demo/adventure/engine/cli/GameCli.java:210` - `GameCli.walkabout(...)` (AI disabled fallback path)
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java:28` - `narrateEngine(...)` (debug visibility + fallbacks)
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java:63` - `narrateSnapshot(...)` (debug visibility + fallbacks)
- `src/main/java/com/demo/adventure/ai/runtime/NarratorService.java:27` - `rewrite(...)` (explicit config + debug logging)
- `src/main/java/com/demo/adventure/ai/runtime/dm/AiDmAgent.java:25` - `narrate(...)` (fail-safe null for fallback)
- `src/main/java/com/demo/adventure/authoring/gardener/ai/AiFixtureDescriptionExpander.java:97` - `expand(...)` (fallback to deterministic expander)
- `src/main/java/com/demo/adventure/ai/runtime/dm/DmAgent.java:16` - `DmAgent.narrate(...)` (contract requires deterministic fallback)

## Learning
- `src/main/resources/agents/player.md:132` - OODA loop + introspection loops guidance (thinking styles + loop counts)
- `src/main/java/com/demo/adventure/authoring/gardener/ai/AiFixtureDescriptionExpander.java:176` - `applyResponse(...)` (uses history and records new descriptions)
- `src/main/java/com/demo/adventure/domain/model/Thing.java:183` - `getDescriptionHistory(...)` (description history as feedback)
- `src/main/java/com/demo/adventure/domain/model/Thing.java:189` - `recordDescription(...)` (description history as feedback)
- `src/main/java/com/demo/adventure/authoring/gardener/GardenerPatch.java:16` - `GardenerPatch.Metadata` (modelId, promptVersion, worldFingerprint)
- `src/main/java/com/demo/adventure/authoring/gardener/WorldFingerprint.java:22` - `fingerprint(...)` (pair patches with a world snapshot)

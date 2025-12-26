# AI/CLI Runbook (Authoritative)

Status: Authoritative (runtime + CLI contract)
Last verified: 2025-12-26

## Purpose
Define the runtime contract for translator -> engine -> narrator, including classic fallback and prompt wiring.

## Inputs
- Structured game YAML, runtime prompts, and configuration (see "Inputs and configuration").
## Outputs
- Raw engine output and (when enabled) narrated output; smart-actor turn snapshot when configured.

## Tests
- See "Validation gates" for the standard test commands.

## Scope
- AI-enabled CLI flow (2025) and classic fallback (1980).
- Translator/Engine/Narrator role contract and invariants.
- Prompt locations, configuration, and debug toggles.

## Modes
- **1980 (classic):** AI disabled; commands run through scanner/compiler/interpreter only.
- **2025 (AI):** translator + narrator enabled; requires `OPENAI_API_KEY`. Missing key disables AI with a warning and keeps the classic path.

## Inputs and configuration
- Structured game definition: `src/main/resources/games/<id>/game.yaml`
- Game backstory (required by GameCli): `src/main/resources/games/<id>/narrative/backstory.md`
- Storybook bundle (if present): `src/main/resources/storybook/<id>/`
- Smart actor specs (optional): `src/main/resources/games/<id>/world/smart-actors.yaml` and tag metadata in `src/main/resources/games/<id>/motif/tags.yaml`
- Runtime prompt templates: `src/main/resources/agents/translator.md`, `src/main/resources/agents/narrator.md`
- Smart actor prompt template: `src/main/resources/agents/smart-actor-system.md`
- Config sources (priority): JVM props > env vars > `application.properties`
- Translator config: `ai.translator.model`, `ai.translator.temperature`, `ai.translator.top_p`, `ai.translator.logprobs`, `ai.translator.top_logprobs`, `ai.translator.debug`
- Narrator config: `ai.narrator.model`, `ai.narrator.temperature`, `ai.narrator.top_p`, `ai.narrator.logprobs`, `ai.narrator.top_logprobs`, `ai.narrator.debug`

## Constraints
- Structured load must fail loudly; no silent fallback for structured files.
- Translator output must be a single-line command string (validated before execution).
- Narrator prompt uses a single template (no style override fields).
- Classic fallback runs only when AI is disabled and only when the compiler cannot parse the input; it must use scanner tokens only.
- Smart actors run only in 2025 mode with AI enabled; during combat they are limited to ATTACK/FLEE and otherwise pass with a hesitation.

## Role contract (2025)
- **Translator:** deterministic mapper from player text to a canonical command. Inputs: player text, visible fixtures/items, inventory, last scene text. Output: one command only. No questions or colors.
- **Engine:** authoritative state transition + factual turn snapshot (location, fixtures/items/actors, exits).
- **Narrator:** stateless rewrite of engine output with tone; cannot add exits/items/fixtures/objectives or change state.
- **Smart actors:** operate after player turn using the same interpreter/handlers; combat verbs restricted to ATTACK/FLEE.

## Runtime flow (AI enabled)
1) Player input → local parse (scanner/compiler/interpreter).
2) If parse fails and AI enabled → TranslatorService.
3) TranslationOrchestrator validates translated command via CommandInterpreter (single pass).
4) Engine executes the command and emits raw output + snapshot.
5) Narrator rewrites engine output using BACKSTORY (no mechanics added).
6) Smart actors (if enabled) may act after the player turn.
7) Error path: translation failures request a rephrase via HELP; narration failures or AI-disabled paths emit raw engine output.

## Validation gates
- Structured load guard: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Deterministic integrity sweep: `mvn -q -Dtest=GameIntegrityTest -DrunIntegrity=true test`
- Adventure playbook integration: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,MansionAdventurePlaybookIntegrationTest,WesternAdventurePlaybookIntegrationTest,SpyAdventurePlaybookIntegrationTest test`
- Minigame regression: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest test`
- Smart-actor deterministic runs: `mvn -q -Dtest=SmartActorCombatPlaybookIntegrationTest test`
- Broad sweep: `mvn -q test`

## Verification checklist
- Backstory present in each structured game (`narrative/backstory.md`).
- Translator output is single-line and parser-valid.
- Narrator always includes exits and does not invent mechanics.
- Inventory/crafting/combat routes through scanner/compiler/interpreter.
- Receipts emitted for command actions (cell ops, combat events).

## References
- AI roles and future roadmap: `docs/design/ai-roles.md`
- CLI architecture overview: `docs/design/cli.md`
- Engine architecture: `docs/design/engine.md`

# Game Testing Strategy (Agent Validation)

Last verified: 2026-01-02

## Purpose
Define the repeatable testing and validation workflow for each game.
## Inputs
- Playbooks, minigames, and structured game YAML.
## Outputs
- Test results, transcripts, and logged blockers.
## Tests
- See "Automated gates" for the standard commands.
## Goals
- Catch mechanical regressions before play.
- Keep 1980 mechanics and 2025 AI loops aligned with the parser/interpreter.
- Produce repeatable evidence (tests + transcripts) for each game.

## Primary artifacts
- Playbook YAML/MD for each full game run (expected outputs in YAML, narrative notes in MD).
- Minigame playbooks under `src/test/resources/minigames/` for focused mechanics.
- Playtest transcripts under `docs/process/playtests/` (ASCII-normalized).
- BUJO daily log + journal scope entries for evidence and traceability.

## Automated gates (run from the repo root)
1) Structured load guard:
   - `mvn -q -Dtest=GameMenuStructuredLoadTest test`
2) Deterministic integrity check (no LLM):
   - `mvn -q -Dtest=GameIntegrityTest -DrunIntegrity=true test`
   - Note: the integrity test is skipped unless `-DrunIntegrity=true` (or `RUN_INTEGRITY=true`) is set.
   - Note: search is bounded by `GameIntegrityConfig.defaults()`; raise limits for deeper reachability sweeps.
3) Adventure playbook integration:
   - `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,MansionAdventurePlaybookIntegrationTest,WesternAdventurePlaybookIntegrationTest,SpyAdventurePlaybookIntegrationTest test`
   - Note: Island playbooks use `src/test/resources/games/island-adventure-test/game.yaml` with a reduced clock gate for deterministic win checks.
4) Minigame regression:
   - `mvn -q -Dtest=MiniGamePlaybookIntegrationTest test`
5) Smart-actor deterministic runs (if enabled):
   - `mvn -q -Dtest=SmartActorCombatPlaybookIntegrationTest test`
6) Broad sweep when many files change:
   - `mvn -q test`

## Manual walkthrough protocol (1980 mode)
Use this when validating full game pacing or when playbooks are incomplete.

1) Start the CLI: `./adventure --mode=1980 --quiet`
2) Use a natural command flow:
   - `look`, `inventory`, `search` at each new plot.
   - `inspect <thing>` and `open <thing>` when descriptions hint at it.
   - `take <item>` then `inspect` to collect clue text.
   - `use <item>` and `use <item> on <thing>` to validate gating.
3) Track blockers:
   - If a description implies an item/fixture but `take`/`open`/`use` says "You don't see that here" or "No such item here," log it as a mechanical mismatch.
   - Stop after 10-15 unproductive turns and record the blocker.

### Capture transcript (agent workflow)
- Run in a detached `screen` session so output is captured in one file.
- Save to `docs/process/playtests/YYYY-MM-DD-<game>-1980-walkthrough.md`.
- Normalize to ASCII if needed (strip control codes before committing).
- Record key steps and the final outcome (win/lose/blocked).

## AI-mode validation (2025 mode)
Use this to validate translator/narrator correctness without changing mechanics.

- Confirm local parse runs first; translator only runs when parse fails.
- Translator must return a single-line command that the parser accepts.
- Narrator must include exits and must not invent items/fixtures/exits.
- If AI is disabled or `OPENAI_API_KEY` is missing, ensure the classic path runs and no translator/narrator output appears.

## Smart-actor validation
- Use playbooks with `smartActorDecisions` and `smartActorExpectations` so output is deterministic.
- Keep decisions within each smart actor's `allowedVerbs` list.
- During combat, smart actors should ATTACK or FLEE; if they hesitate unexpectedly, check player targeting and ownership updates.

## Reporting checklist
For each failure, capture:
- Game + plot label.
- Command issued.
- Expected vs actual output.
- Inventory state (if relevant).
- Whether the failure was playbook-based or manual.

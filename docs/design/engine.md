# Engine Architecture (TODAY → TOMORROW → SOMEDAY)

## TODAY (working)
- **Responsibility:** Execute canonical commands, update world state, and emit authoritative turn results.
- **Core types:** `GameSave`, `WorldRecipe`, `KernelRegistry`, `Thing/Plot/Item/Gate`, `KeyExpressionEvaluator`, `ContainerPacker`.
- **Ops:** Movement (`Move`), inspection (`Look/Listen/Describe/Search`), container open/close (`Open/Close`), crafting (`CraftingTable` + `CraftingRecipeLoader`), gating via key expressions.
- **Loading:** Structured games (`src/main/resources/games/<id>/game.yaml` + map/fixtures/items/actors/descriptions) via `StructuredGameSaveLoader`; no silent fallback to monolithic unless explicitly non-structured.
- **Invariants:** Engine output is the only source of mechanical truth per turn; no AI bypass. Capacity, visibility, key expressions enforced centrally.
- **Tests:** Integration tests per world (e.g., Island/Mansion), kernel op tests, structured load smoke (`GameMenuStructuredLoadTest`).
- **Patterns:** Grounding (structured-first, engine truth), Orchestration (single command → deterministic ops), Verification (tests/receipts), Trust UX (no hidden mechanics), Learning (future judge/transcript backlog).
- **Testing patterns:** See `docs/design/pattern-tests.md` for grounding/orchestration/verification/trust-UX/learning test strategies.

### Text diagram (runtime flow)
```
Player/Interpreter (cmd) --> Engine Ops --> KernelRegistry
                                |              |
                                v              v
                         Turn Result ------> Narrator
```

## TOMORROW (in motion)
- **Validation hardening:** Expand structured-load validation coverage (fixtures/items ownership, gate consistency) across all games; add CI guardrails.
- **Replay receipts:** Deterministic replay of transcripts for debugging demos/tests.
- **World diffs:** Lightweight state diffing for regression checks and gardener validation.

## SOMEDAY (planned)
- **Turn scheduler:** Multi-actor turn orchestration (players + NPC/AI characters) that routes all actions through the same command boundary.
- **Ambient events:** Explicit non-mechanical ambient layer that cannot change state.
- **AI guardrails:** Judge checks on engine-facing inputs/outputs to reject mechanic violations before narration.
- **MUD multiplayer:** Multi-session engine with shared state, turn arbitration, and per-actor command queues.
- **DB backing:** Persisted saves, transcripts, and replay receipts in a database; deterministic reloads.
- **REST API:** Engine/CLI surfaces as HTTP endpoints for play, authoring, and replay.

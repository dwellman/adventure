# CLI Architecture (TODAY → TOMORROW → SOMEDAY)

Authoritative AI/CLI runtime runbook:
- `docs/runbooks/ai-cli.md`

## TODAY (working)
- **Game CLI:** `com.demo.adventure.engine.cli.GameCli` (`./adventure`) presents menu, loads structured `game.yaml`, runs classic (`--mode=1980`) or AI (`--mode=2025`).
- **Package split:** `engine` (cli/runtime/flow/mechanics/command), `ai` (runtime + authoring AI), `domain` (model/kernel/save schema), `authoring` (cli/save/lang/gardener/zone/samples).
- **AI integration:** Runtime translator/narrator templates and role prompts live in `src/main/resources/agents/` (some agent prompts are not wired into GameCli yet); `application.properties` controls debug/model/temperature/top_p/logprobs. Interpreter deterministic; Narrator tone-only.
- **Builder tools:** `GameBuilderCli`, `ArchitectCli`, `ZoneBuilderCli`, `GardenerCli`, `GameStructExporter`, `GamePlanCli` with wrappers (`./builder`). Use YAML + structured loader; no legacy prompt paths.
- **Inputs:** Always go through `CommandScanner`/`TokenType` normalization; per-game `motif/aliases.yaml` can extend scanner keywords; no hand parsing.
- **Invariants:** Player-facing output must stay free of debug spew; no hardcoded game logic in CLI; AI enabled only with `--mode=2025` and `OPENAI_API_KEY`.
- **Patterns:** Grounding (structured loads), Orchestration (Interpreter → Engine → Narrator, one step per tick), Verification (tests/receipts), Trust UX (debug off, no hidden mechanics), Learning (BUJO + future judges/golden transcripts).
- **Testing patterns:** See `docs/design/pattern-tests.md` for grounding/orchestration/verification/trust-UX/learning test strategies.

### Text diagram (CLI surface)
```
./adventure (1980/2025)
   |
   +--> GameCli --> RuntimeLoader/LoopRuntime --> Engine
   |
./builder
   |- GameBuilderCli (validate/write)
   |- ArchitectCli (plots -> YAML)
   |- ZoneBuilderCli (zone -> YAML)
   |- GardenerCli (desc patch)
   |- GamePlanCli (AI-assisted authoring pipeline)
```

## TOMORROW (in motion)
- **Authoring prompts:** Add Storyteller/Planner/Gardener prompts (generic) under `src/main/resources/agents/` when ready; keep CLIs as the authoritative path until then.
- **UX polish:** Clearer AI diagnostics flags, menu surfacing of AI mode, and structured-load validation hints.
- **Docs:** Keep `docs/design/ai-cli-mode.md` aligned with properties and prompt versions.

## SOMEDAY (planned)
- **Demo runner:** Scripted “tour” driver that feeds player-like inputs through CLI for reproducible talks.
- **Multi-actor CLI:** Turn scheduler UI for NPC/AI character turns, still honoring the same Interpreter → Engine boundary.
- **Judge hooks:** CLI-level validation to block AI outputs that attempt mechanic changes before reaching Engine/Narrator.
- **MUD/admin controls:** CLI/API affordances for multiplayer sessions (lobbies, turns, observers).
- **REST API:** HTTP surface for play/authoring/replay alongside CLI.

# Design Overview (TODAY → TOMORROW → SOMEDAY)

## TODAY (working)
- **Runtime flow:** Interpreter/translator (`--mode=2025`) → Engine → Narrator; classic CLI (`--mode=1980`) skips AI. Runtime templates and role contracts live in `src/main/resources/agents/` (some agent prompts are not wired into GameCli yet); debug off by default via `application.properties`.
- **Content model:** Structured games under `src/main/resources/games/<id>/` (`game.yaml` + map/fixtures/items/actors/descriptions) loaded by `StructuredGameSaveLoader` with no silent fallback.
- **CLIs:** GameCli (play), Builder/Architect/ZoneBuilder/Gardener/GameStructExporter for YAML validation/generation. Wrapper scripts `./adventure`, `./builder`.
- **Testing:** `GameMenuStructuredLoadTest` ensures each menu game loads; `mvn -q test` for the full suite.

## TOMORROW (in motion)
- **Authoring agents (conceptual):** Storyteller → World Planner → Gardener prompts to generate Game Spec → World Plan → description overlay. Until prompts land, use CLIs/YAML authoring.
- **Docs to refresh:** `docs/reference/design/kernel.md`, `docs/reference/design/pipeline.md` for current kernel/pipeline notes; align them with structured-game-first flows when updated.

## SOMEDAY (planned)
- **In-world agents:** Smart NPCs / AI characters / demo-mode actor issuing moves through Interpreter → Engine (no narrator fiat).
- **Artifacts:** Formalize authoring outputs (Game Spec/World Plan/description stack) and prompt versions with receipts; add CI guardrails for prompt drift.

## Pointers
- AI/CLI runbook (authoritative): `docs/howto/runbooks/ai-cli.md`
- AI roles/contracts: `docs/reference/design/ai-roles.md`
- Engine: `docs/reference/design/engine.md`
- CLI: `docs/reference/design/cli.md`
- Content pipeline: `docs/reference/design/content.md`
- DSL spec: `docs/reference/design/dsl.md`
- Key expression syntax: `docs/reference/design/key-expression.md`
- Kernel design: `docs/reference/design/kernel.md`
- Pipeline design: `docs/reference/design/pipeline.md`
- Parser grammar: `docs/reference/design/parser.bnf`
- AI CLI mode (superseded): `docs/reference/design/ai-cli-mode.md`
- Smart actors: `docs/reference/design/smart-actors.md`
- BUJO: `docs/process/bujo/readme.md`, daily logs in `docs/process/bujo/daily/`

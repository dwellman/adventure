## Player CLI parsing and crafting
- Always rely on `CommandScanner` + `TokenType` to normalize verbs; do not hand-parse player input. `CRAFT` is normalized to the `MAKE` token; the CLI switches on `TokenType.MAKE` to route to crafting.
- Do not use heuristics or ad-hoc parsing/normalization in the AI path. Always use the existing scanner/compiler/interpreter logic (e.g., `CommandScanner`, `CommandCompiler`, `Direction.parse`) instead of rolling custom rules.
- Classic (1980) mode may use a deterministic fallback, but it must be isolated from the AI path, must only use `CommandScanner` tokens, and should run only when the compiler cannot parse the input.
- Crafting verbs should call `CraftingTable` (with recipes loaded from YAML via `CraftingRecipeLoader` when available) instead of implementing bespoke crafting logic in the CLI.
- Keep help text in sync with supported commands; if a verb is not implemented, remove it from help rather than silently accepting it.
- After crafting, refresh the inventory list from the registry so the new item appears and consumed ingredients disappear. See `GameCliCraftTest` for the guardrail.

## Crafting recipes live in YAML
- Use `CraftingRecipeLoader` to read `crafting.yaml` (e.g., under `src/main/resources/games/<id>/`) and pass recipes into `CraftingTable`. The default hardcoded recipes are only a fallback for tests. Do not re-embed game-specific recipes in Java.

## Visibility vs. key expressions
- `Thing.isOpen()` and `Thing.isVisible()` both use the `key` expression; visibility also respects the `visible` flag. To gate visibility (e.g., HAS("Lit Torch")), set `visible: true` and `keyString` to your condition. Setting `visible: false` hides the thing regardless of key.

## Use existing mechanics for randomness/gating
- When handling player actions (SEARCH, movement, etc.), prefer the existing key-expression + DICE evaluation (`KeyExpressionEvaluator`) instead of ad-hoc randomness. Don’t replace kernel ops with manual `ThreadLocalRandom` calls.

## Suppress debug/output leakage
- Player-facing output should not include BOM/dev spew or raw key-expression debug traces. If you need diagnostics, guard them behind a debug flag or keep them out of normal CLI flow.
- `KeyExpressionEvaluator` keeps debug logging available for tests; `GameCli` turns it off at startup. Do not re-enable debug logging in the player CLI.

## AI CLI roles (2025 mode)
- Translator: deterministic intent router. Inputs: player text, visible fixtures/items, inventory, previous error, last scene text. No backstory. Outputs one command, one clarifying question, or a color passthrough. No invention/heuristics; if unsure, ask a question.
- Engine: authoritative state and mechanics. Runs the command, updates the world, and emits a factual snapshot (location, fixtures/items/actors, exits). No AI influence here.
- Narrator: stateless rewrite layer. Inputs: engine output/snapshot, BACKSTORY, LAST_COMMAND, STYLE. Keeps mechanics factual (no new fixtures/items/exits/state changes), but can add brief atmospheric flavor and one short reaction to playful commands in sparse scenes. Always includes exits; fixtures/items only if present.
- If translator returns a question, engine/narrator are skipped until the player answers. Color passthrough narrates the last scene plus the color line.

## Respect structured vs. monolithic loads
- When loading `game.yaml` from `src/main/resources/games/<id>/`, fail loudly if structured load breaks rather than silently falling back to the monolithic loader. Fallback is only appropriate for non-structured files.

## Auto-synth gates
- The map assembler will synthesize open/visible gates between adjacent plots that lack a declared gate. This is intentionally “optimistic” movement and should only fill gaps; declare explicit gates in YAML when you need custom behavior or blocking key expressions.

## BUJO + housekeeping
- Log each working day under `docs/bujo/daily/<YYYY-MM-DD>.md` (What I touched / Notes-Risks / Tests / Next) and add a scope/tests note in `docs/journal.md`. Roll up to `docs/bujo/weekly/` or `monthly/` when applicable.
- Migrate unfinished bullets forward and prune stale ones. If you add docs, update the docs index (if present). Record sweeps/refactors in a housekeeping log (`docs/housekeeping/cleaning.log` if it exists).
- Keep AI debug off for players by default; note any metadata changes (IDE regeneration, wrapper tweaks) and keep Maven coords in `readme.md` in sync.

## AI interaction model (TODAY → TOMORROW → SOMEDAY)

### TODAY (working now)
- **Runtime loop:** Interpreter/translator (only in `--mode=2025`) → Engine → Narrator. Classic `--mode=1980` skips AI. Translator/Narrator prompts live in `src/main/resources/agents/`; debug toggles come from `application.properties`.
- **Invariants:** Interpreter is deterministic (no invention); Engine is the source of truth; Narrator may add tone but must not add exits/items/fixtures/objectives or new mechanics.
- **Content:** Structured games load from `src/main/resources/games/<id>/game.yaml` with includes; no silent fallbacks. Builder/Architect/ZoneBuilder/Gardener CLIs operate on YAML for validation/generation.
- **Proof:** Runtime traces show `~ translator command:` and narrator raw/rewritten output; structured load test (`GameMenuStructuredLoadTest`) and full suite via `mvn -q test`.

### TOMORROW (not yet complete, in motion)
- **Pregame authoring agents (conceptual):** Storyteller (interview to Game Spec), World Planner (plots/graph, Zork-era labels), Gardener (description overlay without structural changes). Use existing CLIs/YAML until prompts for these roles are built/refreshed under `src/main/resources/agents/`.
- **Authoring outputs (conceptual):** Game Spec → World Plan → description stack. Keep them generic/mechanically accurate; do not embed game-specific lore in prompts when implemented.

### SOMEDAY (planned, not developed)
- **Smart NPCs / AI characters:** In-world agents that must execute moves through the Interpreter → Engine path; no narrator fiat.
- **Demo-mode main actor:** Drives turns with persona/scripted goals but still routes through Interpreter → Engine → Narrator.

## Prompt placement and guardrails
- Prompts belong under `src/main/resources/agents/`; do not resurrect legacy `src/prompt/` paths.
- Keep prompts generic across games; avoid embedding lore, items, or missions. Enforce the mechanical contract (no invented exits/items/requirements) in runtime prompts; allow voice/tone only.
- Hero’s journey is a story-shaping constraint for Storyteller/Gardener/Narrator; cozy games are the explicit exception. Interpreter/Engine must not enforce narrative arcs.

## Testing expectations
- Run targeted tests for your change; default to `mvn -q test` for broad changes. Record commands in BUJO entries.
- When touching structured game YAML, at minimum run `mvn -q -Dtest=GameMenuStructuredLoadTest test` to catch load regressions.

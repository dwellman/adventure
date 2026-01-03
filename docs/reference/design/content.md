# Content Pipeline (TODAY → TOMORROW → SOMEDAY)

## TODAY (working)
- **Sources:** Structured game bundles under `src/main/resources/games/<id>/` with `game.yaml` + `world/map.yaml` + `world/fixtures.yaml` + `world/items.yaml` + `world/actors.yaml` + `narrative/descriptions.yaml` + `narrative/backstory.md` + optional `motif/aliases.yaml` (verb aliases) and `motif/tags.yaml` (smart-actor tag sources). Canonical monolithic saves remain in `src/main/resources/cookbook/` for reference.
- **Loading:** `StructuredGameSaveLoader` assembles `GameSave` with no silent fallback to monolithic (unless explicitly non-structured).
- **Tools:** `GameStructExporter` (monolithic → structured), `GameBuilderCli` (validate/build/write), `ArchitectCli` (plots → YAML), `ZoneBuilderCli` (zone graph → YAML), `GardenerCli` (description patch).
- **Rules:** Keys must be respected (`startPlotKey`, `ownerKey`, gate `from/to`); no recomputing keys from names. Crafting recipes live in YAML under `world/` and feed `CraftingTable`. Verb aliases live in `motif/aliases.yaml` and feed `CommandScanner` keywords.
- **Tests:** Integration tests per game; structured load smoke (`GameMenuStructuredLoadTest`).
- **Patterns:** Grounding (structured-first content), Orchestration (CLIs drive authoring), Verification (load + integration tests), Trust UX (fail loud on missing/invalid content), Learning (future judges/golden transcripts).
- **Testing patterns:** See `docs/reference/design/pattern-tests.md` for grounding/orchestration/verification/trust-UX/learning test strategies.

### Text diagram (content flow)
```
Author YAML (structured) --> StructuredGameSaveLoader --> GameSave --> Engine/CLI
Monolithic YAML --------> GameStructExporter -----------^
Architect/ZoneBuilder/Gardener CLIs -> produce/patch YAML
```

## TOMORROW (in motion)
- **Authoring prompts:** Storyteller/Planner/Gardener prompts to generate/augment content; keep outputs mechanically consistent and game-agnostic.
- **Validation:** Expand coverage for ownership, gate consistency, and description overlays; add CI checks for structured bundles across all games.
- **Docs alignment:** Refresh `docs/reference/design/kernel.md` and `docs/reference/design/pipeline.md` to reflect structured-first workflow.

## SOMEDAY (planned)
- **Content completion gate:** Define “done” per game (world pass + garden pass + demo transcript).
- **Character sheets (data, not prompt lore):** Personas/goals/constraints in world data to support AI characters without prompt lore.
- **NPC/Character memory snippets:** Scoped, non-mechanical memory to guide dialogue/tone.
- **Smart actor metadata:** `world/smart-actors.yaml` per game (promptId, backstory, persona/properties, memory + history seeds, policies) feeding AI-backed actors without altering mechanics.
- **Save/load + replay receipts:** Deterministic content + transcript replay for demos and debugging.
- **MUD/DB backing:** Persist structured bundles, saves, and transcripts in a database for multiplayer sessions.
- **REST API:** Content ingest/export endpoints alongside CLI tools.

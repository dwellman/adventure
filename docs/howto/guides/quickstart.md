# Getting Started (Developers)

## Prereqs
- Java 17, Maven.
- (Optional) `OPENAI_API_KEY` for AI mode.

## Build & test
- `mvn -q test` (full suite).
- Quick smoke: `mvn -q -Dtest=GameMenuStructuredLoadTest test`.

## Play the game
- Classic: `./adventure --mode=1980`.
- AI mode: `OPENAI_API_KEY=... ./adventure --mode=2025`.

## Tools
- Builder: `./builder <input.yaml> [--out FILE|--stdout] [--bom] [--report]`.
- Architect: `./builder architect --in src/test/resources/zone-demo/zone-input.sample.yaml --out logs/zone-game.yaml`.
- Zone Builder, Gardener, GameStructExporter: see `readme.md` for exact flags.

## Code layout
- Source: `src/main/java/com/demo/adventure/`
- Structured games: `src/main/resources/games/<id>/game.yaml` (+ map/fixtures/items/actors/descriptions).
- Prompts: `src/main/resources/agents/` (translator/narrator; keep generic).
- Scripts: `adventure`, `builder` wrappers.

## AI config
- `application.properties` (classpath or repo root) controls `ai.narrator.*` and `ai.translator.*` (model, temperature, top_p, logprobs, debug).
- Runtime modes: classic (no AI) or AI-assisted; debug off by default.

## BUJO & handoff
- Log daily work in `docs/process/bujo/daily/<YYYY-MM-DD>.md`; add scope/tests in `docs/process/journal.md`.
- Design index: `docs/reference/design/readme.md` (roles, engine, CLI, content).

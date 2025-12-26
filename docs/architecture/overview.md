# Architecture Overview

Purpose: provide a short map of the runtime and authoring surfaces without diving into implementation detail.

## CLI flow (1980 and 2025)
1. Player input flows through `CommandScanner` and `CommandCompiler`.
2. If compilation succeeds, the command executes through `GameCommandHandler` -> `GameRuntime`.
3. In 2025 mode, if compilation fails, the input is sent to `TranslatorService` which returns a single command string.
4. The translated command re-enters the same scanner/compiler/handler pipeline.
5. The engine output is narrated (AI rewrite in 2025, direct output in 1980).

## Package map
| Package | Responsibility | Notes |
| --- | --- | --- |
| `com.demo.adventure.engine.cli` | CLI entrypoints and menu selection | Loads structured `game.yaml` and wires runtime services. |
| `com.demo.adventure.engine.command` | Command parsing and handler dispatch | Uses `CommandScanner` + `CommandCompiler`; no hand parsing. |
| `com.demo.adventure.engine.runtime` | Runtime loop, command context, trigger resolution | Owns in-memory state for a running session. |
| `com.demo.adventure.engine.mechanics` | Mechanics (key expressions, gates, cells, triggers) | Uses `KeyExpressionEvaluator` and kernel ops. |
| `com.demo.adventure.engine.flow` | Loop timing and turn sequencing | Keeps tick/loop logic separate from command handlers. |
| `com.demo.adventure.engine.integrity` | Deterministic integrity checks | Opt-in reachability and validation for game YAML. |
| `com.demo.adventure.ai.runtime` | Translator + narrator services and prompt templates | AI-only, no game mechanics; prompts under `src/main/resources/agents/`. |
| `com.demo.adventure.ai.authoring` | AI helpers for authoring tools | Used by `GamePlanCli` and related tools. |
| `com.demo.adventure.authoring` | GDL/YAML authoring and builder CLIs | Produces structured game YAML and narrative assets. |
| `com.demo.adventure.domain` | Core game model and kernel registry | Shared by engine and authoring. |
| `com.demo.adventure.buui` | CLI formatting and console utilities | Word-wrap, markdown formatting, menu helpers. |

## Resource layout
- `src/main/resources/games/<id>/` contains structured world/narrative/motif assets used at runtime.
- `src/main/resources/agents/` contains translator/narrator prompts for 2025 mode.
- `src/test/resources/games/` contains test-only game bundles and minigame fixtures.

## Guardrails
- All commands, including directions, go through the scanner/compiler/handlers.
- Translator returns a single command string; it does not answer questions or emit narration.
- Narration rewrites are tone-only; mechanics and exits remain factual.

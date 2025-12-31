---
name: adventure-ai-runbook-review
description: Align AI CLI runbook and prompt documentation with the current GameCli flow, runtime prompt templates, and agent prompt placement.
---

# Adventure AI Runbook Review Skill

## Purpose
Keep AI CLI documentation aligned with the live translator -> engine -> narrator loop and prompt assets.

## When to use
- Updating AI CLI flow docs or prompt templates.
- Verifying translator/engine/narrator separation or classic fallback constraints.
- Auditing AI runtime configuration and debug outputs.

## Workflow
1. Read `src/main/java/com/demo/adventure/engine/cli/GameCli.java` and `src/main/java/com/demo/adventure/ai/runtime/TranslationOrchestrator.java`.
2. Check the translator template in `src/main/resources/agents/translator.md` and the compiled narrator prompt in `src/main/java/com/demo/adventure/ai/runtime/NarratorPromptBuilder.java`.
3. Review role/agent prompts in `src/main/resources/agents/` and note whether they are wired into GameCli 2025 flow.
4. Verify classic fallback behavior in `src/main/java/com/demo/adventure/engine/command/handlers/ClassicCommandFallback.java`.
5. Update `docs/runbooks/ai-cli.md`, `docs/design/ai-cli-mode.md`, and `docs/design/cli.md` to match actuals.
6. Check `docs/architecture/overview.md`, `docs/runbooks/index.md`, `docs/games/index.md`, `docs/glossary.md`, and `docs/adr/index.md` for alignment.
7. Record changes in BUJO daily, `docs/journal.md`, and update `docs/index.md` if new docs were added.

## Guardrails
- Do not edit the deferred CommandScanner unterminated quotes work.
- Avoid heuristic parsing; use CommandScanner/CommandCompiler/Direction.parse for parsing changes.
- Keep AI debug off for players by default.

## References
- `src/main/java/com/demo/adventure/engine/cli/GameCli.java`
- `src/main/java/com/demo/adventure/ai/runtime/TranslatorService.java`
- `src/main/java/com/demo/adventure/ai/runtime/NarrationService.java`
- `src/main/java/com/demo/adventure/ai/runtime/TranslationOrchestrator.java`
- `src/main/java/com/demo/adventure/engine/command/handlers/ClassicCommandFallback.java`
- `docs/runbooks/ai-cli.md`
- `docs/architecture/overview.md`
- `docs/runbooks/index.md`
- `docs/games/index.md`
- `docs/glossary.md`
- `docs/adr/index.md`

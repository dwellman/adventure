# Deep Clean Log

Last verified: 2025-12-30 (v1.0 deep clean)

Use this file to log deep-clean passes. Keep entries chronological and concise.

## Entry Format
```
## YYYY-MM-DD — <Scope> — <Owner>
- Scope: <what was reviewed/changed>
- Commands: <tests/commands; note blockers>
- Coverage: <tool/command or not available>
- Docs: <README/docs updates, Last verified stamps>
- Status: Pending / In Progress / Clean (date, owner)
- Risks/Follow-ups: <open items>
```

## 2025-12-21 — Repo-wide deep clean — Codex
- Scope: Reviewed repo structure and key packages, refreshed housekeeping docs/logs, updated README/docs for package layout, removed stale TODO behavior, and noted reserved scanner helper.
- Commands: `mvn -q test`
- Coverage: Not collected (no coverage tool configured).
- Docs: `readme.md`, `docs/readme.md`, `docs/housekeeping/*`, BUJO and journal updated.
- Status: Clean (2025-12-21, Codex)
- Risks/Follow-ups: `KeyExpressionScanner.readString` is reserved for upcoming string literal handling.

## 2025-12-25 — Spy Adventure playbook sweep — Codex
- Scope: Updated Spy Adventure playbook steps/expectations to follow explicit gates and cleaned stray log artifacts.
- Commands: `mvn -q -Dtest=SpyAdventurePlaybookIntegrationTest test`; `mvn -q test`
- Coverage: Not collected (no coverage tool configured).
- Docs: `src/test/resources/playbooks/spy-adventure/playbook.yaml`, `src/test/resources/playbooks/spy-adventure/playbook.md`, `docs/housekeeping/cleaning.log`, `docs/housekeeping/clean-log.md`, BUJO, journal updated.
- Status: Clean (2025-12-25, Codex)
- Risks/Follow-ups: None.

## 2025-12-25 — Island raft integrity sweep — Codex
- Scope: Added raft setup playbook, adjusted Island backpack capacity for raft carry, and widened integrity sweep defaults.
- Commands: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,GameIntegrityTest test`; `mvn -q test`
- Coverage: Not collected (no coverage tool configured).
- Docs: `src/test/resources/playbooks/island-adventure/playbook-raft.yaml`, `src/test/resources/playbooks/island-adventure/playbook-raft.md`, `docs/housekeeping/cleaning.log`, `docs/housekeeping/deep-clean-checklist.md`, BUJO, journal updated.
- Status: Clean (2025-12-25, Codex)
- Risks/Follow-ups: Integrity sweep still caps at 5,000 states; Island win remains warning until search strategy improves.

## 2025-12-25 — Island test-game win gate — Codex
- Scope: Added test-only Island game YAML with reduced clock gate, escape playbook, and documented the test gate.
- Commands: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,GameIntegrityTest test`; `mvn -q test`
- Coverage: Not collected (no coverage tool configured).
- Docs: `src/test/resources/playbooks/island-adventure/game.yaml`, `src/test/resources/playbooks/island-adventure/playbook-escape.yaml`, `src/test/resources/playbooks/island-adventure/playbook-escape.md`, `docs/housekeeping/cleaning.log`, `docs/housekeeping/deep-clean-checklist.md`, `docs/design/testing-strategy.md`, BUJO, journal updated.
- Status: Clean (2025-12-25, Codex)
- Risks/Follow-ups: Integrity sweep still caps at 5,000 states; Island win remains warning until search strategy improves.

## 2025-12-30 — v1.0 deep clean — Codex
- Scope: Deep clean pass to align docs/runbooks/README with current narrator/translator routing, interaction gating, and conversation behavior; archived conversation TODO list.
- Commands: `mvn -q test`
- Coverage: Not collected (no coverage tool configured).
- Docs: `readme.md`, `docs/architecture/overview.md`, `docs/design/ai-roles.md`, `docs/design/cli.md`, `docs/design/pipeline.md`, `docs/design/smart-actors.md`, `docs/design/todo-conversation.md`, `docs/index.md`, `docs/runbooks/ai-cli.md`, BUJO, journal updated.
- Status: Clean (2025-12-30, Codex)
- Risks/Follow-ups: InteractionState currently gates dice only; choice/confirm prompts remain a scaffold.

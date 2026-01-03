# Deep Clean Playbook (Adventure)

Last verified: 2026-01-02 (Codex — deep clean sweep)

Purpose: repo-wide housekeeping pass to keep code, docs, and content aligned. Review classes, refresh comments where needed, validate docs, and keep logs current.

## Rules of the Road
- Class comments act as indexes: summarize intent, invariants, and entry points when needed.
- Add method comments only when a method is non-obvious; keep inline comments rare and factual.
- Prefer descriptive names and top-to-bottom readability (fields → constructors → public API → helpers).
- Avoid semantic changes unless required for cleanup or correctness.
- Keep prompts and narrative content in storybook/YAML and resource files (no heuristic prose in core).

## Deep Clean Protocol
1. Scope & plan
   - Log the sweep in `docs/process/housekeeping/cleaning.log` and `docs/process/bujo/daily/<date>.md`.
   - Record intended tests in `docs/process/journal.md`.
2. Inventory
   - Verify root `readme.md` and `docs/` are current; note any drift.
   - List packages/classes to review and any recent hot spots.
3. Code pass
   - Refresh class summaries where they are missing or out of date.
   - Trim dead code, unused methods, and stale TODOs.
   - Keep comments minimal and accurate.
4. Docs pass
   - Update `docs/readme.md` and any touched references.
   - Add or refresh `Last verified` stamps for touched docs.
5. Testing
   - Run `mvn -q test` (or targeted suites when appropriate).
   - Record commands/outcomes in logs.
   - Verify JaCoCo coverage is 85%+ (see `target/site/jacoco/index.html`); note shortfalls in BUJO.
6. Log & handoff
   - Update `docs/process/housekeeping/cleaning.log` and `docs/process/housekeeping/clean-log.md`.
   - Capture risks/follow-ups in BUJO and `docs/process/journal.md`.

## Module Deep-Clean Backlog
Update Status to `Pending` / `In Progress (date, call sign)` / `Clean (date, call sign)`.

| Module | Status | Notes |
| --- | --- | --- |
| com.demo.adventure.buui | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.authoring.zone | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.authoring.cli | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.engine.command | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.engine.command.interpreter | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.domain.kernel | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.domain.save | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.engine.mechanics.crafting | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.ai.runtime.dm | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.support.exceptions | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.authoring.gardener | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.authoring.lang.gdl | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.engine.mechanics.keyexpr | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.domain.model | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.engine.mechanics.ops | Clean (2025-12-21, Codex) | Repo-wide sweep |
| com.demo.adventure.authoring.save | Clean (2025-12-21, Codex) | Repo-wide sweep |
| docs | Clean (2025-12-21, Codex) | Housekeeping logs added |
| storybook | Clean (2025-12-21, Codex) | No content edits |

## Deliverables
- Updated docs and housekeeping logs.
- Unused code removed or documented.
- Full test run recorded with outcomes.
- Coverage target (85%+) checked and recorded.

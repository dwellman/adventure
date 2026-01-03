# Deep Clean Checklist (Adventure)

Last verified: 2026-01-02 (Codex — deep clean sweep)

Use this to track a single deep-clean sweep for this repo.

## Metadata
- Scope: Island test-game win gate + integrity sweep
- Owner / Call sign: Codex
- Dates: Start 2025-12-25, Finish 2025-12-25
- Tests planned: `mvn -q test`

## Steps
- Scope recorded in `docs/process/bujo/daily/<date>.md` and `docs/process/journal.md` (commands planned).
- Repo has `readme.md` and `docs/` (design references and logs).
- Inventory of classes/files completed (including recent hot spots).
- Comments refreshed where needed; dead code and TODOs resolved or logged.
- Docs updated with `Last verified` stamps when touched.
- Tests executed; commands and outcomes recorded.
- `docs/process/housekeeping/cleaning.log` updated with scope, commands, outcomes, owner.
- `docs/process/housekeeping/clean-log.md` updated for deep-clean traceability.
- BUJO and `docs/process/journal.md` updated with risks and follow-ups.

## Evidence Links
- Commands run: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,GameIntegrityTest test`; `mvn -q test`
- Docs updated: `src/test/resources/games/island-adventure-test/game.yaml`, `src/test/resources/playbooks/island-adventure/playbook-escape.yaml`, `src/test/resources/playbooks/island-adventure/playbook-escape.md`, `docs/process/housekeeping/cleaning.log`, `docs/process/housekeeping/clean-log.md`, `docs/process/bujo/daily/2025-12-25.md`, `docs/process/journal.md`, `docs/howto/runbooks/testing-strategy.md`
- Open questions/risks: Integrity sweep still caps at 5,000 states; Island win remains warning until search strategy improves.

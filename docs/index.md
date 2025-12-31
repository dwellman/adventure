# Docs Index

Last verified: 2025-12-30 (v1.0 deep clean)

Authoritative references:
- `docs/architecture/overview.md` – package map + runtime flow diagram.
- `src/main/resources/games/island/narrative/backstory.md` – runtime backstory printed by GameCli for Island Adventure.
- `docs/design/dsl.md` – GDL/GEL/PIL grammar, acceptance criteria, and gap analysis.
- `docs/design/kernel.md` – kernel design snapshot.
- `docs/design/key-expression.md` – key expression syntax used by gates/locks.
- `docs/design/pipeline.md` – latest pipeline design.
- `docs/design/pattern-tests.md` – design patterns with usage guidelines and test coverage.
- `docs/design/pattern-index.md` – pattern index for key orchestration and verification touchpoints.
- `docs/runbooks/ai-cli.md` – authoritative AI/CLI runbook (translator → engine → narrator).
- `docs/runbooks/index.md` – runbook index and conventions.
- `docs/runbooks/testing-strategy.md` – testing strategy and agent validation workflow for games.
- `docs/design/gameplan-western.yaml` – Adventure GamePlan template filled for Western Adventure.
- `docs/design/smart-actors.md` – architecture for AI-backed in-world actors (planned).
- `docs/design/todo-conversation.md` – completed @mention smart-actor conversation checklist (archived).
- `docs/runbooks/new-game-walkthrough.md` – “build a new game and walkabout acceptance” guide (includes Gardener/exit/crafting expectations).
- `docs/guides/quickstart.md` – developer quickstart checklist.
- `docs/glossary.md` – shared terminology and definitions.
- `docs/adr/index.md` – architecture decision records index.

Game saves:
- Canonical gardened YAMLs live in `src/main/resources/cookbook/` for Island, Mansion, Western, and Spy.
- Structured per-plot definitions live under `src/main/resources/games/<id>/` (e.g., `games/island`, `games/mansion`, `games/western`, `games/spy`), organized into `world/`, `narrative/`, `motif/`, and `assets/`.
- Storybook authoring assets live under `src/main/resources/storybook/`; full bundles include `game.yaml`, `backstory.md`, `quests.md`, `recipes.yaml`, and `story.md`.
- `docs/games/index.md` – game ids and resource locations.

Validation Tools:
- `docs/cli/storybook-validate-cli.md` – validate a storybook YAML via GameSaveYamlLoader + GameSaveAssembler.
- `docs/cli/gameplan-cli.md` – run the AI-assisted GamePlan interview + planning pipeline.

Housekeeping:
- `docs/housekeeping/housekeeping.md` – shared playbook pointer and local notes.
- `docs/housekeeping/deep-clean.md` – deep clean protocol and backlog.
- `docs/housekeeping/deep-clean-checklist.md` – per-sweep checklist.
- `docs/housekeeping/clean-log.md` – deep clean log instructions.
- `docs/housekeeping/cleaning.log` – cleanup log entries.
- `docs/skill.md` – skill definition for AI runbook/doc alignment checks.
- `docs/bujo/today-tomorrow-someday.md` – rolling task list (today/tomorrow/someday).

Logs:
- `docs/logs/index.md` – log hub (BUJO, journal, scoreboard, playtests, housekeeping).

Playtests:
- `docs/playtests/1980-motif-spotcheck-2025-12-25.md` – 1980 motif-verb spot check transcript (all four adventures).
- `docs/scoreboard.md` – playthrough scoreboard log (manual + scripted runs).

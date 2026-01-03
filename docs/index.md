# Docs Index

Last verified: 2026-01-02 (v1.0 deep clean)

## Start here
- `docs/howto/guides/quickstart.md` - developer quickstart checklist.
- `docs/howto/runbooks/ai-cli.md` - authoritative AI/CLI runtime flow (translator -> engine -> narrator).
- `docs/howto/runbooks/testing-strategy.md` - testing strategy and agent validation workflow.
- `docs/howto/runbooks/new-game-walkthrough.md` - build a new game and walkabout acceptance guide.
- `docs/reference/architecture/overview.md` - package map and runtime flow diagram.
- `docs/reference/design/readme.md` - design overview and doc map.
- `docs/reference/glossary.md` - shared terminology and definitions.
- `docs/reference/skill.md` - AI runbook review skill.

## Design and architecture
- `docs/reference/design/engine.md` - engine/runtime design snapshot.
- `docs/reference/design/kernel.md` - kernel design.
- `docs/reference/design/pipeline.md` - pipeline design.
- `docs/reference/design/content.md` - content model and authoring constraints.
- `docs/reference/design/cli.md` - CLI architecture and runtime flow.
- `docs/reference/design/ai-roles.md` - AI roles and contracts.
- `docs/reference/design/dsl.md` - GDL/GEL/PIL grammar and acceptance rules.
- `docs/reference/design/key-expression.md` - key expression syntax.
- `docs/reference/design/parser.bnf` - parser grammar reference.
- `docs/reference/design/pattern-index.md` - pattern index for grounding/orchestration/verification/trust/learning.
- `docs/reference/design/pattern-tests.md` - tests that enforce the patterns.
- `docs/reference/design/smart-actors.md` - smart-actor architecture (planned).
- `docs/reference/design/ai-cli-mode.md` - superseded by the runbook.
- `docs/reference/design/todo-conversation.md` - archived @mention checklist.

## Authoring and CLI tools
- `docs/howto/cli/gameplan-cli.md` - GamePlan CLI usage.
- `docs/howto/cli/storybook-validate-cli.md` - storybook validation CLI.
- `docs/reference/design/gameplan-western.yaml` - GamePlan example.
- `docs/reference/games/index.md` - game ids and resource locations.

## Game content locations
- `src/main/resources/games/<id>/` - structured game bundles (world/narrative/motif/assets).
- `src/main/resources/cookbook/` - canonical gardened YAML snapshots.
- `src/main/resources/storybook/` - authoring bundles (game.yaml/backstory/quests/recipes/story when present).
- `src/test/resources/games/` - test-only game bundles (including gdl-demo, island-adventure-test, and test fixtures).
- `src/test/resources/minigames/` - test-only minigame fixtures.

## Logs and process
- `docs/process/logs/index.md` - log hub (BUJO, journal, scoreboard, playtests, housekeeping).
- `docs/process/journal.md` - scope and tests log.
- `docs/process/bujo/readme.md` - BUJO process.
- `docs/process/bujo/today-tomorrow-someday.md` - rolling task list.
- `docs/process/scoreboard.md` - playthrough scoreboard.
- `docs/process/playtests/` - playtest transcripts.

## Housekeeping
- `docs/process/housekeeping/housekeeping.md` - housekeeping notes.
- `docs/process/housekeeping/deep-clean.md` - deep clean protocol.
- `docs/process/housekeeping/deep-clean-checklist.md` - per-sweep checklist.
- `docs/process/housekeeping/clean-log.md` - deep clean log.
- `docs/process/housekeeping/cleaning.log` - cleanup log.

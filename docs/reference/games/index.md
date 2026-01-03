# Games Index

Runtime games (structured YAML under `src/main/resources/games/`):
| Game | Game id | Path | Notes |
| --- | --- | --- | --- |
| Island Adventure | `island-adventure` | `src/main/resources/games/island/` | Escape-room survival with loop gating. |
| Mansion Adventure | `mansion-adventure` | `src/main/resources/games/mansion/` | Whodunit with clue board flow. |
| Western Adventure | `western-adventure` | `src/main/resources/games/western/` | Timebound train rescue. |
| Spy Adventure | `spy-adventure` | `src/main/resources/games/spy/` | Cozy exploratory espionage. |

Test-only games (under `src/test/resources/`):
- `src/test/resources/games/island-adventure-test/` - reduced-gate Island variant for playbook/integrity tests.
- `src/test/resources/games/gdl-demo/` - GDL demo fixture for CLI and parser tests.
- `src/test/resources/games/test/` - minimal fixture bundle for unit tests.
- `src/test/resources/minigames/` - integration minigames (combat sim, dungeon adventure, cave walkthrough).

Each runtime game folder contains:
- `game.yaml` - structured entrypoint.
- `world/` - plots, fixtures, items, actors, triggers.
- `narrative/` - backstory and descriptions.
- `motif/` - alias and tag mappings.
- `assets/` - images and supporting art.

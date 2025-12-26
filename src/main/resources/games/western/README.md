# Western Adventure (Design Notes)

Purpose: Timebound western rescue with a bridge TNT threat and train payoff.

Motif and voice:
- Dust, iron, telegraph clicks, and last-chance heroics.
- Time pressure is the defining feel.

World structure:
- 16 plots, start at Train Platform.
- Loop/time pressure driven by `world/loop.yaml` and `world/triggers.yaml`.

Mechanics highlights:
- Bridge signal and strongbox progression; trigger-driven win/lose.
- Motif verbs live in `motif/aliases.yaml` (RIDE/GALLOP/TROT).

Assets:
- Cover art: `assets/western_adventure.png`.

Tests:
- Playbook: `src/test/resources/playbooks/western-adventure/playbook.yaml`.

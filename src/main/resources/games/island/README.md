# Island Adventure (Design Notes)

Purpose: Escape-room style island survival with a looping time reset.

Motif and voice:
- Salt, rust, storm-worn survival.
- Green motif signals the time stone and reset.

World structure:
- 16 plots, start at Wreck Beach.
- Loop reset is driven by `world/loop.yaml` + triggers; notebook persists memory across resets.

Mechanics highlights:
- Crafting and trigger-driven reveals (torch/raft progression).
- Motif verbs live in `motif/aliases.yaml`.

Assets:
- Cover art: `assets/island_adventure.png`.

Tests:
- Playbook: `src/test/resources/playbooks/island-adventure/playbook.yaml`.

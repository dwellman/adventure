# Mansion Adventure (Design Notes)

Purpose: Whodunit exploration with evidence pinning on the case board.

Motif and voice:
- Dusty grandeur, creaking corridors, investigative tension.
- Evidence board pinning is the core feedback loop.

World structure:
- Mansion layout emphasizes corridor traversal and clue collection.
- Smart actor metadata lives in `world/smart-actors.yaml`.

Mechanics highlights:
- Case board evidence pinning drives the win trigger.
- Motif verbs live in `motif/aliases.yaml` (EXAMINE/INVESTIGATE/PIN).
- Tag sources for smart-actor context live in `motif/tags.yaml`.

Assets:
- Cover art: `assets/mansion_adventure.png`.

Tests:
- Playbook: `src/test/resources/playbooks/mansion-adventure/playbook.yaml`.

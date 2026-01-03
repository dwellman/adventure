# Spy Adventure (Design Notes)

Purpose: Spy chase across a neon city to intercept the WOPR.

Motif and voice:
- Neon, rain, coded signage, and surveillance paranoia.
- Emphasis on pursuit and signal tracing.

World structure:
- City grid with explicit gates plus auto-synth movement between adjacent plots.
- Smart actor metadata lives in `world/smart-actors.yaml`.

Mechanics highlights:
- Win trigger requires collecting key items before entering the final niche.
- Motif verbs live in `motif/aliases.yaml` (SURVEIL/TAIL/INFILTRATE).
- Tag sources for smart-actor context live in `motif/tags.yaml`.

Assets:
- Cover art: `assets/spy_adventure.png`.

Tests:
- Playbook: `src/test/resources/playbooks/spy-adventure/playbook.yaml`.

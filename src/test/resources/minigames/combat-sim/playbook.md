# Combat Sim Playbook

Commands:
1) look
2) north
3) attack bandit
4) attack bandit
5) attack bandit
6) quit

Expected highlights:
- Combat begins message.
- Hit/miss lines with health totals.
- Victory message after the bandit falls.

Notes:
- Integration tests pin dice to max rolls for deterministic combat output.
- Smart actors are skipped during combat turns (integration pending).
- Magic is not modeled beyond weaponDamage/armorMitigation on items.

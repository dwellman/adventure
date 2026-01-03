# New Game Creation & Walkabout Acceptance (WIP)

This doc tracks the lightweight loop for creating a new game and proving it with a walkabout. We’ll add detail as the Gardener/zone-builder features evolve.

## Purpose
Provide a minimal, repeatable loop to author a new game and verify it with a walkabout.

## Inputs
- Source `game.yaml`, optional gardener patch, and authoring CLI tools.

## Outputs
- Gardened structured YAML plus a short walkabout transcript.

## Tests
- Use the validation commands listed in "Baseline flow" and record results in BUJO/journal.

## Baseline flow (v1)
1) **Generate** the world (ZoneBuilderCli or other generator) → produce `game.yaml`.
2) **Fingerprint** the world for determinism: `mvn -q -Dexec.mainClass=com.demo.adventure.authoring.cli.WorldFingerprintDump -Dexec.args="game.yaml" exec:java`.
3) **Apply Gardener patch** (text-only; no topology changes):
   - Validate: `mvn -q -Dexec.mainClass=com.demo.adventure.authoring.cli.GardenerCli -Dexec.args="--in game.yaml --patch patch.yaml --validate-only" exec:java`
   - Must PASS: plot coverage 100%, duplicate-title check, fingerprint match; thing coverage may WARN.
   - Apply: `mvn -q -Dexec.mainClass=com.demo.adventure.authoring.cli.GardenerCli -Dexec.args="--in game.yaml --patch patch.yaml --out game-gardened.yaml" exec:java`
4) **Structured export** (optional): `GameStructExporter` → `game.yaml` + map/fixtures/items/actors/descriptions.
5) **Playtest walkabout** (acceptance): run `./adventure`, select the new game, and do a short walk that touches the critical beats (start → key pickup → gated move → one or two branches). Capture 10–15 turns.

## Narration rules (Zork baseline)
- Titles: short, concrete, Title Case; no generator words; global-unique.
- Descriptions: 1–2 sentences, factual, minimal adjectives; orientation cue is allowed.
- Things: concise names/one-liners; no new mechanics; no shouting (ALL CAPS) beyond proper nouns.
- Blocked exits: exit lines show the gate description as the inline reason when closed (punctuation trimmed). When the gate opens, the hint disappears so the exit list always reflects reachable moves.

## Lint/validation expectations
- Gardener validation enforces: known IDs, length caps (plot title 40, plot desc 160, thing name 32, thing desc 120), normalized duplicate-title rejection, fingerprint match, 100% plot coverage.
- `--validate-only` summary should read like:  
  `Validation OK`  
  `Fingerprint: <full> (matches)`  
  `Plots changed: X | Things changed: Y`  
  `Coverage: plots N/N (PASS), things M/T (PASS|WARN) | fingerprint <prefix>`
  - FAIL if any plot missing; WARN if things missing (prints a sample of missing thing IDs).

## Walkabout acceptance checklist (per play transcript)
- Titles/descriptions are concrete and non-procedural (no “Spine/Branch/Zone/Demo”).
- Navigation clarity: exits printed are truthful; blocked gates show the inline reason (from gate descriptions); vertical directions make sense.
- Distinctiveness: adjacent/revisited rooms are distinguishable (at least one unique detail).
- Brevity: stays within caps; no wall-of-text.
- Crafting discoverability: `how craft <item>` reports skill + ingredients; calling `craft`/`how craft` with no args lists known recipes; failure messages echo missing pieces.
- Inventory capacity: hands/pack have limited space (default 0.3×0.3). Authored containers (e.g., Canvas Backpack 0.6×0.6) expand capacity; large items with bigger footprints should fail to fit with a clear “It doesn’t fit…” message.

## Cozy sidequests & crafting loops (Zone Builder Demo)
- Climb loop: grab the Rope Ladder at Treehouse Approach; craft a Pole Ladder (Rope Ladder + Driftwood Pole) to reach the Canopy Outlook. Either ladder satisfies the cliff UP/DOWN gates.
- Hook line: craft a Hooked Line (Rope + Fish Hook) to reach Cliff Perch and pick up Kerosene + Oil Rag for torch work.
- Torch line: Torch (Stick + Rags) → Soaked Torch (Torch + Kerosene) → Lit Torch (Soaked Torch + Flint + River Stone) or Signal Torch (Torch + Oil Rag). Needed for the Shaded Cave gate.
- Raft repair: patch the raft (Punctured Raft + Tar Lump) to reach the Offshore Sandbar.
- Snare sidequest: Set Snare (Snare Wire + Bait) opens the Quiet Clearing for a small reward.
- Ingredient hints: Rope at Ladder Landing; Driftwood Pole/Rags at Treehouse Approach; Fish Hook/Bait/River Stone at Tide Pools; Tar Lump at Tar Pit; Punctured Raft at Lagoon Shore; Flint at Dry Creek Bend.
- Acceptance walk touchpoints: start → rope ladder pickup → climb UP → Cliff Turn EAST to Sea Overlook/Sulfur Trail → Tar Pit/Lagoon Shore → optional puzzles (Hooked Line → Perch, torch gate to Shaded Cave, raft to Sandbar).

## Open questions / future tweaks
- Decide a rule for vertical exits: should DOWN always be the inverse of UP, or can it be “lower route” alongside horizontal exits?
- Style profiles: Gardener can take `styleProfile` (ZORK, ISLAND_PULP, etc.) to theme narration without changing mechanics.
- Add “banned tokens” lint (e.g., reject titles containing generator words) to validate-only.

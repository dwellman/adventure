# World Building Pipeline (Pre-Game) — Design v0.4

Status: Draft (design-only)  
Last verified: 2025-12-30 (v1.0 deep clean)

## Purpose

Define a deterministic, pre-game world building pipeline with a strict separation of concerns:

1) Build the map (topology only)  
2) Populate the map (fixtures, items, actors)  
3) Pregame checks (no AI, collect all problems, block start on any problem)  
4) AI description improvement (text-only, no mechanics changes)
5) AI runtime loop (translator + narrator layered over the running game; mechanics remain deterministic)

This document specifies **order of operations**, **inputs/outputs**, **checks**, and **immutability constraints**.

## Invariants

- The pipeline is deterministic given the same seed and the same inputs.
- Pregame checks **must not stop early**. They collect all problems in a single report.
- If any problems exist, the game **must not start**.
- AI is not permitted to move or create/delete Things, change gates, or change any gate open/key logic. AI may only improve text fields in Phase 4.

## Terminology

## Connector Plot Decoration (Phase 1 Defaults, Phase 4 Enrichment)

### Problem
Connecting plots are often introduced to improve pacing and structure. At creation time (Phase 1),
their final prose is not available yet because AI runs only in Phase 4.

### Decision
Connecting plots must be created with a minimal, readable default description using a small
set of predefined connector archetypes. The Gardener (Phase 4) may later enrich these descriptions,
but may not change any mechanic-relevant fields.

### Connector Archetype Set (initial)
Each archetype provides a 1–2 sentence default description.

- CONNECTOR_TRAIL: A narrow trail linking two places. The way is passable, but watch your footing.
- CONNECTOR_THICKET: A dense thicket presses in from both sides. Something has been moving through here.
- CONNECTOR_RIDGE: A rocky ridge with a view of the land around you. The wind carries distant sounds.
- CONNECTOR_SHORELINE: A stretch of shoreline where surf and debris meet. The ground shifts with each step.
- CONNECTOR_CAVE_PASSAGE: A tight passage between stone walls. The air is cooler here.

### How the engine identifies connector plots (design rule)
At map-build time, a connecting plot must record its archetype in one of these non-mechanical ways:
- Preferred: a plotArchetype string field on the plot, or
- Acceptable: a naming convention (e.g., title prefix "Path:" plus the archetype), or
- Acceptable: a metadata tag that is guaranteed not to be referenced by mechanics/cookbooks.

Choose one in implementation. The pipeline requires the archetype to be identifiable for Phase 4.

- **World seed**: A value used to drive deterministic generation.
- **Place / Plot**: A location node in the world graph (LAND plot).
- **Gate**: A directed edge from one plot to another with direction and a target plot.
- **Gate key string**: The “open DSL string” attached to a gate (stored on the gate). It is compiled in pregame checks but not evaluated.
- **Visibility**: A display property for a gate. It does not affect topology checks.
- **Region**: A coarse plot location bucket (e.g., COAST, INLAND, CLIFF, VOLCANO). Used for authoring and map scattering only. Not used by mechanics or pregame checks.
- **Ownership chain**: Repeated traversal from a Thing’s owner to the owning plot, until the chain ends at a (reachable) place.

## Phase 0 — World Recipe (Hand-Authored Inputs)

### Goal
Define the inputs used to deterministically build the world. This is authored by humans.

### Requirements
The recipe must include:
- `worldSeed`
- `startPlotId` (UUID)
- LAND plots (UUIDs) with:
  - `name`
  - `region` (coarse location bucket; used for authoring/placement only, not mechanics)
  - minimal default `description`
- Gates with:
  - `fromPlotId`
  - `direction`
  - `toPlotId`
  - `keyString` (the “open DSL string”)
  - `open`
  - `visible`
- Connector plot policy:
  - connector archetypes
  - minimal default descriptions

### Notes
- Plot IDs are UUIDs. There is no requirement for additional human-readable handles beyond the plot `name`.
- There is no requirement for “loops.” No loop heuristic is used or enforced in the build pipeline.

## Phase 1 — Build the Map (Topology Only)

### Goal
Create the world graph of plots and gates. **No items, actors, or fixtures are placed in this phase.**

### Steps
1. Set `worldSeed`.
2. Create all LAND plots.
3. Create gates connecting plots.
4. If you introduce connecting plots, assign a connector archetype and set its minimal default description (see Connector Plot Decoration).
5. Attach the “open DSL string” (gate key string) to each gate.
6. Do not place any items/actors/fixtures.

### Output
A MapSpec containing:
- Seed
- Plot list (LAND plots)
- Gate list (source, direction, targetPlotId, visible, open, keyString)

## Phase 2 — Populate the Map (World Contents)

### Goal
Populate the previously created map with Things. **Topology is unchanged.**

### Steps
1. Add fixtures (built-in environment Things owned by plots).
2. Add items and actors with initial ownership.
3. Apply any deterministic builder pass needed to finalize derived container plots/slots and indices.

### Output
A PopulatedWorld containing:
- MapSpec
- Things (fixtures, items, actors)
- Derived ownership/container structures (if any)

## Phase 3 — Pregame Check (No AI)

### Goal
Validate the world before start. Collect **all** issues. Do not start the game if any issues exist.

### Output
A BuildReport containing `problems[]`.

### BuildReport Problem Format (proposal)
Each problem entry should include:
- `code` (stable string)
- `severity` (Error/Warning; for now, any Error blocks start)
- `message` (short, human readable)
- `entityType` (Plot/Gate/Thing/World)
- `entityId` (id)
- `details` (optional map)

### Check 3A — Reachability (Topology Only)
Requirement:
- From the start plot (“Beach”), every LAND plot must be reachable by following gates.
- **Ignore `visible`.**
- **Ignore `open`.**
- **Ignore key DSL evaluation.** (We treat every gate as traversable for topology.)

Algorithm (proposal):
1. Build adjacency edges from every gate whose `targetPlotId` resolves to a plot.
2. Run BFS/DFS from `startPlotId`.
3. For any LAND plot not visited, emit `E_UNREACHABLE_PLOT`.

Notes:
- Gates with missing/invalid targets emit `E_GATE_TARGET_INVALID` and are not used as edges.

### Check 3B — Gate Key DSL Compiles (Do Not Evaluate)
Requirement:
- Every gate’s key string must compile.
- Compilation only. No evaluation.

Output:
- For failures: `E_GATE_KEY_COMPILE_FAIL` with compiler error details.

### Check 3C — Every Item Can Be Found (Ownership Ends at a Reachable Place)
Requirement:
- Every “findable” Thing must have an ownership chain that ends at a reachable LAND plot.
- This is true even if the Thing is inside closed containers.
- This does not require `open` to be true and does not require key evaluation.

Algorithm (proposal):
1. For each Thing that is required to be findable:
   - Follow owner pointers until a LAND plot is reached.
   - If the chain cycles, emit `E_OWNERSHIP_CYCLE`.
   - If the chain ends at a plot not in the reachable set from 3A, emit `E_THING_UNREACHABLE_OWNER_PLOT`.
   - If the chain references a missing owner id, emit `E_THING_OWNER_MISSING`.

## Phase 4 — AI Description Improvement (Text Only)

### Goal
Improve narrative text without altering world mechanics.

### Allowed changes
- Update descriptions (and only descriptions) on plots and Things.
  - This includes enriching connector plots created with minimal archetype defaults.

### Forbidden changes
- No adding/removing Things.
- No changing ownership.
- No changing gates, directions, targets.
- No changing `open` values.
- No changing gate key strings.
- No changing any mechanic-relevant fields.

### Output
A DescriptionPatch (append-only) containing:
- `entityId`
- `oldTextHash` (or old text snapshot)
- `newText`
- `timestamp`

## Phase 5 — AI Runtime Loop (Gameplay)

### Goal
Run gameplay with AI affordances while keeping mechanics deterministic.

### Roles
- **Translator**: deterministic command mapper. Inputs: player text, visible fixtures/items, inventory, last scene text. No backstory. Outputs one command (single-line command string). No invention; only uses the allowed command surface.
- **Engine**: authoritative state machine. Executes commands, updates world state, and emits factual snapshots (location, fixtures/items/actors, exits). Translator/narrator never mutate mechanics.
- **Narrator**: rewrite layer with command-specific prompt variants. Inputs are minimal per variant (scene snapshot vs. action result vs. color/emote/check). Must keep mechanics factual (no new fixtures/items/exits/state changes). Paraphrase is allowed if facts are preserved. Destination names and direction words must stay exact when used. Exits are appended only for scene output.

### Outputs per turn
- On command: engine snapshot + narrated rewrite.
- `author` (AI marker)


## Persistence (YAML Artifacts)

### Goal
After the world is built, persist it to YAML so it can be reloaded, audited, and deterministically
re-generated for consistency checks.

### Artifacts (proposal)
1) `world.manifest.yaml`
- `schemaVersion`
- `worldId`
- `seed`
- `startPlotId`
- `generatorInputs` (all inputs required to regenerate Phase 1 and Phase 2 deterministically)
- `files` (references to the other YAML files)

2) `world.map.yaml` (Phase 1 output)
- LAND plots
- Gates (sourcePlotId, direction, targetPlotId, visible, open, keyString)

3) `world.things.yaml` (Phase 2 output)
- Fixtures, items, actors
- Ownership fields
- Any derived slot/compartment plots created deterministically during population/builder passes

4) `world.descriptions.yaml` (Phase 4 output)
- Decorated descriptions for plots and Things
- Recommended fields per entry:
  - `entityId`
  - `oldTextHash` (or old snapshot)
  - `newText`
  - `timestamp`
  - `author` (e.g., `gardener`)

### Storage rule
- Mechanics/topology state is stored in `world.map.yaml` and `world.things.yaml`.
- Text decoration is stored in `world.descriptions.yaml` to keep AI edits isolated and reviewable.

## Load-Time Consistency Gate (Regenerate + Compare)

### Goal
On load (or before start), confirm the persisted built world matches what the generator would produce from
the stored seed + generator inputs.

### Rule
Before starting the game:
1. Load `world.map.yaml` and `world.things.yaml`.
2. Run Phase 3 Pregame Checks (collect all problems).
3. Regenerate the world from `seed + generatorInputs` (Phase 1 + Phase 2) and compare the result to the loaded map/things.
4. If mismatched, add blocking problems and do not start.

### Compare scope (proposal)
- Map compare: plot ids, gate ids, source/direction/target, open, visible, keyString.
- Things compare: thing ids, kinds, ownership chains, and mechanic-relevant fields.
- Ignore description differences here (descriptions are applied from `world.descriptions.yaml` after consistency passes).

### Output
Add blocking problems such as:
- `E_REGEN_MAP_MISMATCH`
- `E_REGEN_THINGS_MISMATCH`
with details describing the first observed mismatch and a count of total mismatches.


## Start Gate

The game may start only if:
- Phase 1 and Phase 2 completed successfully, and
- Phase 3 BuildReport has **zero** blocking problems.

## Test Plan (proposal)

1. **Happy path**: known seed produces a connected map; all keys compile; all items resolve to reachable plots.
2. **Unreachable plot**: omit a connecting gate; expect `E_UNREACHABLE_PLOT`.
3. **Bad gate target**: gate points to unknown plot; expect `E_GATE_TARGET_INVALID`.
4. **Key compile failure**: invalid DSL string; expect `E_GATE_KEY_COMPILE_FAIL`.
5. **Orphan item**: item owner chain ends in missing plot; expect `E_THING_OWNER_MISSING` or `E_THING_UNREACHABLE_OWNER_PLOT`.
6. **Ownership cycle**: create a cycle; expect `E_OWNERSHIP_CYCLE`.
7. **Visibility ignored**: set all gates `visible=false`; reachability must still pass if targets connect.

## Work Plan for Tomorrow (implementation outline)

- Implement MapSpec builder (Phase 1) and ensure it does not place Things.
- Implement Populate pass (Phase 2) as a separate step that only adds Things.
- Implement BuildReport and the three Phase 3 checks with “collect all problems” behavior.
- Add a hard “do not start if any problems” start gate.
- Implement Phase 4 as a patch-only text pass guarded by strict field immutability rules.

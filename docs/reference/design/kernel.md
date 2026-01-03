# Island Kernel Design v0.3
**Status:** Draft, locked per conversation  
**Primary goal:** A tight, deterministic kernel that supports rich worlds authored from YAML, with puzzles driven by predictable gating and a countdown clock.

---

## 0. What this document is
This is the authoritative design for the **mechanics kernel** and the **world setup pipeline** for Island.

It defines:
- The **minimal mechanical fields** and how they are interpreted (isOpen is derived, not stored).
- The **core entity model** (Actor, Plot, Item) and ownership rules.
- The **movement kernel** (`MOVE`) and how navigation gates work.
- The **turn pipeline** (`PARSE/NORMALIZE → INSPECT/PLAN → COMMIT → TICK`).
- The **world clock + TTL** contract.
- The **key test string** contract (JIT compile, compile-fail = false).
- The **YAML world setup** contract and deterministic world-building passes.
- The **Gardener** (LLM NPC) contract, limited to narrative description updates.

It does **not** define UI, rendering, or a full content bible.

---

## 1. Design principles
### 1.1 Tight kernel
The kernel must stay small, boring, and stable:
- No bespoke verb logic.
- No “helper programs” hidden inside commit.
- Mechanics live in **6 mechanical fields** plus `ThingKind` meaning (isOpen is computed from `key`).

### 1.2 Determinism and proof
- Given the same YAML + seed + sequence of commands, mechanics outcomes are reproducible.
- Every attempt produces a per-turn **Proof of Work** trace sufficient to explain “why”.

### 1.3 One-parent ownership
Every Thing has exactly one parent Plot at a time. This is the core structural invariant.

### 1.4 Compiler-style failures
- **Compile-time issues**: parser/normalizer issues (invalid commands), and key-test compile failures.
- **Plan-time issues**: gate failures (puzzle walls).
- **Commit-time issues**: engine invariants only (bugs), not gameplay.

---

## 2. Glossary
- **Thing**: Base entity for all game objects. Actor, Plot, Item all extend Thing.
- **Plot**: A location. Includes Land plots and sub-plots (slots, compartments).
- **Actor**: A Thing that can act. Players and NPCs.
- **Item**: A Thing that is carried, worn, stored, consumed, crafted.
- **Owner Plot**: The Plot Thing that contains a Thing.
- **Gate Thing**: A directed exit from one Land plot to another, modeled as a Thing.
- **Key**: A test string stored on a Thing (typically a gate) compiled and evaluated during planning.
- **World clock**: Countdown in turns. Reaches 0, game ends.
- **TTL**: Absolute clock value at which a Thing’s state changes or expires.

---

## 3. Core model
### 3.1 Thing (base)
All runtime objects derive from Thing.

#### Identity and narrative fields
- `id: UUID`  
- `label: String` (human-friendly name)
- `description: String` (narrative, never used for mechanics)

#### Ownership
- `ownerPlotId: UUID`  
  Must refer to a Plot Thing. Exactly one.

#### Mechanical fields (the only mechanics fields)
- `visible: boolean`  
- `key: String`  
  A test expression compiled JIT during planning. `isOpen` for a Thing is `eval(key)`. Default key is `"true"` when omitted.
- `ttl: int`  
  Absolute trigger at worldClockRemaining value. `-1` means immortal.
- `size: int`  
- `weight: int`  
- `volume: int`  
  Mutable ordinal used for “amount” style mechanics (bottle/cup, fuel).

**Important:** The meaning of `key`/`isOpen` is defined by `ThingKind` (see 3.4). The kernel does not read descriptions.

---

### 3.2 Plot extends Thing
A Plot is a Thing that can contain Things via ownership. Plots exist in the global registry like everything else.

#### Plot classification
- `plotKind: PlotKind`  
  - `LAND` (world locations)
  - `SLOT` (Actor-attached plots such as wrist/hands/body)
  - `COMPARTMENT` (Item-attached plots such as desk drawers/tabletop, bag inside)

- `plotRole: String`  
  Freeform string used for roles, examples:
  - Land plots: `"CAMP"`, `"BEACH"`, etc (optional)
  - Slots: `"LEFT_HAND"`, `"RIGHT_HAND"`, `"BOTH_HANDS"`, `"WRIST"`, `"BODY"`
  - Compartments: `"INSIDE"`, `"TABLETOP"`, `"DRAWER_1"`, `"DRAWER_2"`

- `hostThingId: UUID?`  
  Required for `SLOT` and `COMPARTMENT` plots.  
  - Slot plots: hostThingId = Actor id
  - Compartment plots: hostThingId = Item id

#### Land plot invariants
Land plots are world roots:
- Land plots **cannot be owned** by any other plot. They are roots of navigation.
- Land plots must have `ownerPlotId = ROOT` (see 3.6) or omitted (loader normalization may set it to ROOT).

#### Slot plot invariants
Slot plots are anchored to an Actor:
- Slot plots are not owned by land plots.
- Slot plots exist “with” the actor by identity linkage (`hostThingId`).
- Items in slot plots remain with the actor when the actor travels.

#### Compartment plot invariants
Compartment plots are anchored to an Item:
- Compartment plots exist “with” the item by identity linkage (`hostThingId`).
- A desk’s drawers do not move separately from the desk.

---

### 3.3 Actor extends Thing
Actors are Things that can perform actions.

#### Physical location
- The Actor’s physical location is `ownerPlotId`, and for Actors this must point to a **Land plot**.

#### Slots (plots attached to actor)
Each Actor has a fixed set of slot plots, auto-created deterministically:
- `LEFT_HAND`
- `RIGHT_HAND`
- `BOTH_HANDS` (this is what `P` resolves to for staging)
- `WRIST`
- `BODY`

Slot plots are Plot Things with:
- `plotKind = SLOT`
- `plotRole` set to one of the above
- `hostThingId = actorId`

**Staging rule:** `P` (as a location token) resolves to `P.BOTH_HANDS` in kernel expansions.

---

### 3.4 Item extends Thing
Items are Things that can be carried, worn, stored, crafted, destroyed.

#### Item stats
- `size, weight, volume` are meaningful for gating and constraints.
- `volume` is mutable and used for transfer-like mechanics (bottle to cup, fuel transfer).

#### Items with compartments
Some items have compartment plots:
- backpack: an `INSIDE` compartment plot
- desk: `TABLETOP`, `DRAWER_1`, `DRAWER_2`, etc

These plots are Plot Things with:
- `plotKind = COMPARTMENT`
- `hostThingId = itemId`

---

### 3.5 ThingKind defines mechanics meaning
`ThingKind` is the interpreter for the mechanical fields.

It defines:
- How `key`/`isOpen` is interpreted for the kind (examples):
  - Gate: passable or blocked
  - Container: openable for access
  - Torch: lit or unlit
- What `key` means (a gate predicate, a torch-lighting requirement, etc)
- What `ttl` means (close at time, burn out at time, expire at time)
- Whether `volume` is used for this kind and how.

**Rule:** The kernel never reads `description` for mechanics. It reads only these fields plus the kind meaning.

---

### 3.6 Root owner sentinel
We use a sentinel UUID:
- `ROOT = 00000000-0000-0000-0000-000000000000`

Land plots and auto-created host-attached plots may have:
- `ownerPlotId = ROOT`

This keeps the “every Thing has an ownerPlotId” invariant while ensuring world roots and identity-attached plots are not accidentally inserted into land inventories.

---

## 4. Global registry
There is a global registry of all Things:
- `ThingRegistry: Map<UUID, Thing>`

### 4.1 Derived indices
To keep the kernel fast and deterministic, we maintain derived indices:
- `PlotInventoryIndex: Map<plotId, SortedSet<thingId>>`  
  Sorted by UUID to ensure deterministic ordering.

Whenever ownership changes:
- update `thing.ownerPlotId`
- update plot inventory index for old owner and new owner

---

## 5. World clock and TTL
### 5.1 World clock
- `worldClockRemaining: int`
- When it reaches `0`, the world ends.

### 5.2 Turn consumption rule
**Every command attempt consumes time.**  
Even if INSPECT/PLAN fails and COMMIT does not occur, the clock ticks down by 1.

This creates urgency and tension.

### 5.3 TTL semantics
TTL is an absolute trigger:
- `ttl = -1` means immortal (no trigger)
- `ttl >= 0` means the Thing triggers when `worldClockRemaining == ttl`

Examples:
- A door with `ttl = 5` closes when clock reaches 5.
- A torch with `ttl = 7` burns out when clock reaches 7.

### 5.4 Turn pipeline ordering
Each turn executes this pipeline, always:

1. `PARSE/NORMALIZE` (may produce CompileException for invalid command)
2. `INSPECT/PLAN` (may produce GateException for puzzle failures)
3. `COMMIT` (only if a plan exists)
4. `TICK` (always)
   - decrement worldClockRemaining by 1
   - apply TTL triggers for the new value
   - if clock hits 0, end world

### 5.5 TTL trigger application
TTL triggers are applied deterministically:
- collect all Things whose ttl == current worldClockRemaining
- sort by Thing UUID
- apply their effects

TTL effects must be “boring commits” and must not consult gates.

### 5.6 TTL effects (minimal set)
Two effect styles cover the game:
- **State change**: set `open=false`, set `visible=false`, etc (meaning depends on ThingKind)
- **Transform**: remove one Thing, add another (example: lit torch becomes burned-out torch)
- **Expire**: remove Thing from registry and from its owner plot inventory

---

## 6. Navigation: Land plots, directed gates, and exits
### 6.1 Directions
Land plots expose **10 gate slots**, one per compass + vertical direction:
- `NORTH, NORTH_EAST, EAST, SOUTH_EAST, SOUTH, SOUTH_WEST, WEST, NORTH_WEST, UP, DOWN`
- Each slot holds either a Gate Thing (see 6.2) or `null` to mean “no path”.

### 6.2 Gates are Things
A navigation exit is a **Gate Thing**:
- It is an Item Thing of a gate kind (implementation choice), owned by a Land plot.
- It has:
  - `ownerPlotId = sourceLandPlotId`
  - `direction` attribute (one of the 10)
  - `targetPlotId` attribute (destination land plot UUID)
  - mechanical fields: `open`, `visible`, `key`, `ttl`

Gates are directed:
- A door is typically represented as two gates (A→B and B→A) optionally linked.
- A trap door is a single directed gate.

### 6.3 YAML invariants for gates
Loader must enforce:
- At most one gate per `(sourcePlotId, direction)`
- `targetPlotId` exists and is a Land plot
- Gate Thing `ownerPlotId` must reference a Land plot

### 6.4 GO planning uses gate scan
`GO direction` is planned by scanning gates:
- Source plot is the Actor’s `ownerPlotId`
- Find the gate Thing for that direction owned by source plot
- Evaluate visibility and open rules:
  - If not visible, treat as “no exit”
  - Compute effective open (see key evaluation rules)
- If pass, plan actor movement:
  - `MOVE(P, P, sourcePlot, targetPlot)`

Forced moves (trap/fall) can reuse the same gate but may ignore `visible` depending on the action type.

---

## 7. The movement kernel: MOVE
### 7.1 Kernel operation
All transfers are expressed as:
- `MOVE(P, I, FromPlot, ToPlot)`

Where:
- `P` is the actor performing the action
- `I` is the Thing being moved (can be actor itself for navigation)
- `FromPlot` and `ToPlot` are Plot Things

### 7.2 Co-location rule
For a move to be legal:
- `P` must be co-located with `FromPlot` and `ToPlot` at the Land plot level.

Co-location is defined by **root land plot**:
- `rootLandPlot(P)` is `P.ownerPlotId`
- A plot is local to P if:
  - it is a Land plot equal to `P.ownerPlotId`, or
  - it is a SLOT plot hosted by P, or
  - it is a COMPARTMENT plot hosted by an item that is ultimately local to P’s land plot

This keeps the kernel minimal while supporting rich containment.

### 7.3 Two-stage local move sugar
A common action is “move item from A to B” within a local scene:
- Example: move gem from pedestal to bag
- Conceptual: “take it, then stow it”

We implement this in one turn as two kernel steps:

1) TAKE phase:
- `MOVE(P, I, A, P.BOTH_HANDS)`

2) STOW/PLACE phase:
- `MOVE(P, I, P.BOTH_HANDS, B)`

Rules:
- Phase 2 runs only if phase 1 commits.
- If phase 2 fails in planning, the item remains in `P.BOTH_HANDS`.

### 7.4 DROP and similar
DROP is a direct move between plots:
- `DROP watch` becomes:
  - `MOVE(P, watch, P.WRIST, rootLandPlot(P))`

“Where is the watch now?” is determined by `watch.ownerPlotId`.

---

## 8. Gating and precedence
Every planned kernel step follows a strict precedence order:

1) **Bind/existence**
- Resolve P, I, FromPlot, ToPlot
- If missing, fail predictably

2) **Source truth**
- Assert `I` is owned by FromPlot
- Assert locality / co-location requirements for P

3) **Removal authority**
- Can `I` be removed from FromPlot?
- Can FromPlot allow removal?
- Can P remove?

4) **Insertion authority**
- Can `I` be inserted into ToPlot?
- Can ToPlot allow insertion?
- Can P insert?

5) **Capacity/constraints**
- Slot capacity, weight/size/volume constraints, etc

6) **Post-conditions**
- No-op, already there, etc

**First failing gate wins.** The first failure stops that kernel step.

---

## 9. The key field: test strings and JIT compile
### 9.1 Key is a test string
`key` is a string that defines a predicate.

Examples:
- `HAS["lit torch"]`
- `DICE[20, n > 15]`
- `(HAS["lit torch"] OR (DICE[20, n > 15]))`

### 9.2 JIT compilation contract
During planning, when a key must be evaluated:
- Compile `key` just in time using the existing tokenizer and LR compiler.
- Evaluate the compiled predicate in the current context.

### 9.3 Compile-fail rule (locked)
If key compilation fails with `CompileException`:
- Record the compile failure in the per-turn trace
- Treat the key predicate result as `false`
- Continue evaluation deterministically

This preserves predictable gameplay behavior.

### 9.4 Effective open rule for gates
For gate-like Things where `open` means passable:
- `effectiveOpen = open OR keyPredicateResult`

We do not mutate `open` when evaluating `key` unless a specific action does so or TTL does so.

### 9.5 Dice (randomness) in key predicates
`DICE[...]` is supported as a simple way to introduce controlled randomness.

Determinism requirement:
- Dice must be derived from a reproducible source:
  - `worldSeed` (chosen at game start)
  - turn identifier (for example: initialClock - currentClock, or the currentClock itself)
  - actorId, gateId
- The dice roll value must be recorded in the per-turn trace.

This gives “order + chaos” while retaining reproducibility for debugging and proofs.

---

## 10. Crafting and uncrafting
Crafting is a separate action family that follows the same pipeline:
- PARSE/NORMALIZE → INSPECT/PLAN → COMMIT → TICK

### 10.1 Craft behavior
- Craft consumes N Things and creates 1 new Thing.
- The consumed Things are removed from:
  - owner plot inventories
  - the global registry
- The new Thing is introduced:
  - added to registry
  - attached to a destination plot (typically P.BOTH_HANDS or a specified slot)

### 10.2 Uncraft behavior
- Uncraft consumes 1 Thing and produces N Things.
- Similar remove and introduce rules apply.

### 10.3 Deterministic crafting
Crafting must be deterministic:
- inputs are selected explicitly by UUID or deterministically by name resolution rules
- outputs have deterministic UUIDs or are generated in a reproducible manner

---

## 11. YAML world setup and world-building passes
### 11.1 YAML responsibility
YAML defines the “important stuff”:
- Major Land plots (with stable `plotKey` and human name)
- Directed Gate Things (exits) grouped under each plot
- Key actors and key items (with stable `key`, name, description)
- Core labels and base descriptions (the deterministic description)
- Ownership via `ownerKey` (can point to a plot or another thing)
- Gate predicates via `keyString` (defaults to `"true"`; `isOpen` = eval(`keyString`))
- Optional coordinates (`locationX/Y`) may be omitted; loader derives deterministic grid coords from keys/region.

YAML does not need to define:
- actor slot plots
- item compartment plots
- filler props, flora/fauna, minor gates
- UUIDs (generated deterministically from kind + key)

### 11.2 Loader validations (hard fail)
The YAML loader must enforce:
- Keys are unique per kind (plots, fixtures, items, actors)
- All `ownerKey` and gate `toPlotKey` resolve
- Gates satisfy the per-direction constraints and unique (fromPlotKey, direction) per plot
- No ownership cycles
- TTL values are either `-1` or within `[0..worldClockStart]` (or a stricter rule you choose)

### 11.3 Deterministic builder pass (required)
After loading YAML, before gameplay:
1) Generate deterministic UUIDs from `kind:key`
2) Auto-create Actor slot plots for each Actor (deterministic UUIDs derived from actorId + slotRole)
3) Auto-create compartment plots for items of specific kinds (desk, backpack, etc) if desired
4) Rebuild indices

### 11.4 Optional seeded filler pass (allowed)
After deterministic builder, you may run a seeded filler pass:
- adds non-critical props and atmosphere
- can set mechanical fields for filler content based on `worldSeed`
- must not break solvability of critical YAML-defined paths

### 11.5 Gardener pass (LLM NPC)
After builder and filler:
- The Gardener (LLM-backed) updates **description fields only**
- Each description update is recorded with a timestamp and source marker
- The Gardener never edits mechanical fields

Descriptions can evolve during play to reflect actions and history, but mechanics remain stable.

---

## 12. Proof of Work (PoW) trace
Each turn produces a trace of:
- parse and normalization results
- planning steps and which gate failed first (or success)
- key evaluation receipts (including dice rolls and compile errors)
- commit deltas applied
- tick and TTL triggers applied

The player UI may show only the top-level hint. The full PoW exists for inspection and debugging.

---

## 13. Failure types
### 13.1 CompileException
- Invalid command syntax
- Invalid key compilation during planning (but key compile failure does not stop planning, it evaluates to false)

### 13.2 GateException
- Used only in INSPECT/PLAN for puzzle walls
- First failing gate only

### 13.3 EngineInvariantException
- Used for commit-time failures (bugs)
- Not a gameplay concept

---

## 14. Minimal v0 world for implementation
To validate the kernel, implement a tiny world from YAML:

- Land plot: `Room` (LAND)
- Land plot: `Hall` (LAND)
- Gate Thing: `Room EAST → Hall`, visible true, `keyString: "true"` (isOpen = true)
- Actor: `Player` in Room
- Item: `Watch` in Player.WRIST slot
- Item: `Book` on a Desk.TABLETOP compartment plot

Goals:
- GO EAST moves actor and ticks clock
- DROP watch moves it from wrist plot to land plot
- MOVE book from tabletop to BOTH_HANDS to desk drawer, demonstrating two-stage move
- TTL closes a gate or burns out a torch
- key compile fail yields deterministic false

---

## 15. Implementation checkpoints (tests)
These tests lock the design:

1) **One-parent invariant**
- Every Thing has exactly one ownerPlotId (or ROOT), and plot inventory indices match.

2) **Land plots are root-only**
- Loader rejects any land plot with ownerPlotId not ROOT.

3) **Gate per direction**
- Loader rejects two gates in same direction from same source plot.

4) **Turn consumption**
- Clock decrements even on planning failure.

5) **GateException is plan-only**
- Commit never throws GateException.

6) **Two-stage move**
- Phase 2 failure leaves item in P.BOTH_HANDS.

7) **Key compile fail**
- compile-fail logs trace and evaluates to false.

8) **TTL triggers**
- TTL triggers fire deterministically at the correct worldClockRemaining value.

---

## 16. Future extensions (non-blocking)
- More compartment roles (pockets, belt, sheath)
- Better name resolution scopes (visibility + reachability)
- Additional key DSL primitives (still compiled, still deterministic)
- Volume transfer actions (bottle to cup) using same pipeline
- More sophisticated Gardener narration policies

---

## 17. Appendix: Fixed decisions recap
- Actor slot plots are auto-created by loader, not authored in YAML.
- `P` as a destination resolves to `P.BOTH_HANDS` for staging.
- Directed navigation exits are Gate Things owned by Land plots.
- Key test strings compile JIT. Compile failure evaluates to false and is recorded.
- Every attempt consumes a turn. Clock ticks even on plan failure.
- TTL triggers are absolute and fire when worldClockRemaining hits that value.
- Gardener modifies descriptions only, never mechanics.

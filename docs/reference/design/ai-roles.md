# AI Roles Architecture (TODAY → TOMORROW → SOMEDAY)

Runtime contract reference:
- `docs/howto/runbooks/ai-cli.md`

## Assumptions & Constraints
- Engine output is the only authoritative mechanical truth per turn.
- Prompts must stay game-agnostic (no embedded lore/items/callouts or game-specific mechanics).
- “Close this project out” = “demoable, shippable AI + engine loop,” not an endless feature runway.
- All agents that change state must go through the Engine boundary; no narrator fiat.
- Patterns: Grounding (structured data, engine truth), Orchestration (deterministic loops, one step per tick), Verification (deterministic checks/judges), Trust UX (visible constraints, no hidden mechanics), Learning (process: BUJO + proofs-of-work, future judges/golden transcripts).
- Testing patterns: See `docs/reference/design/pattern-tests.md` for grounding/orchestration/verification/trust-UX/learning test strategies.

## TODAY (working)
- **Interpreter (runtime, 2025 mode):** deterministic mapper from player NL → canonical command; may not invent mechanics.
- **Engine:** executes commands, updates world, emits authoritative turn result (location header, description, exits/items/fixtures as appropriate).
- **Narrator:** rewrite layer for readability/flow; uses command-specific prompt variants (not a single global template). May add tone but must not add exits/items/fixtures/objectives/requirements/state changes.

### Narrator prompt specialization (runtime)
The narrator prompt is assembled per command intent using `CommandScanner` tokens and engine output shape.
Each variant receives only the minimum context needed to rewrite:
- **SCENE:** `LOOK` (no target), `LISTEN`, successful `MOVE/GO` arrivals → rewrite scene snapshot (location + description + fixtures/items), append exits verbatim.
- **LOOK_TARGET:** `LOOK <thing>`, `INSPECT` → rewrite action result only (no exits).
- **LOOK_DIRECTION:** `LOOK <direction>` / “what is east” → rewrite gate/exit action result; direction names must remain exact; no exits appended.
- **ACTION_RESULT:** generic verbs (`SEARCH/EXPLORE`, `TAKE/DROP/PUT`, `OPEN/USE`, `ATTACK/FLEE`, etc.) → rewrite action result only (no exits).
- **COLOR_EVENT:** rewrite the color line only (no exits).
- **EMOTE / CHECK_REQUEST / CHECK_RESULT:** rewrite emote/check lines only; no exits/location line.

Constraints:
- No new entities, exits, locations, or mechanics.
- Paraphrase is allowed if facts are preserved.
- Destination names and direction words must stay exact when used.
- Grounding guard is mode-specific and paraphrase-tolerant; on failure, the system falls back to deterministic engine output.

## TOMORROW (in motion / conceptual)
- **Storyteller (authoring):** interactive interview to produce a Game Spec (backstory, genre/tone, quests/puzzles, cast, fixtures/NPCs, constraints like cozy vs hero’s journey).
- **World Planner (authoring):** consumes Game Spec, outputs world plan (plots/rooms, connections, fixtures/items/NPC placeholders, short 1980-style names/descriptions); defines canonical mechanical layout.
- **Gardener (authoring):** consumes world plan, adds description overlay (“description stack”) without changing structure (no new exits/requirements/moves).
- These roles currently use CLI + YAML authoring (Builder/Architect/ZoneBuilder/Gardener CLIs) until prompts are refreshed.

### AI-assisted GDL design protocol (Adventure Vibe Coder)
1) Intake & constraints — Role: Producer/Director  
   - Inputs: design brief + canon + guardrails  
   - Process: interview one question at a time to fill constraints before story/backstory; distill scope, invariants, success criteria, and non-negotiables  
   - Outputs: Constraints Brief
2) Story spec — Role: Storyteller  
   - Inputs: Constraints Brief  
   - Process: shape backstory + core loop + beats + challenges + required artifacts  
   - Outputs: Story Spec
3) World plan — Role: World Planner  
   - Inputs: Story Spec + Constraints Brief  
   - Process: map plots, adjacency, gates/key expressions, and placements  
   - Outputs: World Plan
4) GDL synthesis — Role: GDL Synthesizer  
   - Inputs: World Plan  
   - Process: translate plan into GDL statements with consistent IDs/labels  
   - Outputs: GDL Source
5) Compile & validate — Role: Compiler/Validator  
   - Inputs: GDL Source  
   - Process: run GDL scan/compile + world validation, collect errors/warnings  
   - Outputs: Build Report (WorldRecipe/GameSave + validation findings)
6) Structured export — Role: Exporter  
   - Inputs: Build Report (on pass) + Story Spec  
   - Process: emit structured YAML and narrative assets  
   - Outputs: Structured Files (game.yaml + includes, narrative/backstory.md, optional world/crafting.yaml)
7) Runtime verification — Role: Verifier  
   - Inputs: Structured Files + Playbooks  
   - Process: run structured load + playbook integration tests  
   - Outputs: Test Results
8) Iterate — Role: Producer/Director  
   - Inputs: Build Report + Test Results  
   - Process: refine constraints/plan and queue next revision  
   - Outputs: Updated Constraints Brief -> back to step 1

## GamePlan Role Contracts (active target)
All outputs are single YAML documents with no extra commentary. If required inputs are missing, emit:
```yaml
error:
  reason: <short>
  missing_fields: [field1, field2]
```

### Constraints Interviewer (Producer/Director)
Purpose: collect constraints by asking one question at a time before narrative/backstory.
Inputs:
- Partial `constraints_brief`
- Prior answers (if any)
- Guardrails (always)
Invariants:
- Ask exactly one question per turn.
- Do not invent facts or narrative.
- Do not proceed to story/backstory until all required fields are filled.
Output schema:
```yaml
interview:
  status: ask|done
  constraints_brief:
    title:
    game_type:
    description:
    genre:
    motif: []
    player_role:
    win_conditions: []
    lose_conditions: []
    puzzle_count:
    mandatory_items: []
    guardrails: []
    success_criteria: []
  question:
    id:
    text:
    target_field:
    expected_format:
```

### Storyteller
Purpose: produce a concise story spec from the constraints.
Inputs:
- `constraints_brief`
Invariants:
- May elaborate only within constraints.
- No mechanics, maps, or gating rules.
Output schema:
```yaml
story_spec:
  backstory_summary:
  core_loop:
  key_beats: []
  challenges: []
  required_artifacts: []
```

### World Planner
Purpose: translate story spec into a mechanical world plan.
Inputs:
- `story_spec`
- `constraints_brief`
Invariants:
- Use key-expression strings for gates.
- No new mechanics beyond the story spec and constraints.
Output schema:
```yaml
world_plan:
  plots:
    - id:
      label:
      role: start|goal|hub|branch|puzzle|side
      description_stub:
  adjacency:
    - from:
      to:
      direction:
      gate_label:
      key_expression:
      visible:
      hint:
  placements:
    fixtures:
      - id:
        plot_id:
        label:
        purpose:
    items:
      - id:
        plot_id:
        label:
        purpose:
    actors:
      - id:
        plot_id:
        label:
        purpose:
```

### GDL Synthesizer
Purpose: convert the world plan into GDL-ready identifiers.
Inputs:
- `world_plan`
Invariants:
- Preserve IDs and labels exactly.
- Do not invent new entities beyond the plan.
Output schema:
```yaml
gdl_source:
  source: |
    thing("game").fixture("self").kind="game".seed=123.preamble="..."
```

## SOMEDAY (planned)
- **Smart NPCs (in-world):** produce dialogue/ambient actions or proposed moves; any state change must execute via Engine (see `docs/reference/design/smart-actors.md`).
- **AI Characters (protagonist/antagonist):** per-turn planned actions executed via Engine; they are players with goals, not narrators.
- **Demo Actor:** persona/script runner that feeds player-like utterances through Interpreter → Engine → Narrator for reproducible tours.

## SOMEDAY additions (paste-ready backlog)
- Agent contract pack: purpose/inputs/outputs/invariants per role; explicit “may invent / may not invent.”
- Judge checks: generic validator to reject/flag AI output that introduces mechanics or breaks output contract.
- Golden transcript tests: small fixtures per agent (esp. Narrator) with expected outputs/constraints (e.g., exactly one exits footer, no new exits).
- Turn scheduler for multi-actors: defines turn order and how NPC/Character agents submit actions via the Engine boundary.
- Character sheets (data, not prompt lore): personas/goals/constraints stored in world data to keep prompts generic.
- NPC/Character memory model: short “memory snippets” per actor/plot to influence dialogue/tone without inventing mechanics.
- Ambient actors system: background “color” events that never change state.
- Demo-mode script runner: repeatable “tour” playthrough that drives inputs and yields stable receipts.
- Save/load + replay receipts: deterministic replay of transcripts for reproducible demos/debugging.
- Content completion gate: define minimum content pack for “done” (N finished games, world pass + garden pass + demo transcript).

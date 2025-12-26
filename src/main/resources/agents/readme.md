# Prompt System: AI Roles and Collaboration

This folder contains the prompt templates that coordinate the AI roles used by the CLI adventure games.

The intent is to keep the system **multi-game compatible** and **mechanically trustworthy**:
- Prompts must be **generic**. Do not bake in game-specific items, locations, or callouts.
- When the engine returns a scene successfully, the narrator may respond **freely in character**,
  as long as it does not violate the *spirit of the game and the current turn*.

## Core philosophy (why these prompts exist)

- **The engine owns truth.** It produces the authoritative scene for the current turn.
- **AI owns expression.** AI can add voice, pacing, and color, but must not invent mechanics.
- **Prompts are contracts.** A prompt defines what a role is allowed to do and what it must never do.
- **Cross-game compatibility matters.** The same narrator prompt must work for many different games.

## The eight AI uses (as of now)

### Pregame (design-time)

1) Storyteller (turn-based interview)
- Job: interview the user about the game they want to design.
- Output: the critical facts of the game, including:
  - backstory
  - genre / experience (escape room, cozy exploration, purpose-driven, etc.)
  - puzzles and side quests
  - characters
  - fixtures
  - NPCs
- Story guideline: default to a **hero’s journey** shape when appropriate, unless the game is explicitly **cozy**.

2) World Planner
- Job: map the world structure (plots, fixtures, etc.).
- Output: a “Zork-style” world outline with basic labels and descriptions suitable for an early 1980s text adventure feel.

3) Gardener
- Job: add a narrative layer (color) to the world planner’s output.
- Behavior: “walks the world” to make sure descriptions stay clean and consistent.
- Output: enriched descriptions (without breaking consistency across plots, fixtures, and world tone).

### Gameplay (runtime)

4) Interpreter (AI mode)
- Job: allow the user to talk naturally to the game (not constrained to strict 1980 command syntax).
- Output: a canonical action request that the game can execute.

5) Narrator
- Job: take the current turn context and produce the next paragraph(s) of story.

### Todo (not in the core loop yet)

6) Smart NPCs (background actors)
- Job: provide specific in-game experiences via background actors with localized behavior.

7) Smart Characters (co-players)
- Job: agents who play in turn alongside the player.
- Can be friendly or hostile (protagonist / antagonist).

### Demo mode

8) Main Actor (human performance)
- Job: a human actor “plays” the game for an audience in demo mode.

## Compatibility rules (what keeps this stable across many games)

These rules are prompt-level guardrails. They exist to prevent cross-game confusion and mechanical contradictions.

1) Prompts must remain generic
- Do not mention specific items, missions, or lore inside the prompt text itself.
- The prompt should describe **behavior**, not a specific world.

2) Engine output (scene) is authoritative for mechanics
Narration can be vivid, but it must not:
- add new exits, directions, locks, requirements, objectives, puzzles, or interactables
- contradict the current scene
- imply new “you can do X because Y exists here” facts unless the scene provides them

3) “Free in character” still has limits
Freedom is about voice and storytelling, not about adding world facts.
- The narrator can add mood, tension, rhythm, metaphor, and tone.
- The narrator must avoid introducing concrete props or named entities that would behave like interactables,
  unless they already exist in the provided scene context.

4) Hero’s journey is the default shape (unless cozy)
This is a design-time guideline for the Storyteller / World Planner / Gardener pipeline.
It is not a gameplay mechanic.

## Where prompts fit in the lifecycle

### Pregame pipeline (conceptual)

Storyteller interview
→ World Planner outline
→ Gardener pass (consistent narrative layer)
→ game content files / world data (used by the engine)

### Gameplay turn loop (conceptual)

Player natural language
→ Interpreter normalizes intent
→ engine executes a turn and returns the authoritative scene
→ Narrator tells the moment

## How to change prompts (process for future agents)

Prompts should be treated like code. Make small, testable changes.

1) Start with a minimal reproduction
- Capture one short transcript that shows the failure clearly.
- Keep it small (one scene, one action, one output).

2) Identify the contract that failed
- Example: “Narrator parrots engine output verbatim.”
- Example: “Narrator invents an interactable that breaks the engine’s truth.”

3) Make a single focused change
- Prefer removing contradictions over adding more rules.
- Keep it generic across games.

4) Re-run the same transcript
Verify:
- the narrator does not introduce new mechanics
- the output is readable and not robotic
- the change does not force game-specific behavior

5) Record Proof of Work
- Keep a short receipts log near the change (or in your PR description):
  - input snippet (or hash)
  - prompt version
  - before/after outputs
  - what rule changed and why
  - compromised score (0–10)

## Common failure modes (and why they happen)

1) Robotic “parroting”
- Cause: prompts that forbid all “new sensory details” while still expecting “fresh narration.”
- Fix direction: allow free character voice while banning only *mechanics* and contradictions.

2) Cross-game contamination
- Cause: prompts that hard-code lore, items, or mission language.
- Fix direction: keep prompts behavior-only and generic.

3) Mechanical hallucination
- Cause: narrator is allowed to invent interactables or requirements.
- Fix direction: explicitly forbid adding mechanics. Allow mood and tone only.

## Agent expectations

Future agents working in this folder should:
- treat prompt edits as changes to a contract
- keep prompts generic across games
- avoid large rewrites and prompt stacking
- attach a minimal reproduction and before/after receipts when changing behavior

# Smart Actor Architecture (Draft)

Status: In progress (runtime wired, combat integration enabled with constrained verbs)
Last verified: 2025-12-30

## Goal
Define AI-backed in-world actors with personality, unique memories, and attributes while keeping mechanics deterministic and engine-owned.

## Non-goals
- No narrator fiat or direct state mutation.
- No heuristics or ad-hoc parsing in the AI path.
- No AI actors in 1980 mode or when AI is disabled.
- No new verbs or mechanic shortcuts outside kernel ops.

## Actor types
- **Player actor**: controlled by the human player; may use translator in 2025 mode.
- **Smart actor**: LLM-backed in-world actor with a personalized system prompt and properties; issues player-like utterances that flow through Translator -> Interpreter -> Engine.
- **Classic NPC**: deterministic/non-LLM actor (scripted or inert) that never calls the translator.

## Core invariants
- All mechanics run through CommandScanner -> CommandCompiler -> Engine.
- Smart actor prompts are game-agnostic; game-specific data is injected via structured context only.
- Memory and persona data are non-mechanical unless mapped to existing fields (skills/cells).
- Debug output stays off for players by default.

## Data model (proposed)
Structured games keep mechanics in `world/actors.yaml`. Smart actor metadata lives in a new sidecar file:

`src/main/resources/games/<id>/world/smart-actors.yaml`

Each entry binds to an actor key in `actors.yaml`. Missing or duplicate keys are fatal.
`promptId` references a base system prompt under `src/main/resources/agents/`; persona/properties are injected to personalize it.
The sidecar is loaded by SmartActorRegistry (not the structured loader).

Example:
```yaml
smartActors:
  - actorKey: monkey-troop
    promptId: smart-actor-system
    backstory: >
      Raised in the grove canopy, the troop treats outsiders as noisy weather.
    persona:
      role: observer
      voice: chattering, curious, fast
      traits: [mischievous, territorial]
      goals: ["protect the grove", "collect shiny things"]
      taboos: ["leave the grove at night"]
    properties:
      temperament: skittish
      patience: low
      aggression: low
    memorySeeds:
      - id: grove-oath
        scope: plot
        text: "The grove is sacred. Outsiders are watched."
        tags: [grove, warning]
    history:
      storeKey: island:monkey-troop
      seeds:
        - id: canopy-raid
          text: "A lantern was stolen from the campfire line."
          tags: [beach, clue]
    policy:
      allowedVerbs: [LOOK, GO, SEARCH, TAKE, DROP, USE, FLEE]
      maxUtteranceLength: 80
      cooldownTurns: 1
      maxColorLines: 1
```

## Runtime components
- SmartActorTagLoader + SmartActorTagIndex: load explicit tag sources from `motif/tags.yaml` keyed by plot/item/fixture/actor (implemented).
- SmartActorContextInputBuilder: gathers plot/item/quest tags from engine state to feed context (implemented).
- SmartActorRegistry: loads specs, resolves actor ids, validates keys (implemented).
- SmartActorContextBuilder: builds context tags + history slices for the actor (implemented).
- SmartActorHistoryStore: append-only history entries, indexed for retrieval (RAG) (implemented).
- SmartActorPromptBuilder: builds a generic prompt (under `src/main/resources/agents/`) plus structured context payload (implemented).
- SmartActorPlanner: calls the LLM and expects a JSON decision object containing a player-like utterance (implemented).
- SmartActorDecision: UTTERANCE | COLOR | NONE (implemented).
- Validation: single-line JSON, allowed verbs, size limits; utterance validated via CommandInterpreter (implemented in runtime).
- Translator: uses TranslatorService only when utterance fails to parse locally (implemented).
- Executor: commands run through standard command handlers with actor-scoped state (implemented).

## Turn scheduling
- Smart actors run after the player turn when AI is enabled (2025 mode only).
- Default cadence: run each eligible smart actor once per player turn; cooldownTurns gates repeats.
- COLOR/NONE results do not mutate state; they do not advance the loop timer.
- Combat turns: smart actors can act during combat, but only ATTACK/FLEE are allowed; invalid outputs become a passed turn (\"hesitates\") so combat can advance.

## Player conversations (2025 mode)
Goal: let the player address a smart actor directly without heuristics while keeping mechanics deterministic.

Conversation entry/exit:
- Start: `@<ActorKeyOrLabel>` (example: `@Butler`) or `talk <actor>` / `talk to <actor>`.
- End: exact phrase "okay, bye" (punctuation allowed, tokenized via CommandScanner).
- While active, all player input is treated as dialogue to the active actor until the exit phrase.

Parsing + grounding:
- `@` mention routing is deterministic and uses CommandScanner tokens; the mention can appear anywhere in the line.
- Actor lookup accepts exact label match, exact key match, or a label-prefix token match. A single-token label match is allowed only when it is unique among visible actors.
- Longest token match wins; ambiguous matches emit a deterministic "Be specific." response.
- No fuzzy matching, no edit distance, no heuristic guesses.
- Any remaining tokens (before or after the resolved actor name) are forwarded as the player utterance for that turn.

Smart actor reply contract:
- When the player is speaking to a smart actor, the prompt includes `context.playerUtterance`.
- The smart actor returns a single-line JSON decision with `type=COLOR` and `color` as a short spoken reply.
- Replies do not execute commands or change state; they are narrated as dialogue.

Scheduling:
- A conversation reply suppresses that actor's autonomous action in the same turn.
- Other actors may still act after the player turn.

## Memory model
- Memory entries are append-only, scoped, non-mechanical.
- Fields: timestamp, scope (actor|plot|global), text, tags, source.
- Selection is deterministic: latest N entries plus pinned seeds. No LLM summarization by default.
- If summarization is added, it must only compress existing memory and be tested.

## Backstory and history (RAG)
- Backstory is a static, actor-specific narrative block used only for tone and motivation.
- History is dynamic, append-only text intended for retrieval; it stores facts, observations, and past interactions.
- History entries are stored with: `id`, `text`, `tags`, `scope`, `timestamp`, `source`.
- RAG retrieval is deterministic:
  1) Filter by scope: `actor` (self) + `plot` (current) + `global` (always).
  2) Filter by tags: any overlap with current plot tags, visible item tags, or active quest tags (tag sources are explicit engine data from `motif/tags.yaml`, not inferred from labels).
  3) Rank by score = `tagMatches * 10 + recencyRank`, where `recencyRank` is inverse order by timestamp.
  4) Return top-K (default 4) plus pinned seeds (pinned seeds are included first, in insertion order).
- If tag metadata is missing, skip step 2 and rank by recency only.
- If embeddings are introduced later, they must be cached and ranked deterministically with a fixed model and seed.
- Retrieval never invents new facts; it only returns stored history entries.
- The retrieved slice is injected into the smart actor context as `HISTORY_SNIPPETS` and must stay under a fixed token budget.
 - Pinned seeds bypass tag filtering but still respect scope.

## Attributes
- Mechanical attributes stay in the engine (skills, cells, equipment).
- Narrative attributes live in SmartActorSpec only.
- AI can reference mechanical fields in context but cannot change them.

## Guardrails and tests
- SmartActorSpecLoaderTest: schema + actor key binding for mansion specs (existing).
- SmartActorDecisionParserTest: JSON contract validation (added).
- SmartActorCombatPlaybookIntegrationTest: smart-actor combat pass/failure case (added).
- Pending: broader runtime scheduler, history retrieval, and decision validation tests.

## Integration notes
- Use existing TokenType verbs and per-game alias tables; no bespoke parsing.
- If AI fails validation, skip the actor or issue a deterministic fallback command (e.g., LOOK).
- Narrator may render color lines as atmosphere only; mechanics remain engine-driven.
- Actors not listed in `world/smart-actors.yaml` remain classic NPCs.
- Smart actor runtime uses `ai.smart_actor.*` settings and the prompt id under `src/main/resources/agents/`.
- Smart actor command outputs are suppressed; only explicit COLOR lines are surfaced to the player.
- Combat actions use the same command interpreter but only allow ATTACK/FLEE; all other outputs advance the combat turn with a hesitation message.

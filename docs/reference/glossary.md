# Glossary

- AI mode (2025): CLI mode that uses translator + narrator services while keeping mechanics deterministic.
- Classic mode (1980): CLI mode that skips AI and runs engine output directly.
- CommandScanner: Tokenizes player input into `TokenType` tokens.
- CommandCompiler: Builds a structured command from tokens; failure triggers translator in 2025 mode.
- CommandHandlers: Per-command classes that execute compiled commands (look, go, take, use, craft).
- Engine runtime: In-memory session state (registry, inventory, plot, triggers, loop).
- Narrator: AI service that rewrites engine output without changing mechanics or facts.
- Translator: AI service that converts free-form input into a single command string.
- Motif verbs: Game-specific aliases that map thematic verbs to core commands.
- Smart actor: AI-backed actor with memory, policy, and decision bounds; acts through the interpreter.
- Key expression: Conditional expression used for gates, visibility, and triggers.
- Integrity check: Deterministic, non-LLM validation that checks reachability and missing references.
- Structured load: Loading `game.yaml` plus structured `world/`, `narrative/`, and `motif/` assets.
- Playbook: YAML script that drives integration tests through a fixed sequence of commands.
- GDL: Game Description Language used for authoring and compilation into structured YAML.
- GamePlan: AI-assisted interview output used to guide structured world building.

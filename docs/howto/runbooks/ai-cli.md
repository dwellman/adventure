# AI/CLI Runbook (Authoritative)

Status: Authoritative (runtime + CLI contract)
Last verified: 2026-01-02

## Purpose
Define the runtime contract for translator -> engine -> narrator, including classic fallback and prompt wiring.

## Inputs
- Structured game YAML, runtime prompts, and configuration (see "Inputs and configuration").
## Outputs
- Raw engine output and (when enabled) narrated output; smart-actor turn snapshot when configured.

## BUUI output styling
BUUI supports inline markdown markers for emphasis across all CLI output (text, lists, tables, menus). Colors are applied via the active style sheet, not inline tags.

Examples:
```
This is **bold**, _italic_, and `code`.
```

Notes:
- Supported colors include base + bright variants (e.g., `green`, `bright_green`) via the active `BUUI_STYLE` file.
- Set `NO_COLOR=1` to suppress ANSI codes.
- For programmatic tags, use `BuuiMarkup.bold(...)` and `BuuiMarkup.italic(...)` (inline color tags are invalid and will error).
- `BuuiConsole.print(...)` / `ConsolePrinter.print(...)` assume markdown input; use `println(...)` for plain wrapped text.
- `BuuiMarkdown` uses `MarkdownScanner` to detect markup before applying narration blockquotes.
- Markdown pipeline: `MarkdownScanner` → `MarkdownCompiler` → BUUI-ready lines; `MarkdownCompiler.compile(Path, ...)` returns a `MarkdownDocument` with tokens + compiled lines.
- Use `ConsolePrinter.printCompiled(...)` to output precompiled markdown lines with the standard left gutter.
- Styling is driven by a CSS-like `.style` file configured via `BUUI_STYLE` (env or system property); if unset, markdown renders with no ANSI formatting.
- If `BUUI_STYLE` points to an invalid or unreadable style file, markdown rendering halts and reports a validation error to stderr.
- `standard.style` is a sample style file; set `BUUI_STYLE=standard.style` to enable it.
- Set `BUUI_GUTTER` or `BUUI_EDGE_PADDING` (env or system property) to adjust left gutter and edge padding defaults.
- Table rendering uses `RenderStyle.maxWidth` (defaults to `COLUMNS`); override via `TableBuilder.maxWidth(...)` or `RenderStyle.withMaxWidth(...)`.
- Inline color tags raise a markdown validation error and are reported to stderr.

### Markdown style sheets
Format: `selector { property: value; }` blocks (CSS-like, no inline styles).

Supported selectors:
- `heading-1`, `heading-2`, `heading-3`, `heading-4` (aliases: `h1`-`h4`)
- `paragraph` (alias: `p`)
- `list-item` (alias: `li`)
- `list-bullet`
- `table-header` (alias: `th`)
- `table-cell` (alias: `td`)
- `blockquote`
- `code-block` (alias: `pre`)
- `inline-code` (alias: `code`)
- `scene-break` (alias: `hr`)
- `strong` (alias: `bold`)
- `emphasis` (alias: `em`, `italic`)
- `section-label`, `exit-line`

Supported properties:
- `color`: ANSI color name (e.g., `bright_blue`) or `none`.
- `font-weight`: `bold` or `normal`.
- `font-style`: `italic` or `normal`.

### Markdown style mapping (standard.style example)
- Headings: `#` → bold + bright_blue; `##` → bold + bright_cyan; `###` → bold + bright_magenta; `####+` → bold + bright_white.
- Table headers: bold + bright_white.
- Paragraphs/list items/table cells: default style (no color); inline markers still apply.
- List bullets/numbers: bright_cyan.
- Blockquotes (`>`): italic + bright_black.
- Code fences: bright_black (no inline markdown inside code blocks).
- Inline code: bright_black.
- Inline bold (`**`/`__`): bold.
- Inline italics (`*`/`_`): italic.
- Scene breaks (`***`, `---`): bright_black.

## Tests
- See "Validation gates" for the standard test commands.

## Scope
- AI-enabled CLI flow (2025) and classic fallback (1980).
- Translator/Engine/Narrator role contract and invariants.
- Prompt locations, configuration, and debug toggles.

## Modes
- **1980 (classic):** AI disabled; commands run through scanner/compiler/interpreter only.
- **2025 (AI):** translator + narrator enabled; requires `OPENAI_API_KEY`. Missing key disables AI with a warning and keeps the classic path.

## Inputs and configuration
- Structured game definition: `src/main/resources/games/<id>/game.yaml`
- Game backstory (required by GameCli): `src/main/resources/games/<id>/narrative/backstory.md`
- Storybook bundle (if present): `src/main/resources/storybook/<id>/`
- Smart actor specs (optional): `src/main/resources/games/<id>/world/smart-actors.yaml` and tag metadata in `src/main/resources/games/<id>/motif/tags.yaml`
- Runtime prompt templates: `src/main/resources/agents/translator.md` (translator); narrator prompts are compiled on demand in `src/main/java/com/demo/adventure/ai/runtime/NarratorPromptBuilder.java`.
- Smart actor prompt template: `src/main/resources/agents/smart-actor-system.md`
- Config sources (priority): JVM props > env vars > `application.properties`
- Prompt logging: `ai.prompts.print=true` prints system + user prompts before every LLM request (default false).
- Translator config: `ai.translator.model`, `ai.translator.temperature`, `ai.translator.top_p`, `ai.translator.logprobs`, `ai.translator.top_logprobs`, `ai.translator.debug`
- Narrator config: `ai.narrator.model`, `ai.narrator.temperature`, `ai.narrator.top_p`, `ai.narrator.logprobs`, `ai.narrator.top_logprobs`, `ai.narrator.debug`
- Smart actor config: `ai.smart_actor.scope` (`local` = only actors in the player’s plot; `global` = all actors), `ai.smart_actor.debug`

## Constraints
- Structured load must fail loudly; no silent fallback for structured files.
- Translator output must be a single-line command string or `EMOTE: ...` line (validated before execution).
- Narrator prompts are compiled per command variant with minimal context (scene/action/color/emote/check).
- Interaction prompts (dice/choice/confirm) block all other routing until resolved.
- Conversations are explicit: `@<actor>` or `talk <actor>` start, “okay, bye” ends, and all input routes to the active actor until exit.
- `quit`/`q` ends the current game and returns to the main menu; `q` from the menu exits the program.
- Classic fallback runs only when AI is disabled and only when the compiler cannot parse the input; it must use scanner tokens only.
- Smart actors run only in 2025 mode with AI enabled; during combat they are limited to ATTACK/FLEE and otherwise pass with a hesitation.

## Role contract (2025)
- **Translator:** deterministic mapper from player text to a canonical command (or `EMOTE: ...` when the input is a valid non-command action). Inputs: player text, visible fixtures/items, inventory, last scene text. Output: one command or EMOTE line only. No questions or colors.
- **Engine:** authoritative state transition + factual turn snapshot (location, fixtures/items/actors, exits).
- **Narrator:** stateless rewrite of engine output with tone; cannot add exits/items/fixtures/objectives or change state. Exits are only appended on scene output.
- **Smart actors:** operate after player turn using the same interpreter/handlers; combat verbs restricted to ATTACK/FLEE.

## Runtime flow (AI enabled)
1) If an interaction prompt is pending (dice/choice/confirm), the CLI accepts only the expected input and resolves it.
2) If conversation is active, player input is routed to the addressed actor; “okay, bye” ends the conversation.
3) Otherwise, player input → local parse (scanner/compiler/interpreter).
4) If parse fails and AI enabled → TranslatorService.
5) TranslationOrchestrator validates translated command via CommandInterpreter (single pass).
6) Engine executes the command and emits raw output + snapshot.
7) Narrator rewrites engine output using a command-specific prompt (no mechanics added).
8) Smart actors (if enabled) may act after the player turn.
9) Error path: translation failures request a rephrase via HELP; narration failures or AI-disabled paths emit deterministic engine output (no mechanics added).
   - Emote path: if the translator returns `EMOTE: ...`, the engine may emit:
     - `EMOTE: <text>` (no-check), or
     - `CHECK_REQUEST: dice(20,15) | EMOTE: <text>` when a roll is required.
     The narrator renders these as a one-paragraph beat without location/exits; `CHECK_REQUEST` must ask for the roll. After the roll, the engine emits `CHECK_RESULT: roll=<n> target=15 outcome=SUCCESS|FAIL | EMOTE: <text>` and the narrator resolves the beat. Emote/dice turns do not advance engine time.

## Validation gates
- Structured load guard: `mvn -q -Dtest=GameMenuStructuredLoadTest test`
- Deterministic integrity sweep: `mvn -q -Dtest=GameIntegrityTest -DrunIntegrity=true test`
- Adventure playbook integration: `mvn -q -Dtest=IslandAdventurePlaybookIntegrationTest,MansionAdventurePlaybookIntegrationTest,WesternAdventurePlaybookIntegrationTest,SpyAdventurePlaybookIntegrationTest test`
- Minigame regression: `mvn -q -Dtest=MiniGamePlaybookIntegrationTest test`
- Smart-actor deterministic runs: `mvn -q -Dtest=SmartActorCombatPlaybookIntegrationTest test`
- Broad sweep: `mvn -q test`

## Verification checklist
- Backstory present in each structured game (`narrative/backstory.md`).
- Translator output is single-line and parser-valid.
- Narrator includes exits only on scene output and does not invent mechanics.
- Inventory/crafting/combat routes through scanner/compiler/interpreter.
- Receipts emitted for command actions (cell ops, combat events).

## References
- AI roles and future roadmap: `docs/reference/design/ai-roles.md`
- CLI architecture overview: `docs/reference/design/cli.md`
- Engine architecture: `docs/reference/design/engine.md`

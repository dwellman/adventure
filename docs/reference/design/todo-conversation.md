# Smart Actor Conversation (@mention) — Completed

Status: Done (2025-12-30)

Completed items:
- Added TALK command support (TokenType, CommandAction, CommandCompiler, handler, help text).
- Added @mention routing + conversation state with "okay, bye" exit (CommandScanner tokenization only).
- Added talk runtime path to smart actors (playerUtterance in prompt, reply handling, suppress same-turn auto action).
- Updated smart actor prompt template for conversation replies.
- Moved mansion butler to the Hall and verified visibility in the start plot.
- Updated prompts/tests/goldens impacted by the changes.
- Ran targeted tests and recorded results in BUJO/journal.

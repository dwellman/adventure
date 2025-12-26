# ADR 0002: Rename engine session to engine runtime

Status: Accepted
Date: 2025-12-26

## Context
The package name `engine.session` did not convey the broader responsibilities of the runtime loop, command context, and trigger resolution. The term "runtime" is used elsewhere in docs and CLI flow descriptions.

## Decision
Rename `com.demo.adventure.engine.session` to `com.demo.adventure.engine.runtime` and update imports/tests/docs accordingly.

## Consequences
- Consistent terminology across code, docs, and runbooks.
- Cleaner package layout and easier navigation in the engine subtree.

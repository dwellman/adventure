# ADR 0003: Gate integrity checks behind an explicit flag

Status: Accepted
Date: 2025-12-26

## Context
Game integrity checks perform bounded reachability analysis and can exceed the typical unit test runtime budget. Running them on every test suite execution slows feedback cycles.

## Decision
Gate the integrity checks behind an explicit flag (`-DrunIntegrity=true` or `RUN_INTEGRITY=true`) so default suites skip the long run.

## Consequences
- Fast default test runs.
- Integrity validation remains available for CI or pre-release verification.

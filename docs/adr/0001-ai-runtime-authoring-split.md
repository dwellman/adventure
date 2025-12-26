# ADR 0001: Split AI runtime and authoring packages

Status: Accepted
Date: 2025-12-26

## Context
AI services were mixed between runtime concerns (translator/narrator) and authoring tooling. This made ownership unclear and documentation harder to align with the CLI runtime flow.

## Decision
Split AI code into:
- `com.demo.adventure.ai.runtime` for translator/narrator services, prompt templates, and runtime orchestration.
- `com.demo.adventure.ai.authoring` for authoring-only clients used by GamePlan and related tools.

## Consequences
- Runtime flow documentation can map directly to `ai.runtime` packages.
- Authoring tools remain isolated and can evolve without impacting CLI runtime.
- Imports and docs must reference the new package paths.

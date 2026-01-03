# Design Patterns and Test Coverage

This document defines the usage guidelines, design patterns, and test patterns for core project practices.

## Grounding

Usage guideline
- Use when output must be constrained by authoritative context (docs, rules, lore, policies).
- Use when hallucination risk is higher than minor style drift.
- Use when you can name the sources that must be relied on.

Design pattern
- Retrieval as a first-class dependency (port/adapter).
- Context bundle passed into Translate and Narrate.
- Policy: no propose or narrate without retrieved anchors.

Testing patterns
- Retrieval contract tests: given a query, required sources are retrieved.
- Coverage tests: answers must cite or attach anchors.
- Regression tests when corpus changes (golden sets for key queries).

## Orchestration

Usage guideline
- Use when the work requires tools, multi-step procedures, or bounded loops.
- Use when actions must be sequenced safely (one step per tick).
- Use when you need traceability for what happened and why.

Design pattern
- Orchestrator as a state machine controlling tool calls.
- Ports/adapters for tools (compiler, tests, game engine, search).
- Policy objects for step limits, tool selection, and escalation.

Testing patterns
- Tool contract tests (mock tool responses, verify calls and ordering).
- Replay tests (recorded tool traces reproduce behavior).
- Loop-limit and timeout tests (thrash prevention).

## Verification

Usage guideline
- Use when correctness is binary and must be machine-checked (compile, tests, invariants).
- Use when you want fail-fast safety and reproducible receipts.
- Use when you need detection of thrash, cycles, and contradictions.

Design pattern
- Deterministic judge boundary (compiler/tests/invariant engine).
- Validator layer separate from executors.
- Receipts/events as first-class, machine-generated artifacts.

Testing patterns
- Property/invariant tests (state must always satisfy constraints).
- Golden/master tests (expected outputs or event streams).
- Mutation tests (prove tests detect defects).
- Runtime invariant checks (assert plus receipt).

## Trust UX

Usage guideline
- Use when users must understand, approve, override, or recover safely.
- Use when ambiguity must be surfaced rather than hidden (unknown refs, missing data).
- Use when you need a consistent fallback that preserves trust.

Design pattern
- Dual-channel output: user message plus receipts.
- Explicit resolution policies (QUERY_STRICT vs COMPUTE_FALLBACK).
- Fallback policy that is deliberate, not incidental.

Testing patterns
- Fallback tests (I do not know what that is on unknown query).
- Explanation snapshot tests (stable user-facing narratives).
- Receipt completeness tests (every action emits required receipts).
- Error-path contract tests (consistent messages plus reasons).

## Learning

Usage guideline
- Use when you want improvement without sacrificing correctness guarantees.
- Use when you can separate generate from judge/score.
- Use when feedback can be captured and replayed.

Design pattern
- Scorecard plus judge interface (separate from generation).
- Configuration-driven thinking style and loop budgets (OODA controls).
- Offline evaluation harness (data-driven iteration) rather than online self-modifying logic.

Testing patterns
- Regression suites for prompts or behavior changes.
- A/B evaluation harness with fixed test sets and scores.
- Drift tests (quality does not degrade beyond thresholds).
- Data quality tests (sanitization and input validity).

## Testing gaps (TODO)
- DONE: Grounding - retrieval contract tests for translator/narrator context bundles (TranslatorPromptCoverageTest, NarratorPromptSelectionTest).
- DONE: Grounding - anchor coverage tests (NarratorFallbackTest.snapshotFallbackAnchorsLocationAndExitsOnMultiSceneError).
- DONE: Grounding - golden regression set for key queries when corpora change (TranslatorPromptGoldenTest, NarratorPromptGoldenTest).
- DONE: Orchestration - tool contract tests (TranslationOrchestratorTest.translatesOnceWhenLocalParseFails).
- DONE: Orchestration - invalid-output tests (TranslationOrchestratorTest.failsOnInvalidTranslatorCommand).
- DONE: Verification - property/invariant tests for combat/receipt consistency (CombatEngineTest.attackKeepsHealthInBounds).
- DONE: Verification - golden event-stream tests for multi-step flows (CombatEngineTest.receiptStreamMatchesMultiStepFlow).
- DONE: Verification - mutation tests to validate detection of defects (CellMutationReceiptMutationTest.detectsTamperedReceiptFields).
- DONE: Trust UX - strict unknown-reference fallback tests (GameCliCombatErrorTest.attackUnknownTargetEmitsReceiptAndMessage).
- DONE: Trust UX - receipt completeness tests per action (CombatEngineTest.attackMissRecordsReceiptsWithoutCellMutation).
- DONE: Trust UX - error-path contract tests for consistent messages (GameCliCombatErrorTest.fleeWithoutEncounterReportsNotInCombat).
- DONE: Learning - prompt regression suites with fixed inputs (TranslatorPromptGoldenTest, NarratorPromptGoldenTest).
- DONE: Learning - A/B evaluation harness scaffolding and drift tests (PromptEvaluationHarnessTest).
- DONE: Learning - data quality tests for prompt inputs and retrieval hygiene (TranslatorPromptCoverageTest.promptSanitizesInputsAndDefaultsMissingLists).

## Pattern Coverage Matrix

Grounding
- Retrieval contract: `TranslatorPromptCoverageTest`, `NarratorPromptSelectionTest`
- Anchor coverage: `NarratorFallbackTest.snapshotFallbackAnchorsLocationAndExitsOnMultiSceneError`
- Regression (golden prompts): `TranslatorPromptGoldenTest`, `NarratorPromptGoldenTest`

Orchestration
- Tool contracts: `TranslationOrchestratorTest.translatesOnceWhenLocalParseFails`
- Invalid outputs: `TranslationOrchestratorTest.failsOnInvalidTranslatorCommand`

Verification
- Invariants: `CombatEngineTest.attackKeepsHealthInBounds`
- Golden receipts: `CombatEngineTest.receiptStreamMatchesMultiStepFlow`
- Mutation detection: `CellMutationReceiptMutationTest.detectsTamperedReceiptFields`

Trust UX
- Unknown references: `GameCliCombatErrorTest.attackUnknownTargetEmitsReceiptAndMessage`
- Error path contracts: `GameCliCombatErrorTest.fleeWithoutEncounterReportsNotInCombat`
- Fallback behavior: `NarratorFallbackTest`

Learning
- Prompt regression: `TranslatorPromptGoldenTest`, `NarratorPromptGoldenTest`
- A/B + drift scoring: `PromptEvaluationHarnessTest`

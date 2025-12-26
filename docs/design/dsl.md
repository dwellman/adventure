# DSL Design Specification: GDL, GEL, PIL

Overview

This document defines the BNF (Backus-Naur Form) grammar, design theory, acceptance criteria, and verified gap analysis for the three core DSLs in the system:

- GDL - Game Definition Language
- GEL - Game Expression Language
- PIL - Player Interaction Language

Each grammar is designed to support a deterministic, modular game engine architecture. This document includes a precise delta between the specification and the actual codebase, to support implementation planning.

## 1. GDL - Game Definition Language

Purpose

Defines the structure and starting state of the game world. This includes entities, their fixtures, and attributes. GDL is static and declarative. It contains no behavior, evaluation, or side effects.

Acceptance Criteria

- Can define thing, actor, and fixture declarations
- Can assign literal values and optional expressions
- Can serialize to/from a known object model
- Produces no side effects on load
- Compatible with static analysis tooling

Rules

- Expression values must be quoted strings (for example `keyString="HAS(\"Key\")"`). Unquoted GEL expressions are invalid in GDL.

BNF Grammar

```bnf
<gdl> ::= <declaration>*

declaration ::= thing_decl | actor_decl

thing_decl ::= "thing" "(" <id> ")" "." <fixture_decl>
actor_decl ::= "actor" "(" <id> ")" "." <fixture_decl>

fixture_decl ::= "fixture" "(" <id> ")" "." <attribute_assign> ("." <attribute_assign>)*

attribute_assign ::= <id> "=" <value>

<value> ::= <literal> | <expression>

<literal> ::= <string> | <number> | <boolean> | <list>
<list> ::= "[" [<value> ("," <value>)*] "]"
<id> ::= /[a-zA-Z_][a-zA-Z0-9_]*/
<string> ::= '"' .*? '"'
<number> ::= /[0-9]+(\.[0-9]+)?/
<boolean> ::= "true" | "false"
<expression> ::= see GEL
```

Gaps in Code (as of 2025-12-21)

- No serializer or formatter to emit GDL from an in-memory `GameSave`.

## 2. GEL - Game Expression Language

Purpose

Provides runtime logic evaluation. Expressions are used for conditions, gates, visibility, and derived values. GEL is compiled and evaluated against game state.

Acceptance Criteria

- Compiles with existing `KeyExpressionCompiler`
- Produces an AST compatible with the evaluation engine
- Operates on literal values and context-bound identifiers
- Supports function calls, unary/binary ops, parentheses
- Supports nested fixture traversal (e.g., `thing.fixture("x").attr`)

BNF Grammar

```bnf
<expression> ::= <or_expr>

<or_expr> ::= <and_expr> ("||" <and_expr>)*
<and_expr> ::= <equality_expr> ("&&" <equality_expr>)*

equality_expr ::= <rel_expr> (("==" | "!=" ) <rel_expr>)*
<rel_expr> ::= <add_expr> (("<" | "<=" | ">" | ">=") <add_expr>)*

<add_expr> ::= <mult_expr> (("+" | "-") <mult_expr>)*
<mult_expr> ::= <unary_expr> (("*" | "/") <unary_expr>)*

<unary_expr> ::= ("!" | "-") <primary_expr> | <primary_expr>

<primary_expr> ::= <literal>
               | <identifier>
               | <function_call>
               | <attr_access>
               | "(" <expression> ")"

<attr_access> ::= <identifier> ("." <identifier> | "." "fixture" "(" <string> ")")+
<function_call> ::= <identifier> "(" [<expression> ("," <expression>)*] ")"

<literal> ::= "true" | "false" | <number> | <string>
<number> ::= /[0-9]+(\.[0-9]+)?/
<string> ::= '"' .*? '"'
<identifier> ::= /[a-zA-Z_][a-zA-Z0-9_]*/
```

Gaps in Code

- Nested function calls are not supported (deferred).
  - `src/main/java/com/demo/adventure/keyexpression/KeyExpressionCompiler.java`
- Identifiers cannot be resolved contextually; unresolved identifiers raise evaluation errors.
  - `src/main/java/com/demo/adventure/keyexpression/KeyExpressionEvaluator.java`
- Canonical expression syntax doc: `docs/design/key-expression.md`.

## 3. PIL - Player Interaction Language

Purpose

Maps raw user input into structured commands the engine can interpret. PIL is context-aware, token-driven, and routed through a controlled parser (`CommandScanner`, `GameCli`).

Acceptance Criteria

- Recognizes supported verbs, targets, and directions
- Supports basic sentence structures (verb noun, verb direction, etc.)
- Tokenizes according to `TokenType`
- Integrates with `ParsedCommand` or equivalent structure

BNF Grammar

```bnf
<command> ::= <verb> <argument>*

<verb> ::= "HELP" | "LOOK" | "LISTEN" | "INVENTORY" | "QUIT" | "SEARCH" | "HOW" | "MAKE" | "TAKE" | "DROP" | "GO" | <direction>
<direction> ::= "north" | "south" | "east" | "west" | "up" | "down"

<argument> ::= <quoted_string> | <identifier> | <direction>
<identifier> ::= /[a-zA-Z_][a-zA-Z0-9_-]*/
<quoted_string> ::= '"' .*? '"'
```

Gaps in Code

- No structured parsing of arguments - entire post-verb string treated as raw.
  - `src/main/java/com/demo/adventure/engine/command/interpreter/CommandCompiler.java`
  - `src/main/java/com/demo/adventure/engine/command/interpreter/CommandInterpreter.java`
- Several spec verbs (OPEN, USE, PUT, INSPECT) are tokenized but not routed - resolve to UNKNOWN.
- Diagnostics are minimal - CLI emits generic error on failure.
  - `src/main/java/com/demo/adventure/engine/cli/GameCli.java`
- Help docs drift from command list - `player.md` includes unsupported verbs.

Summary Table

| Layer     | DSL | Purpose                         | Timing      | Evaluated By                    |
| --------- | --- | ------------------------------- | ----------- | ------------------------------- |
| Structure | GDL | World and object structure      | Pre-runtime | GameSave YAML or future GDL compiler |
| Logic     | GEL | Condition logic, derived values | Runtime     | Expression compiler + evaluator |
| Input     | PIL | Player command mapping          | Runtime     | CommandScanner + GameCli |

General Design Acceptance Criteria

- Each DSL must be testable in isolation and usable by both AI and human authors
- Must reject malformed input with clear diagnostics
- Must conform to runtime-safe constraints (no logic in GDL, no mutation in GEL, etc.)
- Must be forward-compatible for extension without breaking syntax

Next Steps (Not Included Here)

- Define parser scaffolding and integration paths per layer
- Register grammar definitions with LLMs if used for AI generation
- Develop test fixtures for each DSL grammar

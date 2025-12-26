# Island Key Expression Syntax v0.1

This document describes the runtime expression language used by gate keys, visibility
checks, and other `keyString` fields.

## Lexical notes
- Whitespace is ignored.
- `//` starts a line comment.
- Identifiers are normalized to uppercase by the scanner; author function names in
  uppercase for clarity.

## Literals
- Booleans: `true`, `false`
- Numbers: integers or decimals (negative numbers use unary minus)
- Strings: double-quoted; backslash escapes the next character. Interpolation is
  reserved and not supported.

Examples:
- `"Lit Torch"`
- `3`
- `-2.5`
- `true`

## Operators and precedence
1. Parentheses: `(...)`
2. Unary: `!`, `-`
3. Multiplicative: `*`, `/`
4. Additive: `+`, `-`
5. Relational: `<`, `<=`, `>`, `>=`
6. Equality: `==`, `!=`
7. Logical AND: `&&`
8. Logical OR: `||`

## Functions
Supported functions (uppercase):
- `DICE(n | "dN")` -> number (supported sides: 4, 6, 8, 10, 12, 20)
- `HAS(label)` -> boolean
- `SEARCH(label)` -> boolean
- `SKILL(tag)` -> boolean

Nested function calls are not supported; arguments must be non-function expressions.

## Attribute access
Use dot paths to read attributes from the runtime context.

Grammar:
`root ( "." property | "." fixture("name") )+`

Cell fields (Thing-attached cells):
- `thing.cell.amount`
- `thing.cell.capacity`
- `thing.cell.volume`
- `thing.cell.name`

Missing cells resolve to `0` for numeric fields and the normalized cell key for `.name`.

Examples:
- `door.open`
- `room.fixture("table").visible`
- `lantern.kerosene.amount > 0`

Resolution is provided by the AttributeResolver at evaluation time. Resolution policy:
- Query mode: unresolved paths throw an unknown-reference error (user-facing "I don't know what that is").
- Compute mode: unresolved numeric fields fall back to `0` and booleans to `false` so expressions keep running.

## Examples
- `HAS("Key") && door.fixture("lock").open`
- `DICE(6) > 3`

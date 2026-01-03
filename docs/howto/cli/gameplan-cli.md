# GamePlanCli

Interactive AI-assisted authoring pipeline for the Adventure GamePlan. It runs the constraints interview, story spec, world plan, GDL synthesis, compile/validate, and structured export steps and writes receipts.

## Usage

```
mvn -q -Dexec.mainClass=com.demo.adventure.authoring.cli.GamePlanCli \
  -Dexec.args="--game western-adventure --out logs/gameplan --debug" exec:java
```

Optional arguments:
- `--seed <yaml>`: pre-seed a partial `constraints_brief` YAML file.
- `--debug`: print system/user prompts and responses.

## Outputs
- `constraints-brief.yaml`
- `story-spec.yaml`
- `world-plan.yaml`
- `gdl-source.yaml`
- `game.gdl`
- `game-save.yaml`
- `build-report.yaml`
- `structured-export.yaml`
- `structured/` (game.yaml, world/map.yaml, world/fixtures.yaml, world/items.yaml, world/actors.yaml, narrative/descriptions.yaml, narrative/backstory.md)
- `verification.yaml`
- `iterate.yaml`
- `receipt.yaml`
- `pipeline-receipt.yaml`

## Notes
- Requires `OPENAI_API_KEY`.

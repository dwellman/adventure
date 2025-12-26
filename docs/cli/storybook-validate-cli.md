# StorybookValidateCli

Validate a storybook GameSave YAML by loading it with GameSaveYamlLoader and building the world registry with GameSaveAssembler.

## Usage

```
mvn -q -Dexec.mainClass=com.demo.adventure.authoring.cli.StorybookValidateCli -Dexec.args="src/main/resources/storybook/gdl-demo/game.yaml" exec:java
```

## Notes

- Input must match the GameSaveYamlLoader format (monolithic YAML with plots, gates, fixtures, items, and actors).
- On failure, the CLI prints a WorldBuildReport with structured diagnostics.

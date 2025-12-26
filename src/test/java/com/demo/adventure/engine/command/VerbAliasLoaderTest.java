package com.demo.adventure.engine.command;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VerbAliasLoaderTest {

    @Test
    void loadsAliasList() {
        String yaml = """
aliases:
  - alias: Examine
    canonical: inspect
  - alias: build
    canonical: make
""";
        Map<String, TokenType> aliases = VerbAliasLoader.load(yaml);
        assertThat(aliases).containsEntry("EXAMINE", TokenType.INSPECT);
        assertThat(aliases).containsEntry("BUILD", TokenType.MAKE);
    }
}

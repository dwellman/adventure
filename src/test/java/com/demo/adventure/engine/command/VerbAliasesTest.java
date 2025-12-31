package com.demo.adventure.engine.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VerbAliasesTest {

    @Test
    void normalizesGoToMove() {
        assertThat(VerbAliases.canonicalize("go")).isEqualTo("MOVE");
    }

    @Test
    void reverseLookupIncludesGoAlias() {
        assertThat(VerbAliases.aliasesFor("move")).contains("GO");
    }

    @Test
    void normalizesStrikeToAttack() {
        assertThat(VerbAliases.canonicalize("strike")).isEqualTo("ATTACK");
    }

    @Test
    void normalizesRunToMove() {
        assertThat(VerbAliases.canonicalize("run")).isEqualTo("MOVE");
    }

    @Test
    void normalizesRollToDice() {
        assertThat(VerbAliases.canonicalize("roll")).isEqualTo("DICE");
    }

    @Test
    void reverseLookupIncludesStrikeAlias() {
        assertThat(VerbAliases.aliasesFor("attack")).contains("STRIKE");
    }
}

package com.demo.adventure.engine.command.handlers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClassicCommandFallbackTest {

    @Test
    void resolvesWhereAmIToLook() {
        assertThat(ClassicCommandFallback.resolve("where am i?")).isEqualTo("look");
    }

    @Test
    void resolvesLookAroundToLook() {
        assertThat(ClassicCommandFallback.resolve("look around")).isEqualTo("look");
    }

    @Test
    void resolvesDirectionTokenToGo() {
        assertThat(ClassicCommandFallback.resolve("northwest")).isEqualTo("go northwest");
    }

    @Test
    void ignoresUnknownInput() {
        assertThat(ClassicCommandFallback.resolve("dance")).isNull();
    }
}

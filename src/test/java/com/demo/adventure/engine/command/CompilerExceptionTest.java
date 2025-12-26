package com.demo.adventure.engine.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompilerExceptionTest {

    @Test
    void messageIncludesTokenLocation() {
        Token token = new Token(TokenType.IDENTIFIER, "go", null, 2, 4);
        CompilerException ex = new CompilerException("Unexpected", token);

        assertThat(ex.getMessage()).isEqualTo("Unexpected at line 2, column 4");
    }
}
